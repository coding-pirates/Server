package de.upb.codingpirates.battleships.server;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.id.IntId;
import de.upb.codingpirates.battleships.network.id.IntIdManager;
import de.upb.codingpirates.battleships.server.game.GameHandler;
import de.upb.codingpirates.battleships.server.network.ServerApplication;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    private static Map<Id, GameHandler> games = Maps.newHashMap();
    private static Map<Id, GameHandler> clientToGame = Maps.newHashMap();

    public Id createGame(@Nonnull Configuration configuration, String name) {
        Id id = this.idManager.generate();
        games.putIfAbsent(id, new GameHandler(configuration, name, id));
        return id;
    }

    /**
     * @return {@code false} if game does not exist or client already exists in the game
     */
    public boolean addClientToGame(Id gameId, Client client, ClientType clientType) {
        if (games.containsKey(gameId)) {
            games.get(gameId).addClient(client, clientType);
            clientToGame.putIfAbsent(new IntId(client.getId()), games.get(gameId));
            return true;
        }
        return false;
    }

    /**
     * @return {@code false} if game does not exist
     */
    public boolean removeClientFromGame(Id gameId, Client client) {
        if (games.containsKey(gameId)) {
            games.get(gameId).removeClient(client);
            clientToGame.remove(client.getId());
            return true;
        }
        return false;
    }

    public GameHandler getGameManagerForClientId(Id clientId) {
        return clientToGame.get(clientId);
    }

    @Nullable
    public GameHandler getGame(Id id) {
        return games.get(id);
    }

    public void run() {
        games.values().forEach(GameHandler::run);
    }
}
