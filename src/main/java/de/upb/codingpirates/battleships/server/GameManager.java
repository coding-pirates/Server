package de.upb.codingpirates.battleships.server;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.logic.Configuration;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.id.IntIdManager;
import de.upb.codingpirates.battleships.network.message.notification.ContinueNotification;
import de.upb.codingpirates.battleships.network.message.notification.GameInitNotification;
import de.upb.codingpirates.battleships.network.message.notification.PauseNotification;
import de.upb.codingpirates.battleships.server.game.GameHandler;
import de.upb.codingpirates.battleships.server.network.ServerApplication;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

/**
 * handles all game related tasks
 * <p>
 * get an instance with {@link ServerApplication#getGameManager()}
 */
public class GameManager {
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


    public int createGame(@Nonnull Configuration configuration, String name, boolean tournament) {
        int id = this.idManager.generate().getInt();
        games.putIfAbsent(id, new GameHandler(name, id, configuration, tournament));
        return id;
    }

    public void addClientToGame(int gameId, Client client, ClientType clientType) throws InvalidActionException {
        if (games.containsKey(gameId)) {
            if (!games.get(gameId).addClient(clientType, client)) {
                throw new InvalidActionException("The game is full");
            }
            clientToGame.putIfAbsent(client.getId(), gameId);
        } else {
            throw new InvalidActionException("The game does not exist");
        }
    }

    public void removeClientFromGame(int client) throws InvalidActionException {
        if (clientToGame.containsKey(client)) {
            games.get(clientToGame.remove(client)).removeClient(client);
        } else {
            throw new InvalidActionException("Client does not belong to a game");
        }
    }

    public void launchGame(int gameId) {
        games.get(gameId).launchGame();
        clientManager.sendMessageToClients(new GameInitNotification(games.get(gameId).getAllClients(), games.get(gameId).getConfiguration()), games.get(gameId).getAllClients());
    }

    public void pauseGame(int gameId) {
        games.get(gameId).pauseGame();
        clientManager.sendMessageToClients(new PauseNotification(), games.get(gameId).getAllClients());
    }

    public void continueGame(int gameId) {
        games.get(gameId).continueGame();
        clientManager.sendMessageToClients(new ContinueNotification(), games.get(gameId).getAllClients());
    }

    public Collection<GameHandler> getAllGames() {
        return games.values();
    }

    public GameHandler getGameHandlerForClientId(int clientId) throws InvalidActionException {
        if (!clientToGame.containsKey(clientId)) {
            throw new InvalidActionException("This client does not participate in a game");
        }
        return games.get(clientToGame.get(clientId));
    }

    @Nullable
    public GameHandler getGame(int id) {
        return games.get(id);
    }

    public void run() {
        games.values().forEach(GameHandler::run);
    }
}
