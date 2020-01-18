package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.RemainingTimeRequest;
import de.upb.codingpirates.battleships.network.message.response.ResponseBuilder;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public final class RemainingTimeRequestHandler extends AbstractServerMessageHandler<RemainingTimeRequest> {

    @Inject
    public RemainingTimeRequestHandler(final @Nonnull ClientManager clientManager,
                                       final @Nonnull GameManager gameManager) {
        super(clientManager, gameManager, RemainingTimeRequest.class);
    }

    @Override
    public void handleMessage(@Nonnull final RemainingTimeRequest message,
                              @Nonnull final Id connectionId) throws GameException {
        final long remainingTime =
            gameManager
                .getGameHandlerForClientId(connectionId.getInt())
                .getRemainingTime();

        clientManager.sendMessageToId(ResponseBuilder.remainingTimeResponse(remainingTime), connectionId);
    }
}
