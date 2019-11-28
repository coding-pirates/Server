package de.upb.codingpirates.battleships.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.network.ConnectionHandler;
import de.upb.codingpirates.battleships.network.connectionmanager.ServerConnectionManager;
import de.upb.codingpirates.battleships.network.exceptions.BattleshipException;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.id.IntId;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.notification.ErrorNotification;
import de.upb.codingpirates.battleships.server.network.ServerApplication;
import de.upb.codingpirates.battleships.server.util.Markers;
import de.upb.codingpirates.battleships.server.util.Translator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * handles all client related tasks
 * <p>
 * get an instance with {@link ServerApplication#getClientManager()}
 */
public class ClientManager implements ConnectionHandler, Translator {
    private static final Logger LOGGER = LogManager.getLogger();

    @Nonnull
    private final ServerConnectionManager connectionManager;

    /**
     * maps client id to client
     * <p>
     * dont use this attribute. Use clients()
     */
    @Nonnull
    private final Map<Integer, Client> clients = Maps.newHashMap();
    /**
     * maps client id to player
     * <p>
     * dont use this attribute. Use player()
     */
    @Nonnull
    private final Map<Integer, Client> player = Maps.newHashMap();
    /**
     * maps client id to spectator
     * <p>
     * dont use this attribute. Use spectator()
     */
    @Nonnull
    private final Map<Integer, Client> spectator = Maps.newHashMap();

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
    public Client create(int id,@Nonnull String name,@Nonnull ClientType clientType) throws InvalidActionException {
        LOGGER.debug(Markers.CLIENT, "create client with id: {}, type {}", id, clientType);
        if (this.clients().containsKey(id)) {
            throw new InvalidActionException("game.clientManager.createClient.idExists");
        }

        Client client = new Client(id, name);

        this.clients().putIfAbsent(id, client);
        switch (clientType) {
            case PLAYER:
                this.player().putIfAbsent(id, client);
                break;
            case SPECTATOR:
                this.spectator().putIfAbsent(id, client);
                break;
        }
        return client;
    }

    /**
     * disconnect client with specific id
     *
     * @param clientId
     */
    public void disconnect(int clientId) {
        this.clients().remove(clientId);
        this.player().remove(clientId);
        this.spectator().remove(clientId);

    }

    /**
     * send message to all connected clients
     *
     * @param message
     */
    public void sendMessageToAll(Message message) {
        try {
            for (int id : this.clients().keySet()) {
                this.connectionManager.send(new IntId(id), message);
            }
        } catch (IOException e) {
            LOGGER.error(Markers.CONNECTION, "could not send message", e);
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
                this.connectionManager.send(new IntId(client), message);
            }
        } catch (IOException e) {
            LOGGER.error(Markers.CONNECTION, "could not send message", e);
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
            LOGGER.error(Markers.CONNECTION, "could not send message", e);
        }
    }

    /**
     * send message to all clients listed
     *
     * @param message
     * @param clients
     */
    public void sendMessageToClients(Message message, Collection<Client> clients) {
        try {
            for (Client client : clients) {
                this.connectionManager.send(new IntId(client.getId()), message);
            }
        } catch (IOException e) {
            LOGGER.error(Markers.CONNECTION, "could not send message", e);
        }
    }

    /**
     * send message to all listed clients
     *
     * @param message
     * @param clients
     */
    public void sendMessageToClient(Message message, Client... clients) {
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
        if (this.player().containsKey(id))
            return ClientType.PLAYER;
        else if (this.spectator().containsKey(id))
            return ClientType.SPECTATOR;
        throw new InvalidActionException("game.clientManager.clientNotExist");
    }

    /**
     * @return Client for id or {@code null} if client id is not represent
     */
    @Nullable
    public Client getClient(int id) {
        return this.clients().get(id);
    }

    @Override
    public void handleBattleshipException(BattleshipException e) {
        if (e.getConnectionId() != null) {
            this.sendMessageToId(new ErrorNotification(e.getErrorType(), e.getMessageId(), this.translate(e.getMessage())), e.getConnectionId());
        } else {
            LOGGER.warn(Markers.CLIENT, "could not send ErrorNotification. Could not identify source client");
        }
    }

    /**
     * @return {@link #clients} as Thread synchronized object
     */
    @Nonnull
    private Map<Integer, Client> clients() {
        synchronized (clients) {
            return clients;
        }
    }

    /**
     * @return {@link #player} as Thread synchronized object
     */
    @Nonnull
    private Map<Integer, Client> player() {
        synchronized (this.player) {
            return this.player;
        }
    }

    /**
     * @return {@link #spectator} as Thread synchronized object
     */
    @Nonnull
    private Map<Integer, Client> spectator() {
        synchronized (this.spectator) {
            return spectator;
        }
    }
}
