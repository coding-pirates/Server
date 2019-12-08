package de.upb.codingpirates.battleships.server;

import java.util.*;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.upb.codingpirates.battleships.logic.*;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.id.IntIdManager;
import de.upb.codingpirates.battleships.network.message.notification.ContinueNotification;
import de.upb.codingpirates.battleships.network.message.notification.PauseNotification;
import de.upb.codingpirates.battleships.server.game.GameHandler;
import de.upb.codingpirates.battleships.server.network.ServerApplication;
import de.upb.codingpirates.battleships.server.util.Markers;

/**
 * handles all game related tasks
 * <p>
 * get an instance with {@link ServerApplication#getGameManager()}
 */
public class GameManager {
    private static final Logger LOGGER = LogManager.getLogger();

    @Nonnull
    private final ClientManager clientManager;
    @Nonnull
    private final IntIdManager idManager;

    /**
     * Maps ids associated with {@link Game} objects to their {@link GameHandler}.
     */
    private final ObservableMap<Integer, GameHandler> games =
        FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());

    /**
     * Maps client ids to the ids of {@link Game}s they are participating in.
     */
    private final Map<Integer, Integer> clientToGame = Collections.synchronizedMap(Maps.newHashMap());

    @Inject
    public GameManager(@Nonnull ClientManager handler, @Nonnull IntIdManager idManager) {
        this.clientManager = handler;
        this.idManager = idManager;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                GameManager.this.run();
            }
        }, 1L, 1L);
    }

    /**
     * creates game based on parameter
     *
     * @param configuration
     * @param name
     * @param tournament
     * @return {@code -1} if game was created successful, {@code > 0} if the selected field size of the Configuration is too small
     */
    public int createGame(@Nonnull Configuration configuration, @Nonnull String name, boolean tournament) {
        LOGGER.info(configuration.getShipTypes().get(0));
        int size = checkField(configuration);
        if (size != -1) {
            return size;
        }
        int id = this.idManager.generate().getInt();
        LOGGER.debug(Markers.GAME, "Create game: {} with id: {}", name, id);
        this.games.putIfAbsent(id, new GameHandler(name, id, configuration, tournament, clientManager));
        return -1;
    }

    /**
     * adds client with clientType to the specific game
     *
     * @param gameId
     * @param client
     * @param clientType
     * @throws InvalidActionException if game does not exist
     */
    public void addClientToGame(int gameId, @Nonnull Client client, @Nonnull ClientType clientType) throws InvalidActionException {
        LOGGER.debug(Markers.GAME, "Adding client {}, with type {}, to game {}", client.getId(), clientType, gameId);

        if (!this.games.containsKey(gameId)) {
            LOGGER.error(Markers.GAME, "Can't find game {}", gameId);
            throw new InvalidActionException("game.gameManager.noGame");
        }
        this.games.get(gameId).addClient(clientType, client);
        this.clientToGame.putIfAbsent(client.getId(), gameId);
    }

    /**
     * Removes the {@link Client} with the provided {@code clientId} from the game it is participating in.
     *
     * @param clientId The id of the {@link Client} that is to be removed from its game.
     *
     * @throws InvalidActionException If the {@link Client} belonging to the provided {@code clientId} is not currently
     *                                participating in a game.
     */
    public void removeClientFromGame(int clientId) throws InvalidActionException {
        LOGGER.debug(Markers.CLIENT, "Remove client {} from active game", clientId);
        if (clientToGame.containsKey(clientId)) {
            this.games.get(this.clientToGame.remove(clientId)).removeClient(clientId);
        } else {
            LOGGER.warn(Markers.CLIENT, "Client {} does not participate in a game", clientId);
            throw new InvalidActionException("game.gameManager.noGameForClient");
        }
    }

    /**
     * Launches the {@link Game} associated with the provided {@code gameId} under the circumstance that its current
     * player count is at least two.
     *
     * @param gameId The id associated with the {@link Game} which is to be launched.
     *
     * @return {@code true} if the game was successfully launched, otherwise {@code false}.
     */
    public boolean launchGame(int gameId) {
        LOGGER.debug(Markers.GAME, "launched game {}, {}", gameId, this.games.get(gameId).getGame().getName());
        return this.games.get(gameId).launchGame();
    }

    /**
     * pauses game with id
     *
     * @param gameId gameId
     *               
     * @see #continueGame(int) 
     */
    public void pauseGame(int gameId) {
        LOGGER.debug(Markers.GAME, "paused game {}, {}", gameId, this.games.get(gameId).getGame().getName());
        this.games.get(gameId).pauseGame();
        clientManager.sendMessageToClients(new PauseNotification(), this.games.get(gameId).getAllClients());
    }

    /**
     * Continues the (previously paused) {@link Game} associated with the provided {@code gameId}.
     *
     * @param gameId The id of the {@link Game} that is to be continued.
     *               
     * @see #pauseGame(int) 
     */
    public void continueGame(int gameId) {
        LOGGER.debug(Markers.GAME, "Continuing game {}, {}", gameId, this.games.get(gameId).getGame().getName());
        this.games.get(gameId).continueGame();
        clientManager.sendMessageToClients(new ContinueNotification(), this.games.get(gameId).getAllClients());
    }

    /**
     * Aborts the {@link Game} associated with the provided {@code gameId}.
     *
     * @param gameId The id of the {@link Game} that is to be aborted.
     * @param points if {@code false} all points will be set to 0
     */
    public void abortGame(int gameId, boolean points) {
        LOGGER.debug(Markers.GAME, "Aborting game {}, {}", gameId, this.games.get(gameId).getGame().getName());
        this.games.get(gameId).abortGame(points);
    }

    /**
     * @param clientId for which a game should be found
     * @return a game where the client participate
     * @throws InvalidActionException if the client does not participate in a game
     */
    @Nonnull
    public GameHandler getGameHandlerForClientId(int clientId) throws InvalidActionException {
        if (!this.clientToGame.containsKey(clientId)) {
            LOGGER.warn(Markers.CLIENT, "Could not get game for client {}", clientId);
            throw new InvalidActionException("game.gameManager.noGameForClient");
        }
        return this.games.get(this.clientToGame.get(clientId));
    }

    /**
     * @param id id of the game
     * @return the game with the id
     * @throws InvalidActionException if the game does not exist
     */
    @Nonnull
    public GameHandler getGame(int id) throws InvalidActionException {
        if (!this.games.containsKey(id)) {
            LOGGER.warn(Markers.GAME, "The game with id: {} does not exist", id);
            throw new InvalidActionException("game.gameManager.gameNotExist");
        }
        return this.games.get(id);
    }

    /**
     * run method for every game
     */
    private void run() {
        this.games.values().forEach(GameHandler::run);
    }

    /**
     * checks if the ships can fit into the field
     *
     * @param configuration the configuration which should be checked
     * @return {@code -1} if it fits, else recommendation for a field size;
     */
    private int checkField(@Nonnull Configuration configuration) {//TODO better algorithm
        Collection<ShipType> ships = configuration.getShips().values();

        List<BoundingBox> boxes = Lists.newArrayList();

        for (ShipType ship : ships) {
            int x = ship.getPositions().stream().max((a, b) -> Math.max(Math.abs(a.getX()), Math.abs(b.getX()))).get().getX();
            int y = ship.getPositions().stream().max((a, b) -> Math.max(Math.abs(a.getY()), Math.abs(b.getY()))).get().getY();
            boxes.add(new BoundingBox(x + 1, y + 1));
        }

        int maxFields = boxes.stream().mapToInt(BoundingBox::getSize).sum();
        if (maxFields > configuration.getHeight() * configuration.getWidth()) {
            return (int) Math.sqrt(maxFields);
        }
        return -1;
    }

    public Collection<GameHandler> getGameHandlers() {
        return games.values();
    }

    public ObservableMap<Integer, GameHandler> getGameMappings() {
        return games;
    }
}
