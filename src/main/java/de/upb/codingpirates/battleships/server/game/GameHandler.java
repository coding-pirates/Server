package de.upb.codingpirates.battleships.server.game;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.upb.codingpirates.battleships.logic.*;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.exceptions.game.NotAllowedException;
import de.upb.codingpirates.battleships.network.message.notification.*;
import de.upb.codingpirates.battleships.network.message.request.PlaceShipsRequest;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.util.ServerMarker;
import de.upb.codingpirates.battleships.server.util.Translator;

/**
 * @author Paul Becker
 */
public class GameHandler implements Translator {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nonnull
    private ClientManager clientManager;

    /** The core {@link Game} object wrapped by this {@code GameHandler}. */
    @Nonnull
    private final Game game;

    /**
     * Maps IDs of {@link Client}s whose {@link ClientType} is {@link ClientType#PLAYER} to their respective object
     * instances.
     *
     * @see #spectatorsById
     */
    @Nonnull
    private final Map<Integer, Client> playersById = Collections.synchronizedMap(Maps.newHashMap());

    /**
     * Maps IDs of {@link Client}s whose {@link ClientType} is {@link ClientType#SPECTATOR} to their respective object
     * instances.
     *
     * @see #playersById
     */
    @Nonnull
    private final Map<Integer, Client> spectatorsById = Collections.synchronizedMap(Maps.newHashMap());

    /**
     * Maps IDs of {@link Client}s whose {@link ClientType} is {@link ClientType#PLAYER} to their {@link Field}s.
     */
    @Nonnull
    private final Map<Integer, Field> fieldsByPlayerId = Collections.synchronizedMap(Maps.newHashMap());

    /**
     * maps client id to shots
     */
    @Nonnull
    private final Map<Integer, Collection<Shot>> shots = Collections.synchronizedMap(Maps.newHashMap());

    /**
     * maps player id to players ships
     */
    @Nonnull
    private final Map<Integer, Map<Integer, Ship>> ships = Collections.synchronizedMap(Maps.newHashMap());

    /**
     * maps player id to players ship placement
     */
    @Nonnull
    private final Map<Integer, Map<Integer, PlacementInfo>> startShip = Collections.synchronizedMap(Maps.newHashMap());

    /**
     * maps player id to players score
     */
    @Nonnull
    private final Map<Integer, Integer> score = Collections.synchronizedMap(Maps.newHashMap());

    /**
     * list of all hit shots
     */
    @Nonnull
    private final List<Shot> hitShots = Collections.synchronizedList(Lists.newArrayList());

    /**
     * list of all missed shots
     */
    @Nonnull
    private final List<Shot> missedShots = Collections.synchronizedList(Lists.newArrayList());

    /**
     * list of all shots that result in a sunken ship
     */
    @Nonnull
    private final List<Shot> sunkShots = Collections.synchronizedList(Lists.newArrayList());

    /**
     * list of all sunken ships
     */
    @Nonnull
    private final List<Ship> sunkenShips = Collections.synchronizedList(Lists.newArrayList());

    /**
     * maps from ship to shots that hit the ship
     */
    @Nonnull
    private final Map<Ship, List<Shot>> shipToShots = Collections.synchronizedMap(Maps.newHashMap());

    /**
     * The minimum amount of {@link Client}s with {@link ClientType#PLAYER} required in order to launch a game using
     * the {@link #launchGame()} method.
     *
     * @see #launchGame()
     */
    public static final int MIN_PLAYER_COUNT = 2;

    /**
     * The maximum amount of {@link Client}s with {@link ClientType#SPECTATOR} which can spectate a {@link Game}.
     *
     * @see #addClient(ClientType, Client)
     */
    private static final int MAX_SPECTATOR_COUNT = Integer.MAX_VALUE;

    public GameHandler(@Nonnull final String name, final int id, @Nonnull final Configuration config, final boolean tournament, @Nonnull final ClientManager clientManager) {
        this.game          = new Game(id, name, GameState.LOBBY, config, tournament);
        this.clientManager = clientManager;

        stateProperty()
            .addListener((observable, oldValue, newValue) -> game.setState(newValue));
        currentPlayerCountProperty
            .addListener((observable, oldValue, newValue) -> game.setCurrentPlayerCount(newValue.intValue()));
    }

    /*
     * Usually these properties would be part of the Game class, however, this is not possible because the Game class
     * is common to all platforms and not all the platforms it is used on, Android in particular, implement JavaFX,
     * which is why the currentPlayerCountProperty and stateProperty are now part of the GameHandler rather than the
     * Game class.
     *
     * Listeners are set up on these properties which mirror any changes occurring to the currentPlayerCountProperty
     * an stateProperty to the fields of the Game object wrapped by this GameHandler to keep the values synchronized.
     */

