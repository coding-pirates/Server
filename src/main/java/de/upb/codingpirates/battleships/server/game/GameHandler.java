package de.upb.codingpirates.battleships.server.game;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.logic.*;
import de.upb.codingpirates.battleships.network.message.notification.*;
import de.upb.codingpirates.battleships.network.message.request.PlaceShipsRequest;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.exceptions.game.NotAllowedException;
import de.upb.codingpirates.battleships.network.id.IntId;
import de.upb.codingpirates.battleships.server.Properties;
import de.upb.codingpirates.battleships.server.Translator;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class GameHandler implements Translator {

    @Nonnull
    private ClientManager clientManager;
    /**
     * game properties
     */
    @Nonnull
    private Game game;
    /**
     * if game owned by tournament
     */
    private boolean tournament;
    /**
     * maps client id to player
     */
    private final Map<Integer, Client> player = Maps.newHashMap();
    /**
     * maps client id to spectator
     */
    private final Map<Integer, Client> spectator = Maps.newHashMap();
    /**
     * maps player id to his field
     */
    private final Map<Integer, Field> fields = Maps.newHashMap();
    /**
     * maps client id to shots
     */
    private final Map<Integer, Collection<Shot>> shots = Maps.newHashMap();
    /**
     * maps player id to players ships
     */
    private final Map<Integer, Map<Integer, Ship>> ships = Maps.newHashMap();
    /**
     * maps player id to players ship placement
     */
    private final Map<Integer, Map<Integer, PlacementInfo>> startShip = Maps.newHashMap();
    /**
     * maps player id to players score
     */
    private final Map<Integer, Integer> score = Maps.newHashMap();
    /**
     * list of all hit shots
     */
    private final List<Shot> hitShots = Lists.newArrayList();
    /**
     * list of all missed shots
     */
    private final List<Shot> missedShots = Lists.newArrayList();
    /**
     * list of all shots that result in a sunken ship
     */
    private final List<Shot> sunkShots = Lists.newArrayList();
    /**
     * list of all sunken ships
     */
    private final List<Ship> sunkenShips = Lists.newArrayList();
    /**
     * maps from ship to shots that hit the ship
     */
    private final Map<Ship, List<Shot>> shipToShots = Maps.newHashMap();

    public GameHandler(@Nonnull String name, int id, @Nonnull Configuration config, boolean tournament, @Nonnull ClientManager clientManager) {
        this.game = new Game(name, id, GameState.LOBBY, config, tournament);
        this.tournament = tournament;
        this.clientManager = clientManager;
    }

    /**
     * adds the client as the spectator or player to the game
     * @throws InvalidActionException if game is full
     */
    public void addClient(@Nonnull ClientType type, @Nonnull Client client) throws InvalidActionException {
        switch (type) {
            case PLAYER:
                if (player.size() >= game.getConfig().getMaxPlayerCount())
                    throw new InvalidActionException("game.isFull");
                player.putIfAbsent(client.getId(), client);
                fields.putIfAbsent(client.getId(), new Field(game.getConfig().getHeight(), game.getConfig().getWidth()));
                game.addPlayer();
            case SPECTATOR:
                if (player.size() >= Properties.MAXSPECTATOR)
                    throw new InvalidActionException("game.isFull");
                spectator.putIfAbsent(client.getId(), client);
        }
    }

    /**
     * removes from the game including all statistics
     */
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

    /**
     * @return all clients participating in the game or viewing the game. (player, spectator)
     */
    public List<Client> getAllClients() {
        List<Client> clients = Lists.newArrayList();
        clients.addAll(this.player.values());
        clients.addAll(this.spectator.values());
        return clients;
    }

    /**
     * @return all players
     */
    public Collection<Client> getPlayer() {
        return player.values();
    }

    /**
     * @return the {@link Game} object
     */
    @Nonnull
    public Game getGame() {
        return game;
    }

    /**
     * @return the {@link Configuration} from the {@link Game} object
     */
    private Configuration getConfiguration() {
        return game.getConfig();
    }

    /**
     * @return all shots that hit a ship
     */
    public List<Shot> getHitShots() {
        return hitShots;
    }

    /**
     * @return all shots that doesn't hit a target
     */
    public List<Shot> getMissedShots() {
        return missedShots;
    }

    /**
     * @return all shots that hit a ship if the ship is already sunken
     */
    public List<Shot> getSunkShots(){
        return sunkShots;
    }

    /**
     * @return all shots
     */
    public List<Shot> getShots() {
        List<Shot> shots = Lists.newArrayList(this.hitShots);
        shots.addAll(missedShots);
        return shots;
    }

    /**
     * @return the ship placements for every player
     */
    public Map<Integer, Map<Integer, PlacementInfo>> getStartShip() {
        return startShip;
    }

    /**
     * @return the score
     */
    public Map<Integer, Integer> getScore() {
        return score;
    }

    /**
     * @return all sunken ships
     */
    public List<Ship> getSunkenShips() {
        return sunkenShips;
    }

    /**
     * adds a ship placement configuration for a player
     * @param clientId id of the player
     * @param ships map from ship id to placementinfo
     * @throws GameException if to many ships have been placed or the ships for the player has already been placed
     */
    public void addShipPlacement(int clientId, Map<Integer, PlacementInfo> ships) throws GameException {
        if(ships.size() > getConfiguration().getShips().size()){
            throw new NotAllowedException("game.player.toManyShips");
        }
        if(this.startShip.putIfAbsent(clientId,ships) != ships){
            throw new InvalidActionException("game.player.shipsAlreadyPlaced");
        }
    }

    /**
     * adds shots placement for a player
     * @param clientId id of the player
     * @param shots all shots from the player
     * @throws GameException if to many shots have been placed or the shots for the player has already been placed
     */
    public void addShotPlacement(int clientId, Collection<Shot> shots) throws GameException {
        if(shots.size() > getConfiguration().getShotCount()){
            throw new NotAllowedException("game.player.toManyShots");
        }
        if(this.shots.putIfAbsent(clientId,shots) != shots){
            throw new InvalidActionException("game.player.shotsAlreadyPlaced");
        }
    }

//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------

    private long timeStamp = 0L;
    private long pauseTimeCache = 0L;
    private GameStage stage = GameStage.START;

    /**
     * main method, called every tick
     * only run when {@link Game} has {@link GameState#IN_PROGRESS}
     * <p>
     * {@link GameStage#START}:
     * does everything which should be done beforehand.<p>
     * {@link GameStage#PLACESHIPS}:
     * waits for round timer & waits for {@link PlaceShipsRequest}'s <p>
     * {@link GameStage#SHOTS}:
     * waits for round timer & waits for {@link de.upb.codingpirates.battleships.network.message.request.ShotsRequest}'s.
     * then calculates all shots <p>
     * {@link GameStage#VISUALIZATION}:
     * waits for visualization round timer.
     * and send {@link PlayerUpdateNotification} & {@link SpectatorUpdateNotification}.
     * then checks is more than one player has ships left<p>
     * {@link GameStage#FINISHED}:
     * sends {@link FinishNotification} and sets gameState to {@link GameState#FINISHED}
     */
    public void run() {
        if(!game.getState().equals(GameState.IN_PROGRESS))return;
        switch (this.stage) {
            case START:
                this.startGame();
            case PLACESHIPS:
                if (System.currentTimeMillis() - timeStamp >= getConfiguration().getRoundTime()) {
                    this.placeShips();
                    this.timeStamp = System.currentTimeMillis();
                    this.clientManager.sendMessageToClients(new GameStartNotification(),getAllClients());
                    this.stage = GameStage.SHOTS;
                }
                break;
            case VISUALIZATION:
                if (System.currentTimeMillis() - timeStamp >= getConfiguration().getVisualizationTime()) {
                    clientManager.sendMessageToClients(new RoundStartNotification(), getAllClients());
                    this.stage = GameStage.SHOTS;
                    this.timeStamp = System.currentTimeMillis();
                    this.clientManager.sendMessageToClients(new PlayerUpdateNotification(hitShots, this.score, this.sunkShots), player.values());
                    this.clientManager.sendMessageToClients(new SpectatorUpdateNotification(hitShots,score,this.sunkShots,missedShots),spectator.values());
                }
                if(ships.size() <= 1){
                    this.stage = GameStage.FINISHED;
                }
                break;
            case SHOTS:
                if (System.currentTimeMillis() - timeStamp >= getConfiguration().getRoundTime()) {
                    this.performShots();
                    this.timeStamp = System.currentTimeMillis();
                    this.stage = GameStage.VISUALIZATION;
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

    /**
     * set games state to {@link GameState#IN_PROGRESS} & stage to {@link GameStage#START}
     */
    public void launchGame() {
        if (this.game.getState() == GameState.LOBBY) {
            this.game.setState(GameState.IN_PROGRESS);
            this.stage = GameStage.START;
        }
    }

    /**
     * sets game stage to {@link GameStage#PLACESHIPS} & sends {@link GameInitNotification} & start round
     */
    private void startGame() {
        if (this.stage.equals(GameStage.START)) {
            this.startShip.clear();
            this.stage = GameStage.PLACESHIPS;
            this.timeStamp = System.currentTimeMillis();
            this.clientManager.sendMessageToClients(new GameInitNotification(getAllClients(), this.getConfiguration()), getAllClients());
        }
    }

    /**
     * saves remaining time of the round and pauses the game
     */
    public void pauseGame() {
        if (this.game.getState() == GameState.IN_PROGRESS) {
            this.game.setState(GameState.PAUSED);
            switch (stage){
                case VISUALIZATION:
                    this.pauseTimeCache = getConfiguration().getVisualizationTime() - (System.currentTimeMillis() - timeStamp);
                    break;
                case PLACESHIPS:
                case SHOTS:
                    this.pauseTimeCache = getConfiguration().getRoundTime() - (System.currentTimeMillis() - timeStamp);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * uses saved remain time to return to game
     */
    public void continueGame() {
        if (this.game.getState() == GameState.PAUSED) {
            this.game.setState(GameState.IN_PROGRESS);
            this.timeStamp = System.currentTimeMillis() + pauseTimeCache;
        }
    }

    /**
     * places all ships in {@link #startShip} and removes all player that didn't placed there ships
     */
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

    /**
     * perform all shots in {@link #shots}
     */
    private void performShots() {
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
                int points = getConfiguration().getHitPoints() / entry.getValue().size();
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
                int points = getConfiguration().getSunkPoints() / entry.getValue().size();
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
        sunkShips.forEach((ship -> this.sunkShots.addAll(this.shipToShots.get(ship))));
        this.shots.clear();
    }

    /**
     * removes player that didn't placed their ships
     */
    private void removeInactivePlayer(Collection<Client> clients) {
        clientManager.sendMessageToClients(new ErrorNotification(ErrorType.INVALID_ACTION, PlaceShipsRequest.MESSAGE_ID, translate("game.player.noPlacedShips")), clients);
        clients.forEach(client -> this.removeClient(client.getId()));
        clients.forEach(client -> clientManager.sendMessageToClients(new LeaveNotification(client.getId()), clients));
    }

    /**
     * @return remaining round time
     * @throws InvalidActionException if there is no round running
     */
    public long getRemainingTime() throws InvalidActionException{
        if(!this.game.getState().equals(GameState.IN_PROGRESS) && !this.game.getState().equals(GameState.PAUSED))throw new InvalidActionException("game.noTimerActive");
        if(this.game.getState().equals(GameState.PAUSED))return this.pauseTimeCache;
        switch (stage){
            case PLACESHIPS:
            case SHOTS:
                return System.currentTimeMillis() - this.timeStamp - getConfiguration().getRoundTime();
            case VISUALIZATION:
                return System.currentTimeMillis() - this.timeStamp - getConfiguration().getVisualizationTime();
            default:
                throw new InvalidActionException("game.noTimerActive");
        }
    }
}
