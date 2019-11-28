package de.upb.codingpirates.battleships.client.handler;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.MessageHandler;
import de.upb.codingpirates.battleships.network.message.response.ShotsResponse;
import de.upb.codingpirates.battleships.server.test.TestLogger;

public class ShotsResponseHandler implements MessageHandler<ShotsResponse> , TestLogger {
    @Override
    public void handle(ShotsResponse message, Id connectionId) throws GameException {
        LOGGER.info("ShotsResponse");
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof ShotsResponse;
    }
}
