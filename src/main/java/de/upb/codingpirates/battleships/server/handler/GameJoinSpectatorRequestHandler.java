package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.exceptions.game.NotAllowedException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.ExceptionMessageHandler;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.request.GameJoinSpectatorRequest;
import de.upb.codingpirates.battleships.network.message.response.GameJoinPlayerResponse;
import de.upb.codingpirates.battleships.network.message.response.GameJoinSpectatorResponse;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;

public class GameJoinSpectatorRequestHandler extends ExceptionMessageHandler<GameJoinSpectatorRequest> {

    @Inject
    private ClientManager clientManager;
    @Inject
    private GameManager gameManager;

    @Override
    public void handleMessage(GameJoinSpectatorRequest message, Id connectionId) throws GameException {
        if (!clientManager.getClientTypeFromID(connectionId.getInt()).equals(ClientType.SPECTATOR)) {
            throw new NotAllowedException("You are not a Spectator");
        }
        gameManager.addClientToGame(message.getGameId(), clientManager.getClient(connectionId.getInt()), ClientType.SPECTATOR);
        clientManager.sendMessageToClient(new GameJoinSpectatorResponse(message.getGameId()), clientManager.getClient(connectionId.getInt()));
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof GameJoinSpectatorRequest;
    }
}
