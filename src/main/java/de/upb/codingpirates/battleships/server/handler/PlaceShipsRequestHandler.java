package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.ExceptionMessageHandler;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.request.PlaceShipsRequest;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.game.GameHandler;

public class PlaceShipsRequestHandler extends ExceptionMessageHandler<PlaceShipsRequest> {
    @Inject
    private ClientManager clientManager;
    @Inject
    private GameManager gameManager;

    @Override
    public void handleMessage(PlaceShipsRequest message, Id connectionId) throws GameException {
        GameHandler gamehandler = gameManager.getGameHandlerForClientId(connectionId.getInt());
        if (gamehandler == null) {
            throw new InvalidActionException("game.handler.placeShipsRequest.noGame");
        }
        gamehandler.addShipPlacement(connectionId.getInt(), message.getPositions());
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof PlaceShipsRequest;
    }
}
