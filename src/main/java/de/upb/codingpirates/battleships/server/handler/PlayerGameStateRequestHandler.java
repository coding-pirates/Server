package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.exceptions.game.NotAllowedException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.PlayerGameStateRequest;
import de.upb.codingpirates.battleships.network.message.response.ResponseBuilder;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.game.GameHandler;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public final class PlayerGameStateRequestHandler extends AbstractServerMessageHandler<PlayerGameStateRequest> {

    @Inject
    public PlayerGameStateRequestHandler(@Nonnull final ClientManager clientManager,
                                         @Nonnull final GameManager gameManager) {
        super(clientManager, gameManager, PlayerGameStateRequest.class);
    }

    @Override
    public void handleMessage(@Nonnull final PlayerGameStateRequest message, @Nonnull final Id connectionId) throws GameException {

        if (!clientManager.getClientTypeFromID(connectionId.getInt()).equals(ClientType.PLAYER))
            throw new NotAllowedException("game.handler.gameJoinPlayerRequest.noPlayer");

        final GameHandler handler = gameManager.getGameHandlerForClientId(connectionId.getInt());

        clientManager.sendMessageToId(
            ResponseBuilder.playerGameStateResponse()
                .gameState(handler.getGame().getState())
                .hits(handler.getHitShots())
                .sunk(handler.getSunkShots())
                .ships(handler.getStartShip().get(connectionId.getInt()))
                .players(handler.getPlayers())
                .build(),
            connectionId);
    }
}
