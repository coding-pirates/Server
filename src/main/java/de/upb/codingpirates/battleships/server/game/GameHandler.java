package de.upb.codingpirates.battleships.server.game;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.logic.*;
import de.upb.codingpirates.battleships.network.message.notification.*;
import de.upb.codingpirates.battleships.network.message.request.PlaceShipsRequest;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.Properties;
import de.upb.codingpirates.battleships.server.game.actions.Action;
import de.upb.codingpirates.battleships.server.game.actions.PlaceShipAction;
import de.upb.codingpirates.battleships.server.game.actions.ShotsAction;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GameHandler {

    private ClientManager clientManager;

    /**
     * game properties
     */
    private Game game;
    /**
     * if game owned by tournament
     */
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

    /**
     * ship to shots that hit the ship
     */
    private Map<Ship, List<Shot>> shipToShots = Maps.newHashMap();

    public GameHandler(String name, int id, Configuration config, boolean tournament, ClientManager clientManager) {
        this.game = new Game(name, id, GameState.LOBBY, config, tournament);
        this.tournament = tournament;
        this.clientManager = clientManager;
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

    public List<Client> getAllClients() {
        List<Client> clients = Lists.newArrayList();
        clients.addAll(this.player.values());
        clients.addAll(this.spectator.values());
        return clients;
    }

    public Collection<Client> getPlayer() {
        return player.values();
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

//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------

    private long timeStamp = 0L;
    private long pauseTimeCache = 0L;
    private GameStage stage = GameStage.START;

    public void run() {
        switch (this.stage) {
            case START:
                this.startGame();
            case PLACESHIPS:
                if (System.currentTimeMillis() - timeStamp >= getConfiguration().ROUNDTIME) {
                    this.placeShips();
                    this.timeStamp = System.currentTimeMillis();
                    this.stage = GameStage.VISUALIZATION;
                }
                break;
            case VISUALIZATION:
                if (System.currentTimeMillis() - timeStamp >= getConfiguration().VISUALIZATIONTIME) {
                    clientManager.sendMessageToClients(new RoundStartNotification(), getAllClients());
                    this.stage = GameStage.SHOTS;
                    this.timeStamp = System.currentTimeMillis();
                }
                break;
            case SHOTS:
                if (System.currentTimeMillis() - timeStamp >= getConfiguration().ROUNDTIME) {
                    List<Ship> sunkShips = this.performShots();
                    List<Shot> sunkShots = Lists.newArrayList();
                    sunkShips.forEach(ship -> sunkShots.addAll(this.shipToShots.get(ship)));
                    this.timeStamp = System.currentTimeMillis();
                    this.stage = GameStage.VISUALIZATION;
                    this.clientManager.sendMessageToClients(new PlayerUpdateNotification(hitShots, this.score, sunkShots), player.values());
                }
                break;
            default:
                break;
        }
    }

    public void launchGame() {
        if (this.game.getState() == GameState.LOBBY) {
            this.game.setState(GameState.IN_PROGRESS);
            this.stage = GameStage.START;
        }
    }

    public void startGame() {
        if (this.stage.equals(GameStage.START)) {
            if (this.actions.containsKey(ActionType.PLACESHIP)) {
                this.actions.get(ActionType.PLACESHIP).clear();
            }
            this.stage = GameStage.PLACESHIPS;
            this.timeStamp = System.currentTimeMillis();
            this.clientManager.sendMessageToClients(new GameInitNotification(getAllClients(), this.getConfiguration()), getAllClients());
        }
    }

    public void pauseGame() {
        if (this.game.getState() == GameState.IN_PROGRESS) {
            this.game.setState(GameState.PAUSED);
            switch (stage){
                case VISUALIZATION:
                    this.pauseTimeCache = getConfiguration().VISUALIZATIONTIME - (System.currentTimeMillis() - timeStamp);
                    break;
                case PLACESHIPS:
                case SHOTS:
                    this.pauseTimeCache = getConfiguration().ROUNDTIME - (System.currentTimeMillis() - timeStamp);
                    break;
                default:
                    break;
            }
        }
    }

    public void continueGame() {
        if (this.game.getState() == GameState.PAUSED) {
            this.game.setState(GameState.IN_PROGRESS);
            this.timeStamp = System.currentTimeMillis() + pauseTimeCache;
        }
    }

    private void placeShips() {
        List<Integer> clients = Lists.newArrayList();
        Map<Integer, Action> actionMap = this.actions.get(ActionType.PLACESHIP);
        if (actionMap != null) {
            for (Map.Entry<Integer, Action> entry : actionMap.entrySet()) {
                clients.add(entry.getKey());
                Field field = this.fields.get(entry.getKey());
                this.startShip.put(entry.getKey(), ((PlaceShipAction) entry.getValue()).getShips());
                Map<Integer, ShipType> ships = getConfiguration().getShips();
                for (Map.Entry<Integer, PlacementInfo> pair : ((PlaceShipAction) entry.getValue()).getShips().entrySet()) {
                    this.ships.computeIfAbsent(entry.getValue().getSourceClient(), id -> Lists.newArrayList()).add(field.placeShip(ships.get(pair.getKey()), pair.getValue()));
                }
            }
        }
        List<Client> clients1 = player.entrySet().stream().filter(integerClientEntry -> !clients.contains(integerClientEntry.getKey())).map(Map.Entry::getValue).collect(Collectors.toList());
        removeInactivePlayer(clients1);
    }

    private List<Ship> performShots() {
        List<Ship> sunkShips = Lists.newArrayList();
        Map<HitType,Map<Shot, List<Integer>>> hitToPoint = Maps.newHashMap();
        Map<Integer, Action> actionMap = this.actions.get(ActionType.SHOTS);
        if (actionMap != null) {
            for (Map.Entry<Integer, Action> entry : actionMap.entrySet()) {
                for (Shot shot : ((ShotsAction) entry.getValue()).getShots()) {
                    ShotHit hit = fields.get(shot.getClientId()).hit(shot);
                    switch (hit.getHitType()) {
                        case HIT:
                            hitToPoint.computeIfAbsent(HitType.HIT,hitType -> Maps.newHashMap()).computeIfAbsent(hit.getShot(),point2D -> Lists.newArrayList()).add(entry.getKey());

                            this.hitShots.add(shot);
                            this.shipToShots.computeIfAbsent(hit.getShip(),(ship -> Lists.newArrayList())).add(hit.getShot());
                            break;
                        case SUNK:
                            hitToPoint.computeIfAbsent(HitType.SUNK,hitType -> Maps.newHashMap()).computeIfAbsent(hit.getShot(),point2D -> Lists.newArrayList()).add(entry.getKey());

                            this.hitShots.add(shot);
                            this.shipToShots.computeIfAbsent(hit.getShip(),(ship -> Lists.newArrayList())).add(hit.getShot());
                            sunkShips.add(hit.getShip());
                            break;
                        default:
                            this.missedShots.add(shot);
                            break;
                    }
                }
            }
        }

        //add points for each hit to the player who shot a Shot at a position
        if(hitToPoint.containsKey(HitType.HIT)){
            for(Map.Entry<Shot, List<Integer>> entry : hitToPoint.get(HitType.HIT).entrySet()) {
                int points = getConfiguration().HITPOINTS / entry.getValue().size();
                entry.getValue().forEach( client ->
                score.compute(client, (id, point) -> {
                    if (point == null) {
                        return points;
                    } else {
                        return point + points;
                    }
                }));
            }
        }
        if(hitToPoint.containsKey(HitType.SUNK)){
            for(Map.Entry<Shot, List<Integer>> entry : hitToPoint.get(HitType.HIT).entrySet()) {
                int points = getConfiguration().SUNKPOINTS / entry.getValue().size();
                entry.getValue().forEach( client ->
                        score.compute(client, (id, point) -> {
                            if (point == null) {
                                return points;
                            } else {
                                return point + points;
                            }
                        }));
            }
        }
        return sunkShips;
    }

    private void removeInactivePlayer(Collection<Client> clients) {
        clientManager.sendMessageToClients(new ErrorNotification(ErrorType.INVALID_ACTION, PlaceShipsRequest.MESSAGE_ID, "No ships were placed"), clients);
        clients.forEach(client -> this.removeClient(client.getId()));
        clients.forEach(client -> clientManager.sendMessageToClients(new LeaveNotification(client.getId()), clients));
    }
}
