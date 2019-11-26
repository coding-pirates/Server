package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.ExceptionMessageHandler;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.request.PointsRequest;
import de.upb.codingpirates.battleships.network.message.response.PointsResponse;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;

import java.util.Map;

public class PointsRequestHandler extends ExceptionMessageHandler<PointsRequest> {
    @Inject
    private ClientManager clientManager;
    @Inject
    private GameManager gameManager;

    @Override
    public void handleMessage(PointsRequest message, Id connectionId) throws GameException {
        Map<Integer, Integer> score = gameManager.getGameHandlerForClientId(connectionId.getInt()).getScore();
        clientManager.sendMessageToId(new PointsResponse(score), connectionId);
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof PointsRequest;
    }
}
