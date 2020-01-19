package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.logic.AbstractClient;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.exceptions.game.NotAllowedException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;
import de.upb.codingpirates.battleships.network.message.response.ResponseBuilder;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public final class ShotsRequestHandler extends AbstractServerMessageHandler<ShotsRequest> {

    @Inject
    public ShotsRequestHandler(@Nonnull final ClientManager clientManager,
                               @Nonnull final GameManager gameManager) {
        super(clientManager, gameManager, ShotsRequest.class);
    }

    @Override
    public void handleMessage(@Nonnull final ShotsRequest message, @Nonnull final Id connectionId) throws GameException {

        AbstractClient client = clientManager.getClient(connectionId.getInt());
        if (client == null)
            throw new InvalidActionException("player does not exists");
        if (!client.handleClientAs().equals(ClientType.PLAYER))
            throw new NotAllowedException("game.handler.gameJoinPlayerRequest.noPlayer");

        gameManager
                .getGameHandlerForClientId(connectionId.getInt())
                .addShotPlacement(connectionId.getInt(), message.getShots());
        clientManager.sendMessageToId(ResponseBuilder.shotsResponse(), connectionId);
    }
}