    // <editor-fold desc="currentPlayerCountProperty">
    private final IntegerProperty currentPlayerCountProperty = new SimpleIntegerProperty();

    public void incrementCurrentPlayerCount() {
        if (currentPlayerCountProperty.get() < Integer.MAX_VALUE)
            currentPlayerCountProperty.add(1);
    }

    public void decrementCurrentPlayerCount() {
        if (currentPlayerCountProperty.get() > 0)
            currentPlayerCountProperty.subtract(1);
    }

    public IntegerProperty currentPlayerCountProperty() {
        return currentPlayerCountProperty;
    }
    // </editor-fold>

    // <editor-fold desc="stateProperty">
    private final ObjectProperty<GameState> stateProperty = new SimpleObjectProperty<>();

    public GameState getState() {
        return stateProperty.get();
    }

    public void setState(@Nonnull final GameState state) {
        stateProperty.set(state);
    }

    public ObjectProperty<GameState> stateProperty() {
        return stateProperty;
    }
    // </editor-fold>

    /**
     * adds the client as the spectator or player to the game
     * @throws InvalidActionException if game is full
     */
    public void addClient(@Nonnull ClientType type, @Nonnull Client client) throws InvalidActionException {
        switch (type) {
            case PLAYER:
                if (playersById.size() >= game.getConfig().getMaxPlayerCount())
                    throw new InvalidActionException("game.isFull");
                playersById.put(client.getId(), client);
                fieldsByPlayerId.put(client.getId(), new Field(getGame().getConfig().getHeight(), getGame().getConfig().getWidth(),client.getId()));
                incrementCurrentPlayerCount();
                break;
            case SPECTATOR:
                if (spectatorsById.size() >= MAX_SPECTATOR_COUNT)
                    throw new InvalidActionException("game.isFull");
                spectatorsById.putIfAbsent(client.getId(), client);
        }
    }

    /**
     * Removes the {@link Client} with the provided {@code clientId} from the {@link Game} wrapped by this
     * {@code GameHandler}, removing all statistics associated with the client of the provided {@code clientId} in the
     * process.
     *
     * @param clientId The ID associated with the {@link Client} instance which is to be removed from the game.
     */
    public void removeClient(final int clientId) {
        if (this.playersById.containsKey(clientId)) {
            this.playersById.remove(clientId);
            this.fieldsByPlayerId.remove(clientId);
            this.ships.remove(clientId);
            this.startShip.remove(clientId);

            decrementCurrentPlayerCount();
        }
        this.spectatorsById.remove(clientId);
    }

    /**
     * @return A {@link List} containing all {@link Client}s which are participating in the {@link Game} wrapped by this
     *         {@code GameHandler} or which are spectating it.
     */
    @Nonnull
    public List<Client> getAllClients() {
        final List<Client> clients = new ArrayList<>(getPlayers().size() + getSpectators().size());

        clients.addAll(getPlayers());
        clients.addAll(getSpectators());

        return clients;
    }

    /**
     * @return all players
     */
    @Nonnull
    public Collection<Client> getPlayers() {
        return playersById.values();
    }

    @Nonnull
    public Collection<Client> getSpectators() {
        return spectatorsById.values();
    }

    @Nonnull
    public Map<Integer, Map<Integer, Ship>> getShips() {
        return ships;
    }

    /** @return The {@link Game} object wrapped by this {@code GameHandler}. */
    @Nonnull
    public Game getGame() {
        return game;
    }

    /** @return the {@link Configuration} from the {@link Game} object */
    @Nonnull
    private Configuration getConfiguration() {
        return game.getConfig();
    }

    /**
     * @return all shots that hit a ship
     */
    @Nonnull
    public List<Shot> getHitShots() {
        return hitShots;
    }

    /**
     * @return all shots that doesn't hit a target
     */
    @Nonnull
    public List<Shot> getMissedShots() {
        return missedShots;
    }

    /**
     * @return all shots that hit a ship if the ship is already sunken
     */
    @Nonnull
    public List<Shot> getSunkShots(){
        return sunkShots;
    }

    /**
     * @return all shots
     */
    @Nonnull
    public List<Shot> getShots() {
        List<Shot> shots = Lists.newArrayList(this.getHitShots());
        shots.addAll(this.getMissedShots());
        return shots;
    }

    /**
     * @return the ship placements for every player
     */
    @Nonnull
    public Map<Integer, Map<Integer, PlacementInfo>> getStartShip() {
        return startShip;
    }

    /**
     * @return the score
     */
    @Nonnull
    public Map<Integer, Integer> getScore() {
        return score;
    }

