package de.upb.codingpirates.battleships.server.game;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.logic.*;
import de.upb.codingpirates.battleships.server.Properties;
import de.upb.codingpirates.battleships.server.game.actions.Action;
import de.upb.codingpirates.battleships.server.game.actions.PlaceShipAction;
import de.upb.codingpirates.battleships.server.game.actions.ShotsAction;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class GameHandler {
    private Game game;
    private boolean tournament;

    /**
     * maps client id to player
     */
    private Map<Integer, Client> player = Maps.newHashMap();
    /**
     * maps client id to spectator
     */
    private Map<Integer, Client> spectator = Maps.newHashMap();
    /**
     * maps player id to his field
     */
    private Map<Integer, Field> fields = Maps.newHashMap();
    /**
     * maps actionType to map from client id to action
     */
    private Map<ActionType, Map<Integer, Action>> actions = Maps.newHashMap();
    /**
     * maps player id to players ships
     */
    private Map<Integer, List<Ship>> ships = Maps.newHashMap();
    /**
     * maps player id to players ship placement
     */
    private Map<Integer, Map<Integer, PlacementInfo>> startShip = Maps.newHashMap();
    /**
     * maps player id to players score
     */
    private Map<Integer, Integer> score = Maps.newHashMap();
    /**
     * list of all hit shots
     */
    private List<Shot> hitShots = Lists.newArrayList();
    /**
     * list of all missed shots
     */
    private List<Shot> missedShots = Lists.newArrayList();

    public GameHandler(String name, int id, Configuration config, boolean tournament) {
        this.game = new Game(name, id, GameState.LOBBY, config, tournament);
        this.tournament = tournament;
    }

    /**
     * @return {@code true} if client was added
     */
    public boolean addClient(ClientType type, Client client) {
        switch (type) {
            case PLAYER:
                if (player.size() >= game.getConfig().MAXPLAYERCOUNT)
                    return false;
                player.putIfAbsent(client.getId(), client);
                fields.putIfAbsent(client.getId(), new Field(game.getConfig().HEIGHT, game.getConfig().WIDTH));
                game.addPlayer();
                return true;
            case SPECTATOR:
                if (player.size() >= Properties.MAXSPECTATOR)
                    return false;
                spectator.putIfAbsent(client.getId(), client);
                return true;
            default:
                return false;
        }
    }

    public void removeClient(int client) {
        if (this.player.containsKey(client)) {
            player.remove(client);
            fields.remove(client);
            game.removePlayer();
        }
        spectator.remove(client);
    }

    /**
     * @return {@code true} if action was added
     */
    public boolean addAction(Action action) {
        return this.actions.computeIfAbsent(action.getType(), (actionType -> Maps.newHashMap())).putIfAbsent(action.getSourceClient(), action) == null;
    }

    public void placeShips() {
        Map<Integer, Action> actionMap = this.actions.get(ActionType.PLACESHIP);
        if (actionMap != null) {
            for (Map.Entry<Integer, Action> entry : actionMap.entrySet()) {
                Field field = this.fields.get(entry.getKey());
                this.startShip.put(entry.getKey(), ((PlaceShipAction) entry.getValue()).getShips());
                Map<Integer, ShipType> ships = getConfiguration().getShips();
                for (Map.Entry<Integer, PlacementInfo> pair : ((PlaceShipAction) entry.getValue()).getShips().entrySet()) {
                    this.ships.computeIfAbsent(entry.getValue().getSourceClient(), id -> Lists.newArrayList()).add(field.placeShip(ships.get(pair.getKey()), pair.getValue()));
                }
            }
        }
    }

    public void performShots() {
        Map<Integer, Action> actionMap = this.actions.get(ActionType.SHOTS);
        if (actionMap != null) {
            for (Map.Entry<Integer, Action> entry : actionMap.entrySet()) {
                for (Shot shot : ((ShotsAction) entry.getValue()).getShots()) {
                    switch (fields.get(shot.getClientId()).hit(shot.getTargetField())) {
                        case HIT:
                            score.compute(entry.getKey(), (id, points) -> {
                                if (points == null) {
                                    return getConfiguration().HITPOINTS;
                                } else {
                                    return points + getConfiguration().HITPOINTS;
                                }
                            });
                            this.hitShots.add(shot);
                            break;
                        case SUNK:
                            score.compute(entry.getKey(), (id, points) -> {
                                if (points == null) {
                                    return getConfiguration().SUNKPOINTS;
                                } else {
                                    return points + getConfiguration().SUNKPOINTS;
                                }
                            });
                            this.hitShots.add(shot);
                            break;
                        default:
                            this.missedShots.add(shot);
                            break;
                    }
                }
            }
        }
    }

    public List<Client> getAllClients() {
        List<Client> clients = Lists.newArrayList();
        clients.addAll(this.player.values());
        clients.addAll(this.spectator.values());
        return clients;
    }

    public Collection<Client> getPlayer() {
        return player.values();
    }

    public void launchGame() {
        if (this.game.getState() == GameState.LOBBY) {
            this.game.setState(GameState.IN_PROGRESS);
            if (this.actions.containsKey(ActionType.PLACESHIP)) {
                this.actions.get(ActionType.PLACESHIP).clear();
            }
        }
    }

    public void pauseGame() {
        if (this.game.getState() == GameState.IN_PROGRESS) {
            this.game.setState(GameState.PAUSED);
        }
    }

    public void continueGame() {
        if (this.game.getState() == GameState.PAUSED) {
            this.game.setState(GameState.IN_PROGRESS);
        }
    }

    public void run() {
        switch (this.game.getState()) {
            case LOBBY:
                break;
            case PAUSED:
                break;
            case FINISHED:
                break;
            case IN_PROGRESS:
                break;
            default:
                break;
        }
    }

    public Game getGame() {
        return game;
    }

    public Configuration getConfiguration() {
        return game.getConfig();
    }

    public List<Shot> getHitShots() {
        return hitShots;
    }

    public List<Shot> getMissedShots() {
        return missedShots;
    }

    public List<Shot> getShots() {
        List<Shot> shots = Lists.newArrayList(this.hitShots);
        shots.addAll(missedShots);
        return shots;
    }

    public Map<Integer, Map<Integer, PlacementInfo>> getStartShip() {
        return startShip;
    }

    public Map<Integer, Integer> getScore() {
        return score;
    }
}
