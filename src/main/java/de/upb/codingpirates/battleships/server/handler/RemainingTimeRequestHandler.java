package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.network.ConnectionHandler;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.ExceptionMessageHandler;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.request.RemainingTimeRequest;
import de.upb.codingpirates.battleships.network.message.response.RemainingTimeResponse;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.game.GameHandler;

import javax.annotation.Nonnull;

public class RemainingTimeRequestHandler extends ExceptionMessageHandler<RemainingTimeRequest> {
    @Nonnull
    private ClientManager clientManager;
    @Nonnull
    private GameManager gameManager;

    @Inject
    public RemainingTimeRequestHandler(@Nonnull ConnectionHandler handler, @Nonnull GameManager gameManager) {
        this.clientManager = (ClientManager) handler;
        this.gameManager = gameManager;
    }

    @Override
    public void handleMessage(RemainingTimeRequest message, Id connectionId) throws GameException {
        GameHandler handler = gameManager.getGameHandlerForClientId(connectionId.getInt());
        clientManager.sendMessageToId(new RemainingTimeResponse(handler.getRemainingTime()), connectionId);
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof RemainingTimeRequest;
    }
}