    /**
     * adds a ship placement configuration for a player
     * @param clientId id of the player
     * @param ships map from ship id to placementinfo
     * @throws GameException if to many ships have been placed or the ships for the player has already been placed
     */
    public void addShipPlacement(int clientId,@Nonnull Map<Integer, PlacementInfo> ships) throws GameException {
            if (ships.size() > getConfiguration().getShips().size()) {
                LOGGER.debug("Client {} would have set to many ships", clientId);
                throw new NotAllowedException("game.player.toManyShips");
            }
            this.startShip.put(clientId, ships);
            if (ships.size() < getConfiguration().getShips().size()) {
                LOGGER.debug("Client {} set to less ships", clientId);
                throw new InvalidActionException("game.player.toLessShips");
            }
            LOGGER.debug("Ships placed successful for player {}", clientId);
    }

    /**
     * adds shots placement for a player
     * @param clientId id of the player
     * @param shots all shots from the player
     * @throws GameException if to many shots have been placed or the shots for the player has already been placed
     */
    public void addShotPlacement(int clientId,@Nonnull Collection<Shot> shots) throws GameException {
        if (shots.size() > getConfiguration().getShotCount()) {
            throw new NotAllowedException("game.player.toManyShots");
        }
        for (Shot shot: shots){
            if(!playersById.containsKey(shot.getClientId())) {
                shots.remove(shot);
                LOGGER.warn("Player {} for shot from {} does not exist", shot.getClientId(), clientId);
            }
        }
        this.shots.put(clientId, shots);
        if (shots.size() < getConfiguration().getShotCount()) {
            throw new InvalidActionException("game.player.toLessShots");
        }
    }


//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------

    private long timeStamp = 0L;
    private long pauseTimeCache = 0L;
    private GameStage stage = GameStage.START;
    private List<Integer> deadPlayer = Lists.newArrayList();

    /**
     * main method, called every tick
     * only run when {@link Game} has {@link GameState#IN_PROGRESS}
     * <p>
     * {@link GameStage#START}:
     * does everything which should be done beforehand.
     * pauses for 1 sec<p>
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
                if (timeStamp < System.currentTimeMillis() - 1000L) {
                    this.startGame();
                    this.sendUpdateNotification();
                    this.createEmptyScore();
                }
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
                    this.stage = GameStage.SHOTS;
                    this.timeStamp = System.currentTimeMillis();
                    this.sendUpdateNotification();

                    this.deadPlayer.forEach(clientId -> {
                        LOGGER.info(ServerMarker.GAME, "{} has lost",clientId);
                        this.ships.remove(clientId);
                        this.removeDeadPlayer(clientId);
                    });
                    if(ships.size() <= 1){
                        this.stage = GameStage.FINISHED;
                    }else {
                        clientManager.sendMessageToClients(new RoundStartNotification(), getAllClients());
                    }
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
                LOGGER.debug("Game {} has finished",game.getId());
                this.clientManager.sendMessageToClients(new FinishNotification(this.score, id),getAllClients());
                this.game.setState(GameState.FINISHED);
                break;
            default:
                break;
        }
    }

    /**
     * set games state to {@link GameState#IN_PROGRESS} & stage to {@link GameStage#START}
     * @return {@code false} if player count is under 2
     */
    public boolean launchGame() {
        if (this.game.getState() == GameState.LOBBY) {
            if (this.playersById.size() < MIN_PLAYER_COUNT) {
                return false;
            }
            this.game.setState(GameState.IN_PROGRESS);
            this.stage = GameStage.START;
            this.timeStamp = System.currentTimeMillis();
        }
        return true;
    }

