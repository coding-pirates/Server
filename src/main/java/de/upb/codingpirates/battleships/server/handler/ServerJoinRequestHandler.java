package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.network.ConnectionHandler;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.ExceptionMessageHandler;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.request.ServerJoinRequest;
import de.upb.codingpirates.battleships.network.message.response.ServerJoinResponse;
import de.upb.codingpirates.battleships.server.ClientManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

public class ServerJoinRequestHandler extends ExceptionMessageHandler<ServerJoinRequest> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nonnull
    private ClientManager clientManager;

    @Inject
    public ServerJoinRequestHandler(@Nonnull ConnectionHandler handler) {
        this.clientManager = (ClientManager) handler;
    }

    @Override
    public void handleMessage(ServerJoinRequest message, Id connectionId) throws GameException {
        LOGGER.debug("Handle ServerJoinRequest from {}, with name {}, as {}", connectionId, message.getName(), message.getClientType());
        this.clientManager.create(connectionId.getInt(), message.getName(), message.getClientType());
        this.clientManager.sendMessageToId(new ServerJoinResponse(connectionId.getInt()), connectionId);
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof ServerJoinRequest;
    }
}
