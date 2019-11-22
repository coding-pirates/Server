package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.ExceptionMessageHandler;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.request.PlayerGameStateRequest;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;

public class PlayerGameStateRequestHandler extends ExceptionMessageHandler<PlayerGameStateRequest> {
    @Inject
    private ClientManager clientManager;
    @Inject
    private GameManager gameManager;

    @Override
    public void handleMessage(PlayerGameStateRequest message, Id connectionId) {

    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof PlayerGameStateRequest;
    }
}
