package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.logic.GameState;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.exceptions.game.NotAllowedException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.SpectatorGameStateRequest;
import de.upb.codingpirates.battleships.network.message.response.ResponseBuilder;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.game.GameHandler;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public final class SpectatorGameStateRequestHandler extends AbstractServerMessageHandler<SpectatorGameStateRequest> {

    @Inject
    public SpectatorGameStateRequestHandler(@Nonnull final ClientManager clientManager,
                                            @Nonnull final GameManager gameManager) {
        super(clientManager, gameManager, SpectatorGameStateRequest.class);
    }

    @Override
    public void handleMessage(@Nonnull final SpectatorGameStateRequest message,
                              @Nonnull final Id connectionId) throws GameException {
        if (!clientManager.getClientTypeFromID(connectionId.getInt()).equals(ClientType.SPECTATOR))
            throw new NotAllowedException("game.handler.spectatorGameStateRequest.noSpectator");

        final GameHandler handler = gameManager.getGameHandlerForClientId(connectionId.getInt());
        if (handler.getGame().getState() != GameState.IN_PROGRESS)
            throw new NotAllowedException("game.handler.spectatorGameStateRequest.wrongTime");

        clientManager.sendMessageToId(
            ResponseBuilder.spectatorGameStateResponse()
                .players(handler.getPlayers())
                .shots(handler.getShots())
                .ships(handler.getStartShip())
                .gameState(handler.getGame().getState())
                .build(),
            connectionId);
    }
}
