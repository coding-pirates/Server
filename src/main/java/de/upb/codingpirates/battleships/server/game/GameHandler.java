package de.upb.codingpirates.battleships.server.game;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.logic.*;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.exceptions.game.NotAllowedException;
import de.upb.codingpirates.battleships.network.id.IntId;
import de.upb.codingpirates.battleships.network.message.notification.*;
import de.upb.codingpirates.battleships.network.message.request.PlaceShipsRequest;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.Properties;

import java.util.*;
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
     * maps client id to shots
     */
    private Map<Integer, Collection<Shot>> shots = Maps.newHashMap();
    /**
     * maps player id to players ships
     */
    private Map<Integer, Map<Integer,Ship>> ships = Maps.newHashMap();
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

    private List<Ship> sunkenShips = Lists.newArrayList();

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
            score.remove(client);
            ships.remove(client);
            startShip.remove(client);
        }
        spectator.remove(client);
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

    public Map<Ship, List<Shot>> getShipToShots() {
        return shipToShots;
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

    public List<Ship> getSunkenShips() {
        return sunkenShips;
    }

    public void addShipPlacement(int clientId, Map<Integer,PlacementInfo> ships) throws GameException {
        if(ships.size() > getConfiguration().getShips().size()){
            throw new NotAllowedException("You have set to many ships");
        }
        if(this.startShip.putIfAbsent(clientId,ships) != ships){
            throw new InvalidActionException("Your ships are already placed");
        }
    }

    public void addShotPlacement(int clientId, Collection<Shot> shots) throws GameException {
        if(shots.size() > getConfiguration().SHOTCOUNT){
            throw new NotAllowedException("Your have shot to many times");
        }
        if(this.shots.putIfAbsent(clientId,shots) != shots){
            throw new InvalidActionException("Your shots are already placed");
        }
    }

//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------

    private long timeStamp = 0L;
    private long pauseTimeCache = 0L;
    private GameStage stage = GameStage.START;

    /**
     * main method, called every tick
     */
    public void run() {
        if(game.getState().equals(GameState.FINISHED) || game.getState().equals(GameState.LOBBY) || game.getState().equals(GameState.PAUSED))return;
        switch (this.stage) {
            case START:
                this.startGame();
            case PLACESHIPS:
                if (System.currentTimeMillis() - timeStamp >= getConfiguration().ROUNDTIME) {
                    this.placeShips();
                    this.timeStamp = System.currentTimeMillis();
                    this.clientManager.sendMessageToClients(new GameStartNotification(),getAllClients());
                    this.stage = GameStage.SHOTS;
                }
                break;
            case VISUALIZATION:
                if (System.currentTimeMillis() - timeStamp >= getConfiguration().VISUALIZATIONTIME) {
                    clientManager.sendMessageToClients(new RoundStartNotification(), getAllClients());
                    this.stage = GameStage.SHOTS;
                    this.timeStamp = System.currentTimeMillis();
                }
                if(ships.size() <= 1){
                    this.stage = GameStage.FINISHED;
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
                    this.clientManager.sendMessageToClients(new SpectatorUpdateNotification(hitShots,score,sunkShots,missedShots),spectator.values());
                }
                break;
            case FINISHED:
                Optional<Map.Entry<Integer,Integer>> winner = score.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue));
                int id = 0;
                if(winner.isPresent()){
                    id = winner.get().getKey();
                }
                this.clientManager.sendMessageToClients(new FinishNotification(score,new IntId(id)),getAllClients());
                this.game.setState(GameState.FINISHED);
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
            this.startShip.clear();
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
        Map<Integer, ShipType> ships = getConfiguration().getShips();
        for (Map.Entry<Integer, Map<Integer, PlacementInfo>> clientEntry : startShip.entrySet()) {
            clients.add(clientEntry.getKey());
            Field field = this.fields.get(clientEntry.getKey());
            for (Map.Entry<Integer, PlacementInfo> shipEntry : clientEntry.getValue().entrySet()) {
                Ship ship = field.placeShip(ships.get(shipEntry.getKey()), shipEntry.getValue());
                if(ship != null) {//TODO ship can't be placed
                    this.ships.computeIfAbsent(clientEntry.getKey(), id -> Maps.newHashMap()).putIfAbsent(shipEntry.getKey(), ship);
                }
            }
        }
        List<Client> clients1 = player.entrySet().stream().filter(integerClientEntry -> !clients.contains(integerClientEntry.getKey())).map(Map.Entry::getValue).collect(Collectors.toList());
        this.removeInactivePlayer(clients1);
    }

    private List<Ship> performShots() {
        List<Ship> sunkShips = Lists.newArrayList();
        Map<HitType,Map<Shot, List<Integer>>> hitToPoint = Maps.newHashMap();
            for (Map.Entry<Integer, Collection<Shot>> entry : shots.entrySet()) {
                for (Shot shot : entry.getValue()) {
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
        this.ships.forEach((clientId, ships) -> ships.forEach((shipId, ship) -> {
            if(sunkShips.contains(ship)){
                ships.remove(shipId);
            }
            if(ships.isEmpty()){
                this.ships.remove(clientId);
            }
        }));
        this.shots.clear();
        return sunkShips;
    }

    private void removeInactivePlayer(Collection<Client> clients) {
        clientManager.sendMessageToClients(new ErrorNotification(ErrorType.INVALID_ACTION, PlaceShipsRequest.MESSAGE_ID, "No ships were placed"), clients);
        clients.forEach(client -> this.removeClient(client.getId()));
        clients.forEach(client -> clientManager.sendMessageToClients(new LeaveNotification(client.getId()), clients));
    }

    public long getRemainingTime() throws InvalidActionException{
        if(!this.game.getState().equals(GameState.IN_PROGRESS) && !this.game.getState().equals(GameState.PAUSED))throw new InvalidActionException("there is no timer active");
        if(this.game.getState().equals(GameState.PAUSED))return this.pauseTimeCache;
        switch (stage){
            case PLACESHIPS:
            case SHOTS:
                return System.currentTimeMillis() - this.timeStamp - getConfiguration().ROUNDTIME;
            case VISUALIZATION:
                return System.currentTimeMillis() - this.timeStamp - getConfiguration().VISUALIZATIONTIME;
            default:
                throw new InvalidActionException("there is no timer active");
        }
    }
}
