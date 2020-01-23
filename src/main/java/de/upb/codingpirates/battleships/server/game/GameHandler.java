package de.upb.codingpirates.battleships.server.game;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.logic.*;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.exceptions.game.NotAllowedException;
import de.upb.codingpirates.battleships.network.message.notification.*;
import de.upb.codingpirates.battleships.network.message.request.PlaceShipsRequest;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.exceptions.GameFullExeption;
import de.upb.codingpirates.battleships.server.util.GameListener;
import de.upb.codingpirates.battleships.server.util.ServerMarker;
import de.upb.codingpirates.battleships.server.util.Translator;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static de.upb.codingpirates.battleships.server.util.ServerProperties.MAX_SPECTATOR_COUNT;
import static de.upb.codingpirates.battleships.server.util.ServerProperties.MIN_PLAYER_COUNT;

/**
 * @author Paul Becker
 */
public class GameHandler implements Runnable, Translator {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nonnull
    private final ClientManager clientManager;

    @Nonnull
    private final GameManager gameManager;

    @Nullable
    private final GameListener gameListener;

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
    private final Map<Integer, AbstractClient> spectatorsById = Collections.synchronizedMap(Maps.newHashMap());

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
     * maps from ship to shots that hit the ship
     */
    @Nonnull
    private final Map<Ship, List<Shot>> shipToShots = Collections.synchronizedMap(Maps.newHashMap());

    public GameHandler(@Nonnull final String name, final int id, @Nonnull final Configuration config, final boolean tournament, @Nonnull final ClientManager clientManager, @Nonnull GameManager gameManager, @Nullable GameListener gameListener) {
        this.game          = new Game(id, name, GameState.LOBBY, config, tournament);
        this.clientManager = clientManager;
        this.gameManager   = gameManager;
        this.gameListener  = gameListener;

        currentPlayerCountProperty
            .addListener((observable, oldValue, newValue) -> game.setCurrentPlayerCount(newValue.intValue()));
        stateProperty
            .addListener((observable, oldValue, newValue) -> game.setState(newValue));
    }

