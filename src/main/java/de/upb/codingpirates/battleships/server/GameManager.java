package de.upb.codingpirates.battleships.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import de.upb.codingpirates.battleships.logic.*;
import de.upb.codingpirates.battleships.network.ConnectionHandler;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.id.IntIdManager;
import de.upb.codingpirates.battleships.network.message.notification.ContinueNotification;
import de.upb.codingpirates.battleships.network.message.notification.PauseNotification;
import de.upb.codingpirates.battleships.server.game.GameHandler;
import de.upb.codingpirates.battleships.server.network.ServerApplication;
import de.upb.codingpirates.battleships.server.util.Markers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.*;

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
     * maps game id to gamehandler
     */
    private final Map<Integer, GameHandler> games = Collections.synchronizedMap(Maps.newHashMap());
    /**
     * maps client id to gameid
     */
    private final Map<Integer, Integer> clientToGame = Collections.synchronizedMap(Maps.newHashMap());

    @Inject
    public GameManager(@Nonnull ConnectionHandler handler, @Nonnull IntIdManager idManager) {
        this.clientManager = (ClientManager) handler;
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
        if (this.games.containsKey(gameId)) {
            this.games.get(gameId).addClient(clientType, client);
            this.clientToGame.putIfAbsent(client.getId(), gameId);
        } else {
            LOGGER.error(Markers.GAME, "Can't find game {}", gameId);
            throw new InvalidActionException("game.gameManager.noGame");
        }
    }

    /**
     * removes client from participating games
     *
     * @param client
     * @throws InvalidActionException if client does not participate
     */
    public void removeClientFromGame(int client) throws InvalidActionException {
        LOGGER.debug(Markers.CLIENT, "Remove client {} from active game", client);
        if (clientToGame.containsKey(client)) {
            this.games.get(this.clientToGame.remove(client)).removeClient(client);
        } else {
            LOGGER.warn(Markers.CLIENT, "Client {} does not participate in a game", client);
            throw new InvalidActionException("game.gameManager.noGameForClient");
        }
    }

    /**
     * launches game with id
     *
     * @param gameId gameId
     * @return {@code false} if player count is under 2
     */
    public boolean launchGame(int gameId) {
        LOGGER.debug(Markers.GAME, "launched game {}, {}", gameId, this.games.get(gameId).getGame().getName());
        return this.games.get(gameId).launchGame();
    }

    /**
     * pauses game with id
     *
     * @param gameId gameId
     */
    public void pauseGame(int gameId) {
        LOGGER.debug(Markers.GAME, "paused game {}, {}", gameId, this.games.get(gameId).getGame().getName());
        this.games.get(gameId).pauseGame();
        clientManager.sendMessageToClients(new PauseNotification(), this.games.get(gameId).getAllClients());
    }

    /**
     * continue game with id
     *
     * @param gameId gameId
     */
    public void continueGame(int gameId) {
        LOGGER.debug(Markers.GAME, "continued game {}, {}", gameId, this.games.get(gameId).getGame().getName());
        this.games.get(gameId).continueGame();
        clientManager.sendMessageToClients(new ContinueNotification(), this.games.get(gameId).getAllClients());
    }

    /**
     * continue game with id
     *
     * @param gameId gameId
     * @param points if {@code false} all points will be set to 0
     */
    public void abortGame(int gameId, boolean points) {
        LOGGER.debug(Markers.GAME, "abort game {}, {}", gameId, this.games.get(gameId).getGame().getName());
        this.games.get(gameId).abortGame(points);
    }

    /**
     * @return all existing games
     */
    public Collection<GameHandler> getAllGames() {
        return this.games.values();
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

}
