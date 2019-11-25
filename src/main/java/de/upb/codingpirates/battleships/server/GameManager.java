package de.upb.codingpirates.battleships.server;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.logic.Configuration;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.id.IntIdManager;
import de.upb.codingpirates.battleships.network.message.notification.ContinueNotification;
import de.upb.codingpirates.battleships.network.message.notification.PauseNotification;
import de.upb.codingpirates.battleships.server.game.GameHandler;
import de.upb.codingpirates.battleships.server.network.ServerApplication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * handles all game related tasks
 * <p>
 * get an instance with {@link ServerApplication#getGameManager()}
 */
public class GameManager {
    private static final Logger LOGGER = LogManager.getLogger();

    @Inject
    private ClientManager clientManager;
    @Inject
    private IntIdManager idManager;

    /**
     * maps game id to gamehandler
     */
    private final Map<Integer, GameHandler> games = Maps.newHashMap();
    /**
     * maps client id to gameid
     */
    private final Map<Integer, Integer> clientToGame = Maps.newHashMap();

    public GameManager() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                GameManager.this.run();
            }
        },1L,1L);
    }

    public int createGame(@Nonnull Configuration configuration, @Nonnull String name, boolean tournament) {
        LOGGER.debug("Create game: {}", name);
        int id = this.idManager.generate().getInt();
        games.putIfAbsent(id, new GameHandler(name, id, configuration, tournament, clientManager));
        return id;
    }

    public void addClientToGame(int gameId, Client client, ClientType clientType) throws InvalidActionException {
        LOGGER.debug("Adding client {}, with type {}, to game {}", client.getId(), clientType,gameId);
        if (games.containsKey(gameId)) {
            games.get(gameId).addClient(clientType, client);
            clientToGame.putIfAbsent(client.getId(), gameId);
        } else {
            LOGGER.error("Can't find game {}", gameId);
            throw new InvalidActionException("game.gameManager.noGame");
        }
    }

    public void removeClientFromGame(int client) throws InvalidActionException {
        LOGGER.debug("Remove client {} from active game", client);
        if (clientToGame.containsKey(client)) {
            games.get(clientToGame.remove(client)).removeClient(client);
        } else {
            LOGGER.warn("Client {} does not participate in a game", client);
            throw new InvalidActionException("game.gameManager.noGameForClient");
        }
    }

    public void launchGame(int gameId) {
        LOGGER.debug("launched game {}, {}", gameId, games.get(gameId).getGame().getName());
        games.get(gameId).launchGame();
    }

    public void pauseGame(int gameId) {
        LOGGER.debug("paused game {}, {}", gameId, games.get(gameId).getGame().getName());
        games.get(gameId).pauseGame();
        clientManager.sendMessageToClients(new PauseNotification(), games.get(gameId).getAllClients());
    }

    public void continueGame(int gameId) {
        LOGGER.debug("continued game {}, {}", gameId, games.get(gameId).getGame().getName());
        games.get(gameId).continueGame();
        clientManager.sendMessageToClients(new ContinueNotification(), games.get(gameId).getAllClients());
    }

    public Collection<GameHandler> getAllGames() {
        return games.values();
    }

    public GameHandler getGameHandlerForClientId(int clientId) throws InvalidActionException {
        if (!clientToGame.containsKey(clientId)) {
            LOGGER.warn("Could not get game for client {}",clientId);
            throw new InvalidActionException("game.gameManager.noGameForClient");
        }
        return games.get(clientToGame.get(clientId));
    }

    @Nonnull
    public GameHandler getGame(int id) throws InvalidActionException {
        if(!games.containsKey(id)){
            LOGGER.warn("The game with id: {} does not exist",id);
            throw new InvalidActionException("game.gameManager.gameNotExist");
        }
        return games.get(id);
    }

    public void run() {
        games.values().forEach(GameHandler::run);
    }
}
