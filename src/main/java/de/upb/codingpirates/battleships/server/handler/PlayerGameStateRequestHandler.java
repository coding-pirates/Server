package de.upb.codingpirates.battleships.server.handler;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.PlayerGameStateRequest;
import de.upb.codingpirates.battleships.network.message.response.PlayerGameStateResponse;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.game.GameHandler;

public final class PlayerGameStateRequestHandler extends AbstractServerMessageHandler<PlayerGameStateRequest> {

    @Inject
    public PlayerGameStateRequestHandler(@Nonnull final ClientManager clientManager,
                                         @Nonnull final GameManager gameManager) {
        super(clientManager, gameManager, PlayerGameStateRequest.class);
    }

    @Override
    public void handleMessage(@Nonnull final PlayerGameStateRequest message,
                              @Nonnull final Id connectionId) throws GameException {
        final GameHandler handler = gameManager.getGameHandlerForClientId(connectionId.getInt());

        clientManager.sendMessageToId(
            new PlayerGameStateResponse.Builder()
                .gameState(handler.getGame().getState())
                .hits(handler.getHitShots())
                .sunk(handler.getSunkShots())
                .ships(handler.getStartShip().get(connectionId.getInt()))
                .players(handler.getPlayers())
                .build(),
            connectionId);
    }
}