    /**
     * sets game stage to {@link GameStage#PLACESHIPS} & sends {@link GameInitNotification} & start round
     */
    private void startGame() {
        if (this.stage.equals(GameStage.START)) {
            LOGGER.debug("Game {} started",game.getId());
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
     * stops game
     * @param points if {@code false} all points will be set to 0
     */
    public void abortGame(boolean points){
        if(this.game.getState() != GameState.FINISHED){
            this.game.setState(GameState.FINISHED);
            if(!points){
                this.createEmptyScore();
            }
            this.sendUpdateNotification();
            this.getAllClients().forEach(client ->this.clientManager.disconnect(client.getId()));
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
            Field field = this.fieldsByPlayerId.get(clientEntry.getKey());
            for (Map.Entry<Integer, PlacementInfo> shipEntry : clientEntry.getValue().entrySet()) {
                Ship ship = field.placeShip(ships.get(shipEntry.getKey()), shipEntry.getValue());
                if(ship != null) {//TODO ship can't be placed
                    this.ships.computeIfAbsent(clientEntry.getKey(), id -> Maps.newHashMap()).put(shipEntry.getKey(), ship);
                }
            }
            if(clientEntry.getValue().size() < getConfiguration().getShips().size()){
                this.placeRandomShips(clientEntry.getKey(),clientEntry.getValue());
            }
        }
        List<Client> clients1 = playersById.entrySet().stream().filter(integerClientEntry -> !clients.contains(integerClientEntry.getKey())).map(Map.Entry::getValue).collect(Collectors.toList());
        this.removeInactivePlayer(clients1);
    }

    /**
     * perform all shots in {@link #shots}
     */
    private void performShots() {
        List<Ship> sunkShips = Lists.newArrayList();
        Map<HitType, Map<Shot, List<Integer>>> hitToPoint = Maps.newHashMap();
        for (Map.Entry<Integer, Collection<Shot>> entry : shots.entrySet()) {
            for (Shot shot : entry.getValue()) {
                if (shot.getClientId() == entry.getKey()) {
                    this.clientManager.sendMessageToInt(new ErrorNotification(ErrorType.INVALID_ACTION, ShotsRequest.MESSAGE_ID, translate("game.gameManager.shotOwnShip")), entry.getKey());
                    continue;
                }
                ShotHit hit = fieldsByPlayerId.get(shot.getClientId()).hit(shot);
                switch (hit.getHitType()) {
                    case HIT:
                        hitToPoint.computeIfAbsent(HitType.HIT, hitType -> Maps.newHashMap()).computeIfAbsent(hit.getShot(), shot1 -> Lists.newArrayList()).add(entry.getKey());
                        this.hitShots.add(shot);
                        this.shipToShots.computeIfAbsent(hit.getShip(), (ship -> Lists.newArrayList())).add(hit.getShot());
                        break;
                    case SUNK:
                        hitToPoint.computeIfAbsent(HitType.SUNK, hitType -> Maps.newHashMap()).computeIfAbsent(hit.getShot(), point2D -> Lists.newArrayList()).add(entry.getKey());
                        this.hitShots.add(shot);
                        this.shipToShots.computeIfAbsent(hit.getShip(), (ship -> Lists.newArrayList())).add(hit.getShot());
                        sunkShips.add(hit.getShip());
                        LOGGER.info(ServerMarker.INGAME,"Ship has been sunk of {}",shot.getClientId());
                        break;
                    case NONE:
                        for (Shot shot1 : this.hitShots) {
                            if (shot1.equals(shot)) {
                                this.clientManager.sendMessageToInt(new ErrorNotification(ErrorType.INVALID_ACTION, ShotsRequest.MESSAGE_ID, translate("game.gameManager.alreadyHit")), entry.getKey());
                                break;
                            }
                        }
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
            for(Map.Entry<Shot, List<Integer>> entry : hitToPoint.get(HitType.SUNK).entrySet()) {
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
        Map<Integer,List<Integer>> remove = Maps.newHashMap();
        this.ships.forEach((clientId, ships) -> ships.forEach((shipId, ship) -> {
            if(sunkShips.contains(ship)){
                remove.computeIfAbsent(clientId, id -> Lists.newArrayList()).add(shipId);
            }
        }));
        remove.forEach((clientId, ships) -> ships.forEach( ship -> this.ships.get(clientId).remove(ship)));
        this.ships.forEach((clientId, ships)->{
            if(ships.isEmpty()){
                this.deadPlayer.add(clientId);
            }
        });
        sunkShips.forEach((ship -> this.sunkShots.addAll(this.shipToShots.get(ship))));
        this.shots.clear();
    }

    /**
     * removed dead player
     * @param clientId
     */
    private void removeDeadPlayer(int clientId){//TODO what should be done if player is dead
        this.removeClient(clientId);
        clientManager.sendMessageToClients(new LeaveNotification(clientId), this.getAllClients());
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

    /**
     * send spectator & player update notifications
     */
    private void sendUpdateNotification(){
        this.clientManager.sendMessageToClients(new PlayerUpdateNotification(this.hitShots, score, this.sunkShots), this.playersById.values());
        this.clientManager.sendMessageToClients(new SpectatorUpdateNotification(this.hitShots, this.score, this.sunkShots, this.missedShots), this.spectatorsById.values());
    }

    /**
     * place all not placed ships randomly to their field
     * @param client client id
     * @param alreadyPlaced all already placed ships
     */
    private void placeRandomShips(int client, Map<Integer, PlacementInfo> alreadyPlaced){
        //TODO fill
    }

    private void createEmptyScore(){
        this.score.clear();
        this.playersById.forEach((id, client)->score.put(id,0));
    }

    public GameStage getStage() {
        return stage;
    }
}
