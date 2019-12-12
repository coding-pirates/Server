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

import static java.util.stream.Collectors.toMap;

public final class PointsRequestHandler extends AbstractServerMessageHandler<PointsRequest> {

    @Inject
    public PointsRequestHandler(@Nonnull final ClientManager clientManager,
                                @Nonnull final GameManager gameManager) {
        super(clientManager, gameManager, PointsRequest.class);
    }

    @Override
    public void handleMessage(@Nonnull final PointsRequest message,
                              @Nonnull final Id connectionId) throws GameException {
        final Map<Integer, Integer> scores =
            gameManager
                .getGameHandlerForClientId(connectionId.getInt())
                .getScore();

        final Map<Integer, Integer> transmittedScores =
            scores
                .entrySet()
                .stream()
                .collect(toMap(Entry::getKey, entry -> entry.getValue() / 4));

        clientManager.sendMessageToId(new PointsResponse(transmittedScores), connectionId);
    }
}
