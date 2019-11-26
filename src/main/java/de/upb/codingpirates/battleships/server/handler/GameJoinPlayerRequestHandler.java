package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.logic.GameState;
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
            throw new NotAllowedException("game.handler.gameJoinPlayerRequest.noPlayer");
        }
        else if (gameManager.getGame(message.getGameId()).getGame().getState().equals(GameState.IN_PROGRESS)) {
            throw new NotAllowedException("game.handler.gameJoinPlayerRequest.gameAlreadyStarted");
        }
        else if (gameManager.getGame(message.getGameId()).getGame().getState().equals(GameState.FINISHED)) {
            throw new NotAllowedException("game.handler.gameJoinPlayerRequest.gameIsFinished");
        }
        gameManager.addClientToGame(message.getGameId(), clientManager.getClient(connectionId.getInt()), ClientType.PLAYER);
        clientManager.sendMessageToClient(new GameJoinPlayerResponse(message.getGameId()), clientManager.getClient(connectionId.getInt()));
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof GameJoinPlayerRequest;
    }
}
