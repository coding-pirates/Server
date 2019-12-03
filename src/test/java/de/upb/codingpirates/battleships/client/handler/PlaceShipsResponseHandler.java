package de.upb.codingpirates.battleships.client.handler;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.MessageHandler;
import de.upb.codingpirates.battleships.network.message.response.PlaceShipsResponse;
import de.upb.codingpirates.battleships.server.test.TestLogger;

public class PlaceShipsResponseHandler implements MessageHandler<PlaceShipsResponse> , TestLogger {
    @Override
    public void handle(PlaceShipsResponse message, Id connectionId) throws GameException {
        LOGGER.info("PlaceShipsResponse");
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof PlaceShipsResponse;
    }
}
