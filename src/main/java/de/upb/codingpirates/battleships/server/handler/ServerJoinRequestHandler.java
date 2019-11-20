package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.MessageHandler;
import de.upb.codingpirates.battleships.network.message.request.ServerJoinRequest;
import de.upb.codingpirates.battleships.server.network.ClientManager;

public class ServerJoinRequestHandler implements MessageHandler<ServerJoinRequest> {
    @Inject
    private ClientManager clientManager;

    @Override
    public void handle(ServerJoinRequest message, Id connectionId) throws InvalidActionException {
        if (this.clientManager.create(connectionId, message.getName(), message.getClientType()) == null) {
            throw new InvalidActionException("The ClientType is not valid", connectionId, message.messageId);
        }
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof ServerJoinRequest;
    }
}
