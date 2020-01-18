package de.upb.codingpirates.battleships.server.handler;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.PlaceShipsRequest;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.util.ServerMarker;

public final class PlaceShipsRequestHandler extends AbstractServerMessageHandler<PlaceShipsRequest> {
    private static final Logger LOGGER = LogManager.getLogger();

    @Inject
    public PlaceShipsRequestHandler(@Nonnull final ClientManager clientManager,
                                    @Nonnull final GameManager gameManager) {
        super(clientManager, gameManager, PlaceShipsRequest.class);
    }

    @Override
    public void handleMessage(final @Nonnull PlaceShipsRequest message,
                              final @Nonnull Id connectionId) throws GameException {
        LOGGER.debug(ServerMarker.HANDLER, "Handling PlaceShipsRequest from clientId {}.", connectionId.getInt());

        final int clientId = connectionId.getInt();

        gameManager
            .getGameHandlerForClientId(connectionId.getInt())
            .addShipPlacement(clientId, message.getPositions());
    }
}
