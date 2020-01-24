package de.upb.codingpirates.battleships.server.handler;

import com.google.common.collect.ImmutableMap;
import de.upb.codingpirates.battleships.logic.AbstractClient;
import de.upb.codingpirates.battleships.logic.GameStage;
import de.upb.codingpirates.battleships.logic.GameState;
import de.upb.codingpirates.battleships.logic.PlacementInfo;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.exceptions.game.NotAllowedException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.notification.NotificationBuilder;
import de.upb.codingpirates.battleships.network.message.request.SpectatorGameStateRequest;
import de.upb.codingpirates.battleships.network.message.response.ResponseBuilder;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.game.GameHandler;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Map;

/**
 * MessageHandler for {@link SpectatorGameStateRequest}
 */
public final class SpectatorGameStateRequestHandler extends AbstractServerMessageHandler<SpectatorGameStateRequest> {

    private static final Map<Integer, Map<Integer, PlacementInfo>> EMPTY = ImmutableMap.of();

    @Inject
    public SpectatorGameStateRequestHandler(@Nonnull final ClientManager clientManager, @Nonnull final GameManager gameManager) {
        super(clientManager, gameManager, SpectatorGameStateRequest.class);
    }

    @Override
    public void handleMessage(@Nonnull final SpectatorGameStateRequest message, @Nonnull final Id connectionId) throws GameException {
        AbstractClient client = clientManager.getClient(connectionId.getInt());

        switch (client.handleClientAs()) {
            case PLAYER:
                throw new NotAllowedException("game.handler.spectatorGameStateRequest.noSpectator");
            case SPECTATOR:
                final GameHandler handler = gameManager.getGameHandlerForClientId(connectionId.getInt());

                clientManager.sendMessage(
                        ResponseBuilder.spectatorGameStateResponse()
                                .players(handler.getAllPlayer())
                                .shots(handler.getShots())
                                .ships(handler.getStage().equals(GameStage.PLACESHIPS) || handler.getStage().equals(GameStage.START) ? EMPTY : handler.getStartShip())
                                .gameState(handler.getState())
                                .build(),
                        connectionId);
                if (handler.getState().equals(GameState.FINISHED)) {
                    clientManager.sendMessage(NotificationBuilder.finishNotification(handler.getScore(), handler.getWinner()), connectionId);
                }
        }
    }
}
