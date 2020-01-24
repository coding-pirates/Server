package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.logic.AbstractClient;
import de.upb.codingpirates.battleships.logic.Client;
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

/**
 * MessageHandler for {@link PlayerGameStateRequest}
 */
public final class PlayerGameStateRequestHandler extends AbstractServerMessageHandler<PlayerGameStateRequest> {

    @Inject
    public PlayerGameStateRequestHandler(@Nonnull final ClientManager clientManager,
                                         @Nonnull final GameManager gameManager) {
        super(clientManager, gameManager, PlayerGameStateRequest.class);
    }

    @Override
    public void handleMessage(@Nonnull final PlayerGameStateRequest message, @Nonnull final Id connectionId) throws GameException {

        AbstractClient client = clientManager.getClient(connectionId.getInt());

        switch (client.handleClientAs()){
            case PLAYER:
                if(!((Client)client).isDead()){
                    final GameHandler handler = gameManager.getGameHandlerForClientId(connectionId.getInt());

                    clientManager.sendMessage(
                            ResponseBuilder.playerGameStateResponse()
                                    .gameState(handler.getState())
                                    .hits(handler.getHitShots())
                                    .sunk(handler.getSunkShots())
                                    .ships(handler.getStartShip().get(connectionId.getInt()))
                                    .players(handler.getPlayers())
                                    .build(),
                            connectionId);
                    break;
                }
            case SPECTATOR:
                throw new NotAllowedException("game.handler.gameJoinPlayerRequest.noPlayer");
        }
    }
}
