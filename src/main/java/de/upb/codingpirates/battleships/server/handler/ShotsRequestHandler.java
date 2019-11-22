package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.ExceptionMessageHandler;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;

public class ShotsRequestHandler extends ExceptionMessageHandler<ShotsRequest> {
    @Inject
    private ClientManager clientManager;
    @Inject
    private GameManager gameManager;

    @Override
    public void handleMessage(ShotsRequest message, Id connectionId) {

    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof ShotsRequest;
    }
}
