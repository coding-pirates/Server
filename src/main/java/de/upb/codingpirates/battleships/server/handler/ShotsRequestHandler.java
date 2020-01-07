package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
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
    public void handleMessage(@Nonnull final ShotsRequest message,
                              @Nonnull final Id connectionId) throws GameException {
        gameManager
            .getGameHandlerForClientId(connectionId.getInt())
            .addShotPlacement(connectionId.getInt(), message.getShots());
        clientManager.sendMessageToId(ResponseBuilder.shotsResponse(), connectionId);
    }
}
