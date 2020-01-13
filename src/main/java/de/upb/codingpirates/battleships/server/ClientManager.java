package de.upb.codingpirates.battleships.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.logic.AbstractClient;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.logic.Spectator;
import de.upb.codingpirates.battleships.network.ConnectionHandler;
import de.upb.codingpirates.battleships.network.connectionmanager.ServerConnectionManager;
import de.upb.codingpirates.battleships.network.exceptions.BattleshipException;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.notification.NotificationBuilder;
import de.upb.codingpirates.battleships.server.util.ServerMarker;
import de.upb.codingpirates.battleships.server.util.Translator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Handles all {@link Client}-related functionality.
 *
 * @author Paul Becker
 */
public class ClientManager implements ConnectionHandler, Translator {
    private static final Logger LOGGER = LogManager.getLogger();

    @Nonnull
    private final ServerConnectionManager connectionManager;

    /**
     * maps client id to client
     */
    @Nonnull
    private final Map<Integer, AbstractClient> clients = Collections.synchronizedMap(Maps.newHashMap());

    /**
     * maps client id to player
     */
    @Nonnull
    private final ObservableMap<Integer, Client> player =
        FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());

    /**
     * maps client id to spectator
     */
    @Nonnull
    private final Map<Integer, Spectator> spectator = Collections.synchronizedMap(Maps.newHashMap());

    @Inject
    public ClientManager(@Nonnull ServerConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * creates new Client based on parameter
     * @param id id of the new client
     * @param name name of the new client
     * @param clientType clientType of the new client
     * @return new Client
     * @throws InvalidActionException if the id exists
     */
    @Nonnull
    public AbstractClient create(int id,@Nonnull String name,@Nonnull ClientType clientType) throws InvalidActionException {
        LOGGER.debug(ServerMarker.CLIENT, "create client with id: {}, type {}", id, clientType);
        if (this.clients.containsKey(id)) {
            throw new InvalidActionException("game.clientManager.createClient.idExists");
        }

        AbstractClient client;
        switch (clientType) {
            case PLAYER:
                this.player.put(id, (Client)(client = new Client(id, name)));
                break;
            case SPECTATOR:
                this.spectator.put(id, (Spectator)(client = new Spectator(id,name)));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + clientType);
        }
        this.clients.put(id, client);
        return client;
    }

    /**
     * disconnect client with specific id
     *
     * @param clientId
     */
    public void disconnect(int clientId) {
        this.clients.remove(clientId);
        this.player.remove(clientId);
        this.spectator.remove(clientId);

    }

    /**
     * send message to all connected clients
     *
     * @param message
     */
    public void sendMessageToAll(Message message) {
        try {
            for (int id : this.clients.keySet()) {
                this.connectionManager.send(new Id(id), message);
            }
        } catch (IOException e) {
            LOGGER.error(ServerMarker.CONNECTION, "could not send message", e);
        }
    }

    /**
     * send message to all clients represented by the integer ids
     *
     * @param message
     * @param clients
     */
    public void sendMessageToInts(Message message, Collection<Integer> clients) {
        try {
            for (int client : clients) {
                this.connectionManager.send(new Id(client), message);
            }
        } catch (IOException e) {
            LOGGER.error(ServerMarker.CONNECTION, "could not send message", e);
        }
    }

    /**
     * send message to all clients represented by the ids
     *
     * @param message
     * @param clients
     */
    public void sendMessageToIds(Message message, Collection<Id> clients) {
        try {
            for (Id client : clients) {
                this.connectionManager.send(client, message);
            }
        } catch (IOException e) {
            LOGGER.error(ServerMarker.CONNECTION, "could not send message", e);
        }
    }

    /**
     * send message to all clients listed
     *
     * @param message
     * @param clients
     */
    public void sendMessageToClients(Message message, Collection<? extends AbstractClient> clients) {
        try {
            for (AbstractClient client : clients) {
                this.connectionManager.send(new Id(client.getId()), message);
            }
        } catch (IOException e) {
            LOGGER.error(ServerMarker.CONNECTION, "could not send message", e);
        }
    }

    /**
     * send message to all listed clients
     *
     * @param message
     * @param clients
     */
    public <T extends AbstractClient> void sendMessageToClient(Message message, T... clients) {
        this.sendMessageToClients(message, Lists.newArrayList(clients));
    }

    /**
     * send message to all clients represented by the integer id
     *
     * @param message
     * @param clients
     */
    public void sendMessageToInt(Message message, Integer... clients) {
        this.sendMessageToInts(message, Lists.newArrayList(clients));
    }

    /**
     * send message to all clients represented by the ids
     *
     * @param message
     * @param clients
     */
    public void sendMessageToId(Message message, Id... clients) {
        this.sendMessageToIds(message, Lists.newArrayList(clients));
    }

    /**
     * @param id client id
     * @return {@link ClientType} for the client id
     * @throws InvalidActionException
     */
    @Nonnull
    public ClientType getClientTypeFromID(int id) throws InvalidActionException {
        if(clients.containsKey(id))
            return clients.get(id).getClientType();
        throw new InvalidActionException("game.clientManager.clientNotExist");
    }

    /**
     * @return Client for id or {@code null} if client id is not represent
     */
    @Nullable
    public AbstractClient getClient(int id) {
        LOGGER.debug(clients.size());
        return this.clients.get(id);
    }

    @Override
    public void handleBattleshipException(@Nonnull final BattleshipException exception) {
        if (exception.getConnectionId() != null) {
            this.sendMessageToId(NotificationBuilder.errorNotification(exception.getErrorType(), exception.getMessageId(), this.translate(exception.getMessage())), exception.getConnectionId());
        } else {
            LOGGER.warn(ServerMarker.CLIENT, "could not send ErrorNotification. Could not identify source client");
        }
    }

    public ObservableMap<Integer, Client> getPlayerMappings() {
        return player;
    }
}