    public GameHandler(@Nonnull final String name, final int id, @Nonnull final Configuration config, final boolean tournament, @Nonnull final ClientManager clientManager, @Nonnull GameManager gameManager) {
        this(name, id, config, tournament, clientManager, gameManager, null);
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

    public int getCurrentPlayerCount() {
        return currentPlayerCountProperty.get();
    }

    public void setCurrentPlayerCount(final int currentPlayerCount) {
        currentPlayerCountProperty.set(currentPlayerCount);
    }

    public IntegerProperty currentPlayerCountProperty() {
        return currentPlayerCountProperty;
    }
    // </editor-fold>

    // <editor-fold desc="stateProperty">
    private final ObjectProperty<GameState> stateProperty = new SimpleObjectProperty<>(GameState.LOBBY);

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
     * @throws GameFullExeption if game is full
     */
    public void addClient(@Nonnull AbstractClient client) throws InvalidActionException {
        switch (client.handleClientAs()) {
            case PLAYER:
                if (playersById.size() >= game.getConfig().getMaxPlayerCount())
                    throw new GameFullExeption();
                playersById.put(client.getId(), (Client) client);
                fieldsByPlayerId.put(client.getId(), new Field(getGame().getConfig().getHeight(), getGame().getConfig().getWidth(),client.getId()));
                currentPlayerCountProperty.set(currentPlayerCountProperty.get() + 1);
                break;
            case SPECTATOR:
                if (spectatorsById.size() >= MAX_SPECTATOR_COUNT)
                    throw new GameFullExeption();
                spectatorsById.putIfAbsent(client.getId(), (Spectator) client);
                break;
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

            this.testGameFinished();
            currentPlayerCountProperty.set(getCurrentPlayerCount() - 1);
            clientManager.sendMessageToClients(NotificationBuilder.leaveNotification(clientId), getAllClients());
        }
        this.spectatorsById.remove(clientId);
    }

    /**
     * @return A {@link List} containing all {@link Client}s which are participating in the {@link Game} wrapped by this
     *         {@code GameHandler} or which are spectating it.
     */
    @Nonnull
    public List<AbstractClient> getAllClients() {
        final List<AbstractClient> clients = new ArrayList<>(getPlayers().size() + getSpectators().size());

        clients.addAll(this.getPlayers());
        clients.addAll(this.getSpectators());

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
    public Collection<AbstractClient> getSpectators() {
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
    public Configuration getConfiguration() {
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
    public void addShipPlacement(int clientId, @Nonnull Map<Integer, PlacementInfo> ships) throws GameException {
        if(this.getStage().equals(GameStage.PLACESHIPS)) {
            if (ships.size() > getConfiguration().getShips().size()) {
                LOGGER.debug("Client {} would have set to many ships", clientId);
                applyPenalty(clientId);
                throw new NotAllowedException("game.player.toManyShips");
            }
            this.startShip.put(clientId, ships);
            if (ships.size() < getConfiguration().getShips().size()) {
                LOGGER.debug("Client {} set to less ships", clientId);
                throw new InvalidActionException("game.player.toLessShips");
            }
            LOGGER.debug("Ships placed successful for player {}", clientId);
        }else {
            throw new InvalidActionException("Its not the time to place ships");
        }
    }

    private void applyPenalty(int clientId){
        switch (getConfiguration().getPenaltyKind()){
            case KICK:
                this.removeInactivePlayer(clientId);
            case POINTLOSS:
                this.score.compute(clientId, (clientId1, score) -> score == null?-getConfiguration().getPenaltyMinusPoints():score - getConfiguration().getPenaltyMinusPoints());
                break;
            default:
                break;
        }
    }

    /**
     * adds shots placement for a player
     * @param clientId id of the player
     * @param shots all shots from the player
     * @throws GameException if to many shots have been placed or the shots for the player has already been placed
     */
    public void addShotPlacement(int clientId,@Nonnull Collection<Shot> shots) throws GameException {
        if (shots.size() > getConfiguration().getShotCount()) {
            applyPenalty(clientId);
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

    private static final List<Shot> EMPTY = ImmutableList.of();
    private long timeStamp = 0L;
    private long pauseTimeCache = 0L;
    private GameStage stage = GameStage.START;
    private List<Client> livingPlayer = Lists.newArrayList();
    private List<Client> deadPlayer = Lists.newArrayList();
    private List<Shot> newHits = Lists.newArrayList();

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
    @Override
    public void run() {
        if (getState() != GameState.IN_PROGRESS)
            return;
        switch (this.stage) {
            case START:
                if (timeStamp < System.currentTimeMillis() - 1000L) {
                    this.startGame();
                    this.sendUpdateNotification(EMPTY);
                    this.createEmptyScore();
                    this.livingPlayer.clear();
                    this.deadPlayer.clear();
                    this.livingPlayer.addAll(playersById.values());
                }
                break;
            case PLACESHIPS:
                if (System.currentTimeMillis() - timeStamp >= getConfiguration().getRoundTime()) {
                    this.placeShips();
                    this.timeStamp = System.currentTimeMillis();
                    this.clientManager.sendMessageToClients(NotificationBuilder.gameStartNotification(),getAllClients());
                    this.stage = GameStage.SHOTS;
                }
                break;
            case VISUALIZATION:
                if (System.currentTimeMillis() - timeStamp >= getConfiguration().getVisualizationTime()) {
                    this.stage = GameStage.SHOTS;
                    this.timeStamp = System.currentTimeMillis();

                    this.deadPlayer.forEach(this::removeDeadPlayer);
                }
                break;
            case SHOTS:
                if (System.currentTimeMillis() - timeStamp >= getConfiguration().getRoundTime()) {
                    this.performShots();
                    this.sendUpdateNotification(newHits);
                    this.timeStamp = System.currentTimeMillis();
                    this.stage = GameStage.VISUALIZATION;
                }
                break;
            case FINISHED:
                LOGGER.debug("Game {} has finished",game.getId());
                this.clientManager.sendMessageToClients(NotificationBuilder.finishNotification(this.score, getWinner()), getAllClients());
                this.game.setState(GameState.FINISHED);
                if(this.gameListener != null)
                    this.gameListener.onGameFinished();
                this.gameManager.gameFinished(this.game.getId());
                break;
            default:
                break;
        }
    }

    public Collection<Integer> getWinner(){
        OptionalInt winnerScore = score.values().stream().mapToInt(value -> value).max();
        Collection<Integer> winner = Lists.newArrayList();
        if(winnerScore.isPresent())
            winner.addAll(score.entrySet().stream().filter(entry -> entry.getValue() == winnerScore.getAsInt()).map(Map.Entry::getKey).collect(Collectors.toList()));
        return winner;
    }

    /**
     * set games state to {@link GameState#IN_PROGRESS} & stage to {@link GameStage#START}
     * @return {@code false} if player count is under 2
     */
    public boolean launchGame() {
        if (getState() == GameState.LOBBY) {
            if (this.playersById.size() < MIN_PLAYER_COUNT) {
                return false;
            }
            this.setState(GameState.IN_PROGRESS);
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
            this.playersById.values().forEach(client -> client.setDead(false));
            this.clientManager.sendMessageToClients(NotificationBuilder.gameInitNotification(getPlayers(), this.getConfiguration()), getAllClients());
        }
    }

    /**
     * saves remaining time of the round and pauses the game
     */
    public void pauseGame() {
        LOGGER.debug(ServerMarker.GAME, "paused game {}, {}", this.game.getId(), this.game.getName());
        if (getState() == GameState.IN_PROGRESS) {
            this.setState(GameState.PAUSED);
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
            this.clientManager.sendMessageToClients(NotificationBuilder.pauseNotification(), this.getAllClients());
        }
    }

    /**
     * uses saved remain time to return to game
     */
    public void continueGame() {
        LOGGER.debug(ServerMarker.GAME, "continued game {}, {}", this.game.getId(), this.game.getName());
        if (getState() == GameState.PAUSED) {
            setState(GameState.IN_PROGRESS);
            this.timeStamp = System.currentTimeMillis() + pauseTimeCache;
            this.clientManager.sendMessageToClients(NotificationBuilder.continueNotification(), this.getAllClients());
        }
    }

    /**
     * stops game
     * @param points if {@code false} all points will be set to 0
     */
    public void abortGame(boolean points) {
        LOGGER.debug(ServerMarker.GAME, "abort game {}, {}", this.game.getId(), this.game.getName());
        if (this.getState() != GameState.FINISHED) {
            this.setState(GameState.FINISHED);
            if (!points) {
                this.createEmptyScore();
            }
            this.sendUpdateNotification(EMPTY);
            this.clientManager.sendMessageToClients(NotificationBuilder.finishNotification(this.score, this.getWinner()),this.getAllClients());
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
        }
        int[] clients1 = playersById.keySet().stream().filter(client -> !clients.contains(client)).mapToInt(i -> i).toArray();
        this.removeInactivePlayer(clients1);
    }

    /**
     * perform all shots in {@link #shots}
     */
    private void performShots() {
        this.newHits.clear();
        List<Ship> sunkShips = Lists.newArrayList();
        Map<Shot,List<Integer>> shotListMap = Maps.newHashMap();
        Map<HitType, List<Shot>> hitToPoint = Maps.newHashMap();
        for (Map.Entry<Integer, Collection<Shot>> entry : shots.entrySet()) {
            for (Shot shot : entry.getValue()) {
                shotListMap.compute(shot, (shot1, list) ->{
                    if(list != null){
                        list.add(entry.getKey());
                        return list;
                    }
                    return Lists.newArrayList(entry.getKey());
                });
                if (shot.getClientId() == entry.getKey()) {
                    this.clientManager.sendMessage(NotificationBuilder.errorNotification(ErrorType.INVALID_ACTION, ShotsRequest.MESSAGE_ID, translate("game.gameManager.shotOwnShip")), entry.getKey());
                    continue;
                }
                ShotHit hit = this.fieldsByPlayerId.get(shot.getClientId()).hit(shot);
                switch (hit.getHitType()) {
                    case HIT:
                        hitToPoint.computeIfAbsent(HitType.HIT, hitType -> Lists.newArrayList()).add(hit.getShot());
                        this.hitShots.add(shot);
                        this.newHits.add(shot);
                        this.shipToShots.computeIfAbsent(hit.getShip(), (ship -> Lists.newArrayList())).add(hit.getShot());
                        break;
                    case SUNK:
                        hitToPoint.computeIfAbsent(HitType.SUNK, hitType -> Lists.newArrayList()).add(hit.getShot());
                        this.hitShots.add(shot);
                        this.newHits.add(shot);
                        this.shipToShots.computeIfAbsent(hit.getShip(), (ship -> Lists.newArrayList())).add(hit.getShot());
                        this.sunkShots.add(shot);
                        sunkShips.add(hit.getShip());
                        LOGGER.info(ServerMarker.INGAME,"Ship has been sunk of {}",shot.getClientId());
                        break;
                    case NONE:
                        for (Shot shot1 : this.hitShots) {
                            if (shot1.equals(shot)) {
                                this.clientManager.sendMessage(NotificationBuilder.errorNotification(ErrorType.INVALID_ACTION, ShotsRequest.MESSAGE_ID, translate("game.gameManager.alreadyHit")), entry.getKey());
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
            for(Shot shot : hitToPoint.get(HitType.HIT)) {
                int points = getConfiguration().getHitPoints() / shotListMap.get(shot).size();
                shotListMap.get(shot).forEach( client ->
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
            for(Shot shot : hitToPoint.get(HitType.SUNK)) {
                int points = getConfiguration().getSunkPoints() / shotListMap.get(shot).size();
                shotListMap.get(shot).forEach( client ->
                        score.compute(client, (id, point) -> {
                            if (point == null) {
                                return points;
                            } else {
                                return point + points;
                            }
                        }));
            }
        }
        this.ships.values().forEach(map -> map.values().removeIf(sunkShips::contains));
        this.shots.clear();
    }

    /**
     * removed dead player
     * @param client
     */
    private void removeDeadPlayer(Client client){
        LOGGER.info(ServerMarker.GAME, "{} has lost",client);
        this.ships.remove(client.getId());
        client.setDead(true);
        this.testGameFinished();
    }

    private void testGameFinished(){
        if(ships.size() <= 1){
            this.stage = GameStage.FINISHED;
        }else {
            clientManager.sendMessageToClients(NotificationBuilder.roundStartNotification(), getAllClients());
        }
    }

    /**
     * removes player that didn't placed their ships
     */
    private void removeInactivePlayer(int... clients) {
        clientManager.sendMessage(NotificationBuilder.errorNotification(ErrorType.INVALID_ACTION, PlaceShipsRequest.MESSAGE_ID, translate("game.player.noPlacedShips")), clients);
        for(Integer clientId: clients){
            this.removeClient(clientId);
        }
    }

    /**
     * @return remaining round time
     * @throws InvalidActionException if there is no round running
     */
    public long getRemainingTime() throws InvalidActionException{
        if (getState() != GameState.IN_PROGRESS && getState() != GameState.PAUSED)
            throw new InvalidActionException("game.noTimerActive");
        if (getState() == GameState.PAUSED)
            return this.pauseTimeCache;

        switch (stage) {
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
    private void sendUpdateNotification(List<Shot> hitShots) {
        this.clientManager.sendMessageToClients(NotificationBuilder.playerUpdateNotification(hitShots, score, this.sunkShots), this.livingPlayer);
        SpectatorUpdateNotification spectatorUpdateNotification = NotificationBuilder.spectatorUpdateNotification(hitShots, this.score, this.sunkShots, this.missedShots);
        this.clientManager.sendMessageToClients(spectatorUpdateNotification, Lists.newArrayList(this.spectatorsById.values()));
        this.clientManager.sendMessageToClients(spectatorUpdateNotification, this.deadPlayer);
    }

    private void createEmptyScore(){
        this.score.clear();
        this.playersById.forEach((id, client)->score.put(id,0));
    }

    public GameStage getStage() {
        return stage;
    }
}
