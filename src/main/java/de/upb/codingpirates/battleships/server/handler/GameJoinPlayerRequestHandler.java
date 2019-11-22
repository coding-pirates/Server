package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.exceptions.game.NotAllowedException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.ExceptionMessageHandler;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.request.GameJoinPlayerRequest;
import de.upb.codingpirates.battleships.network.message.response.GameJoinPlayerResponse;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;

public class GameJoinPlayerRequestHandler extends ExceptionMessageHandler<GameJoinPlayerRequest> {

    @Inject
    private ClientManager clientManager;
    @Inject
    private GameManager gameManager;

    @Override
    public void handleMessage(GameJoinPlayerRequest message, Id connectionId) throws GameException {
        if (!clientManager.getClientTypeFromID(connectionId.getInt()).equals(ClientType.PLAYER)) {
            throw new NotAllowedException("You are not a Player");
        }
        gameManager.addClientToGame(message.getGameId(), clientManager.getClient(connectionId.getInt()), ClientType.PLAYER);
        clientManager.sendMessageToClient(new GameJoinPlayerResponse(message.getGameId()), clientManager.getClient(connectionId.getInt()));
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof GameJoinPlayerRequest;
    }
}
