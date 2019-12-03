package de.upb.codingpirates.battleships.client.handler;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.MessageHandler;
import de.upb.codingpirates.battleships.network.message.response.GameJoinPlayerResponse;
import de.upb.codingpirates.battleships.server.test.TestLogger;

public class GameJoinPlayerResponseHandler implements MessageHandler<GameJoinPlayerResponse> , TestLogger {
    @Override
    public void handle(GameJoinPlayerResponse message, Id connectionId) throws GameException {
        LOGGER.debug("gamejoined {}",message.getGameId());
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof GameJoinPlayerResponse;
    }
}
