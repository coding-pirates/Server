package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.network.ConnectionHandler;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.ExceptionMessageHandler;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.game.GameHandler;

public class ShotsRequestHandler extends ExceptionMessageHandler<ShotsRequest> {
    private ClientManager clientManager;
    private GameManager gameManager;

    @Inject
    public ShotsRequestHandler(ConnectionHandler handler, GameManager gameManager) {
        this.clientManager = (ClientManager) handler;
        this.gameManager = gameManager;
    }

    @Override
    public void handleMessage(ShotsRequest message, Id connectionId) throws GameException {
        GameHandler gamehandler = gameManager.getGameHandlerForClientId(connectionId.getInt());
        if (gamehandler == null) {
            throw new InvalidActionException("game.handler.shotsRequest.noGame");
        }
        gamehandler.addShotPlacement(connectionId.getInt(), message.getShots());
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof ShotsRequest;
    }
}
