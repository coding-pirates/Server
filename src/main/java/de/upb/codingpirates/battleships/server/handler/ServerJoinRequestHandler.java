package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.ExceptionMessageHandler;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.request.ServerJoinRequest;
import de.upb.codingpirates.battleships.network.message.response.ServerJoinResponse;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;

public class ServerJoinRequestHandler extends ExceptionMessageHandler<ServerJoinRequest> {
    @Inject
    private ClientManager clientManager;
    @Inject
    private GameManager gameManager;

    @Override
    public void handleMessage(ServerJoinRequest message, Id connectionId) throws InvalidActionException {
        if (this.clientManager.create(connectionId.getInt(), message.getName(), message.getClientType()) == null) {
            throw new InvalidActionException("game.handler.serverJoinRequest.noClientType");
        } else {
            this.clientManager.sendMessageToId(new ServerJoinResponse(connectionId.getInt()), connectionId);
        }
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof ServerJoinRequest;
    }
}
