package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.network.ConnectionHandler;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.exceptions.game.NotAllowedException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.ExceptionMessageHandler;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.request.SpectatorGameStateRequest;
import de.upb.codingpirates.battleships.network.message.response.SpectatorGameStateResponse;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.game.GameHandler;

public class SpectatorGameStateRequestHandler extends ExceptionMessageHandler<SpectatorGameStateRequest> {
    private ClientManager clientManager;
    private GameManager gameManager;

    @Inject
    public SpectatorGameStateRequestHandler(ConnectionHandler handler, GameManager gameManager) {
        this.clientManager = (ClientManager) handler;
        this.gameManager = gameManager;
    }

    @Override
    public void handleMessage(SpectatorGameStateRequest message, Id connectionId) throws GameException {
        GameHandler handler = gameManager.getGameHandlerForClientId(connectionId.getInt());
        if (!clientManager.getClientTypeFromID(connectionId.getInt()).equals(ClientType.SPECTATOR)) {
            throw new NotAllowedException("game.handler.spectatorGameStateRequest.noSpectator");
        }
        clientManager.sendMessageToId(new SpectatorGameStateResponse(handler.getPlayer(), handler.getShots(), handler.getStartShip(), handler.getGame().getState()), connectionId);
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof SpectatorGameStateRequest;
    }
}
