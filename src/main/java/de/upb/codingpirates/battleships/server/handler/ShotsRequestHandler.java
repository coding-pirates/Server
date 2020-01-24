package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.logic.AbstractClient;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.exceptions.game.NotAllowedException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;
import de.upb.codingpirates.battleships.network.message.response.ResponseBuilder;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * MessageHandler for {@link ShotsRequest}
 */
public final class ShotsRequestHandler extends AbstractServerMessageHandler<ShotsRequest> {

    @Inject
    public ShotsRequestHandler(@Nonnull final ClientManager clientManager, @Nonnull final GameManager gameManager) {
        super(clientManager, gameManager, ShotsRequest.class);
    }

    @Override
    public void handleMessage(@Nonnull final ShotsRequest message, @Nonnull final Id connectionId) throws GameException {

        AbstractClient client = clientManager.getClient(connectionId.getInt());

        switch (client.handleClientAs()) {
            case PLAYER:
                if (!((Client) client).isDead()) {
                    gameManager
                            .getGameHandlerForClientId(connectionId.getInt())
                            .addShotPlacement(connectionId.getInt(), message.getShots());
                    clientManager.sendMessage(ResponseBuilder.shotsResponse(), connectionId);
                    break;
                }
            case SPECTATOR:
                throw new NotAllowedException("game.handler.gameJoinPlayerRequest.noPlayer");
        }
    }
}
