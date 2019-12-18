package de.upb.codingpirates.battleships.server.handler;

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.PointsRequest;
import de.upb.codingpirates.battleships.network.message.response.PointsResponse;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;

public final class PointsRequestHandler extends AbstractServerMessageHandler<PointsRequest> {

    @Inject
    public PointsRequestHandler(@Nonnull final ClientManager clientManager,
                                @Nonnull final GameManager gameManager) {
        super(clientManager, gameManager, PointsRequest.class);
    }

    @Override
    public void handleMessage(PointsRequest message, Id connectionId) throws GameException {
        final Map<Integer, Integer> scores =
            gameManager
                .getGameHandlerForClientId(connectionId.getInt())
                .getScore();
        clientManager.sendMessageToId(new PointsResponse(scores), connectionId);
    }
}
