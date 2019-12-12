package de.upb.codingpirates.battleships.server.handler;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.ServerJoinRequest;
import de.upb.codingpirates.battleships.network.message.response.ServerJoinResponse;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;

public final class ServerJoinRequestHandler extends AbstractServerMessageHandler<ServerJoinRequest> {
    private static final Logger LOGGER = LogManager.getLogger();

    @Inject
    public ServerJoinRequestHandler(@Nonnull final ClientManager clientManager,
                                    @Nonnull final GameManager gameManager) {
        super(clientManager, gameManager, ServerJoinRequest.class);
    }

    @Override
    public void handleMessage(@Nonnull final ServerJoinRequest message,
                              @Nonnull final Id connectionId) throws GameException {
        LOGGER.debug("Handling ServerJoinRequest from clientId {}, with name '{}', as type {}.", connectionId, message.getName(), message.getClientType());

        clientManager.create(connectionId.getInt(), message.getName(), message.getClientType());
        clientManager.sendMessageToId(new ServerJoinResponse(connectionId.getInt()), connectionId);
    }
}
