package de.upb.codingpirates.battleships.client.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.network.connectionmanager.ClientConnectionManager;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.MessageHandler;
import de.upb.codingpirates.battleships.network.message.request.LobbyRequest;
import de.upb.codingpirates.battleships.network.message.response.ServerJoinResponse;
import de.upb.codingpirates.battleships.server.test.ServerTests;
import de.upb.codingpirates.battleships.server.test.TestLogger;

import java.io.IOException;

public class ServerJoinResponseHandler implements MessageHandler<ServerJoinResponse> , TestLogger {

    @Inject
    private ClientConnectionManager dispatcher;

    @Override
    public void handle(ServerJoinResponse message, Id connectionId) throws GameException {
        LOGGER.debug("handle ServerJoinResponse with id {}",message.getClientId());
        dispatcher.setConnectionId(message.getClientId());
        try {
            ServerTests.getConnector().sendMessageToServer(new LobbyRequest());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof ServerJoinResponse;
    }
}
