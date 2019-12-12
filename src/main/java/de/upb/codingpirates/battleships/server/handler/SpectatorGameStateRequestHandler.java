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

import javax.annotation.Nonnull;

public class SpectatorGameStateRequestHandler extends ExceptionMessageHandler<SpectatorGameStateRequest> {
    @Nonnull
    private ClientManager clientManager;
    @Nonnull
    private GameManager gameManager;

    @Inject
    public SpectatorGameStateRequestHandler(@Nonnull ConnectionHandler handler, @Nonnull GameManager gameManager) {
        this.clientManager = (ClientManager) handler;
        this.gameManager = gameManager;
    }

    @Override
    public void handleMessage(SpectatorGameStateRequest message, Id connectionId) throws GameException {
        if (!clientManager.getClientTypeFromID(connectionId.getInt()).equals(ClientType.SPECTATOR)) {
            throw new NotAllowedException("game.handler.spectatorGameStateRequest.noSpectator");
        }
        GameHandler handler = gameManager.getGameHandlerForClientId(connectionId.getInt());
        if(handler.getGame().getState() != GameState.IN_PROGRESS){
            throw new NotAllowedException("game.handler.spectatorGameStateRequest.wrongTime");
        }

        clientManager.sendMessageToId(new SpectatorGameStateResponse(handler.getPlayers(), handler.getShots(), handler.getStartShip(), handler.getGame().getState()), connectionId);
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof SpectatorGameStateRequest;
    }
}
