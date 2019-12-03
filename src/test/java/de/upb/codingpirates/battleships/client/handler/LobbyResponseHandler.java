package de.upb.codingpirates.battleships.client.handler;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.MessageHandler;
import de.upb.codingpirates.battleships.network.message.response.LobbyResponse;
import de.upb.codingpirates.battleships.server.test.ServerTests;
import de.upb.codingpirates.battleships.server.test.TestLogger;

public class LobbyResponseHandler implements MessageHandler<LobbyResponse> , TestLogger {

    @Override
    public void handle(LobbyResponse message, Id connectionId) throws GameException {
        LOGGER.debug("handle LobbyResponse with games {}",message.getGames().size());
        ServerTests.setLobbySize(message.getGames().size());
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof LobbyResponse;
    }
}
