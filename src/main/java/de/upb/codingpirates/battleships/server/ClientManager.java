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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private final ServerConnectionManager connectionManager;

    /**
     * maps client id to client
     */
    private final Map<Integer, Client> clients = Maps.newHashMap();
    /**
     * maps client id to player
     */
    private final Map<Integer, Client> player = Maps.newHashMap();
    /**
     * maps client id to spectator
     */
    private final Map<Integer, Client> spectator = Maps.newHashMap();

    @Inject
    public ClientManager(ServerConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Nullable
    public Client create(int id, String name, ClientType clientType) {
        switch (clientType) {
            case PLAYER:
                synchronized (player) {
                    if (player.containsKey(id)) {
                        return null;
                    }
                }
                break;
            case SPECTATOR:
                synchronized (spectator) {
                    if (spectator.containsKey(id)) {
                        return null;
                    }
                }
                break;
            default:
                return null;
        }
        synchronized (player) {
            if (player.containsKey(id)) {
                return null;
            }
        }


        Client client = new Client(id, name);

        switch (clientType) {
            case PLAYER:
                synchronized (player) {
                    player.putIfAbsent(id, client);
                }
                break;
            case SPECTATOR:
                synchronized (spectator) {
                    spectator.putIfAbsent(id, client);
                }
                break;
            default:
                return null;
        }
        synchronized (clients) {
            clients.putIfAbsent(id, client);
        }

        return client;
    }

    public void disconnect(int clientId) {
        synchronized (clients) {
            clients.remove(clientId);
        }
        synchronized (player) {
            player.remove(clientId);
        }
        synchronized (spectator) {
            spectator.remove(clientId);
        }

    }

    public void sendMessageToAll(Message message) {
        try {
            for (int id : clients.keySet()) {
                this.connectionManager.send(new IntId(id), message);
                LOGGER.debug("send message to {}", player.get(id).getName());
            }
        } catch (IOException e) {
            LOGGER.error("could not send message", e);
        }
    }

    public void sendMessageToInts(Message message, Collection<Integer> clients) {
        try {
            for (int client : clients) {
                this.connectionManager.send(new IntId(client), message);
            }
        } catch (IOException e) {
            LOGGER.error("could not send message", e);
        }
    }

    public void sendMessageToIds(Message message, Collection<Id> clients) {
        try {
            for (Id client : clients) {
                this.connectionManager.send(client, message);
            }
        } catch (IOException e) {
            LOGGER.error("could not send message", e);
        }
    }

    public void sendMessageToClients(Message message, Collection<Client> clients) {
        try {
            for (Client client : clients) {
                this.connectionManager.send(new IntId(client.getId()), message);
            }
        } catch (IOException e) {
            LOGGER.error("could not send message", e);
        }
    }

    public void sendMessageToClient(Message message, Client... clients) {
        this.sendMessageToClients(message, Lists.newArrayList(clients));
    }

    public void sendMessageToInt(Message message, Integer... clients) {
        this.sendMessageToInts(message, Lists.newArrayList(clients));
    }

    public void sendMessageToId(Message message, Id... clients) {
        this.sendMessageToIds(message, Lists.newArrayList(clients));
    }

    public ClientType getClientTypeFromID(int id) throws InvalidActionException {
        if (player.containsKey(id))
            return ClientType.PLAYER;
        else if (spectator.containsKey(id))
            return ClientType.SPECTATOR;
        throw new InvalidActionException("game.clientManager.clientNotExist");
    }

    public Client getClient(int id) {
        return this.clients.get(id);
    }

    @Override
    public void handleBattleshipException(BattleshipException e) {
        if (e.getConnectionId() != null) {
            this.sendMessageToId(new ErrorNotification(e.getErrorType(), e.getMessageId(), this.translate(e.getMessage())), e.getConnectionId());
        } else {
            LOGGER.warn("could not send ErrorNotification. Could not identify source client");
        }
    }
}
