package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.logic.AbstractClient;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.GameJoinSpectatorRequest;
import de.upb.codingpirates.battleships.network.message.response.ResponseBuilder;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.util.ServerMarker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public final class GameJoinSpectatorRequestHandler extends AbstractServerMessageHandler<GameJoinSpectatorRequest> {
    private static final Logger LOGGER = LogManager.getLogger();

    @Inject
    public GameJoinSpectatorRequestHandler(@Nonnull final ClientManager clientManager, @Nonnull final GameManager gameManager) {
        super(clientManager, gameManager, GameJoinSpectatorRequest.class);
    }

    @Override
    public void handleMessage(@Nonnull final GameJoinSpectatorRequest message, @Nonnull final Id connectionId) throws GameException {
        LOGGER.debug(ServerMarker.CLIENT, "Handling GameJoinSpectatorRequest from clientId {}, for gameId {}.", connectionId, message.getGameId());

        AbstractClient client = clientManager.getClient(connectionId.getInt());
        if (client.getClientType().equals(ClientType.PLAYER)) {
            ((Client) client).setSpectator(true);
        }

        gameManager.addClientToGame(message.getGameId(), client);
        clientManager.sendMessage(ResponseBuilder.gameJoinSpectatorResponse(message.getGameId()), clientManager.getClient(connectionId.getInt()));
    }
}
