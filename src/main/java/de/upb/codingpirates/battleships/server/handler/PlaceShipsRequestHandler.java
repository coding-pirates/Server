package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.MessageHandler;
import de.upb.codingpirates.battleships.network.message.request.PlaceShipsRequest;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.game.GameHandler;

public class PlaceShipsRequestHandler implements MessageHandler<PlaceShipsRequest> {
    @Inject
    private ClientManager clientManager;
    @Inject
    private GameManager gameManager;

    @Override
    public void handle(PlaceShipsRequest message, Id connectionId) throws InvalidActionException {
        GameHandler gamehandler = gameManager.getGameManagerForClientId(connectionId);
        if (gamehandler == null) {
            throw new InvalidActionException("You are not part of a game", connectionId, message.messageId);
        }
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof PlaceShipsRequest;
    }
}
