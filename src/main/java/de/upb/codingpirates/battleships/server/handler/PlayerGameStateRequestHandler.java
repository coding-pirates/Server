package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.network.ConnectionHandler;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.ExceptionMessageHandler;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.request.PlayerGameStateRequest;
import de.upb.codingpirates.battleships.network.message.response.PlayerGameStateResponse;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.game.GameHandler;

public class PlayerGameStateRequestHandler extends ExceptionMessageHandler<PlayerGameStateRequest> {
    private final ClientManager clientManager;
    private final GameManager gameManager;

    @Inject
    public PlayerGameStateRequestHandler(ConnectionHandler handler, GameManager gameManager) {
        this.clientManager = (ClientManager) handler;
        this.gameManager = gameManager;
    }

    @Override
    public void handleMessage(PlayerGameStateRequest message, Id connectionId) throws GameException {
        GameHandler handler = gameManager.getGameHandlerForClientId(connectionId.getInt());
        this.clientManager.sendMessageToId(new PlayerGameStateResponse(handler.getGame().getState(),handler.getHitShots(),handler.getSunkShots(),handler.getStartShip().get(connectionId.getInt()),handler.getPlayer()), connectionId);
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof PlayerGameStateRequest;
    }
}
