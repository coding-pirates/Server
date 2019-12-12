package de.upb.codingpirates.battleships.server;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import de.upb.codingpirates.battleships.server.util.ServerMarker;
import de.upb.codingpirates.battleships.server.util.Translator;

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
    private final Map<Integer, Client> clients = Collections.synchronizedMap(Maps.newHashMap());

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
    private final Map<Integer, Client> spectator = Collections.synchronizedMap(Maps.newHashMap());

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
        LOGGER.debug(ServerMarker.CLIENT, "create client with id: {}, type {}", id, clientType);
        if (this.clients.containsKey(id)) {
            throw new InvalidActionException("game.clientManager.createClient.idExists");
        }

        Client client = new Client(id, name);
        this.clients.putIfAbsent(id, client);
        switch (clientType) {
            case PLAYER:
                this.player.putIfAbsent(id, client);
                break;
            case SPECTATOR:
                this.spectator.putIfAbsent(id, client);
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
                this.connectionManager.send(new IntId(id), message);
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
                this.connectionManager.send(new IntId(client), message);
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
    public void sendMessageToClients(Message message, Collection<Client> clients) {
        try {
            for (Client client : clients) {
                this.connectionManager.send(new IntId(client.getId()), message);
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
        if (this.player.containsKey(id))
            return ClientType.PLAYER;
        else if (this.spectator.containsKey(id))
            return ClientType.SPECTATOR;
        throw new InvalidActionException("game.clientManager.clientNotExist");
    }

    /**
     * @return Client for id or {@code null} if client id is not represent
     */
    @Nullable
    public Client getClient(int id) {
        return this.clients.get(id);
    }

    @Override
    public void handleBattleshipException(@Nonnull final BattleshipException exception) {
        if (exception.getConnectionId() != null) {
            this.sendMessageToId(new ErrorNotification(exception.getErrorType(), exception.getMessageId(), this.translate(exception.getMessage())), exception.getConnectionId());
        } else {
            LOGGER.warn(ServerMarker.CLIENT, "could not send ErrorNotification. Could not identify source client");
        }
    }

    public ObservableMap<Integer, Client> getPlayerMappings() {
        return player;
    }
}
