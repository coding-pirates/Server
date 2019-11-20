package de.upb.codingpirates.battleships.server.network;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import de.upb.codingpirates.battleships.logic.util.Client;
import de.upb.codingpirates.battleships.logic.util.ClientType;
import de.upb.codingpirates.battleships.network.ConnectionHandler;
import de.upb.codingpirates.battleships.network.connectionmanager.ServerConnectionManager;
import de.upb.codingpirates.battleships.network.exceptions.BattleshipException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.id.IntId;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.notification.ErrorNotification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class ClientManager implements ConnectionHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    @Inject
    private ServerConnectionManager connectionManager;

    private final Map<Id, Client> clients = Maps.newHashMap();
    private final Map<Id, Client> spectator = Maps.newHashMap();

    @Nullable
    public Client create(Id id, String name, ClientType clientType) {
        switch (clientType) {
            case PLAYER:
                synchronized (clients) {
                    if (clients.containsKey(id)) {
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


        Client client = new Client((Integer) id.getRaw(), name);

        switch (clientType) {
            case PLAYER:
                synchronized (clients) {
                    clients.putIfAbsent(id, client);
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

        return client;
    }

    public void disconnect(Id clientId) {
        synchronized (clients) {
            clients.remove(clientId);
        }
        synchronized (clients) {
            spectator.remove(clientId);
        }

    }

    public void sendMessageToAll(Message message) {
        try {
            for (Id id : clients.keySet()) {
                this.connectionManager.send(id, message);
                LOGGER.debug("send message to {}", clients.get(id).getName());
            }
        } catch (IOException e) {
            LOGGER.error("could not send message", e);
        }
    }

    private void sendMessageTo(Message message, Collection<Id> clients) {
        try {
            for (Id client : clients) {
                this.connectionManager.send(client, message);
            }
        } catch (IOException e) {
            LOGGER.error("could not send message", e);
        }
    }

    public void sendMessageTo(Message message, Client... clients) {
        this.sendMessageTo(message, Arrays.stream(clients).map(client -> new IntId(client.getId())).collect(Collectors.toList()));
    }

    public void sendMessageTo(Message message, Id... clients) {
        this.sendMessageTo(message, Lists.newArrayList(clients));
    }

    @Override
    public void handleBattleshipException(BattleshipException e) {
        if (e.getConnectionId() != null) {
            this.sendMessageTo(new ErrorNotification(e.getErrorType(), e.getMessageId(), e.getMessage()), e.getConnectionId());
        } else {
            LOGGER.warn("could not send ErrorNotification. Could not identify source client");
        }
    }
}
