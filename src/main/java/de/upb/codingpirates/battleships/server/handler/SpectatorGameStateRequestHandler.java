package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.MessageHandler;
import de.upb.codingpirates.battleships.network.message.request.SpectatorGameStateRequest;
import de.upb.codingpirates.battleships.server.network.ClientManager;

public class SpectatorGameStateRequestHandler implements MessageHandler<SpectatorGameStateRequest> {
    @Inject
    private ClientManager clientManager;

    @Override
    public void handle(SpectatorGameStateRequest message, Id connectionId) {

    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof SpectatorGameStateRequest;
    }
}
