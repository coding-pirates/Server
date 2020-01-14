package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.logic.AbstractClient;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.exceptions.game.NotAllowedException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.PlaceShipsRequest;
import de.upb.codingpirates.battleships.network.message.response.ResponseBuilder;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.util.ServerMarker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public final class PlaceShipsRequestHandler extends AbstractServerMessageHandler<PlaceShipsRequest> {
    private static final Logger LOGGER = LogManager.getLogger();

    @Inject
    public PlaceShipsRequestHandler(@Nonnull final ClientManager clientManager, @Nonnull final GameManager gameManager) {
        super(clientManager, gameManager, PlaceShipsRequest.class);
    }

    @Override
    public void handleMessage(final @Nonnull PlaceShipsRequest message, final @Nonnull Id connectionId) throws GameException {
        LOGGER.debug(ServerMarker.HANDLER, "Handling PlaceShipsRequest from clientId {}.", connectionId.getInt());

        AbstractClient client = clientManager.getClient(connectionId.getInt());
        if(client == null)
            throw new InvalidActionException("player does not exists");
        if (!client.handleClientAs().equals(ClientType.PLAYER))
            throw new NotAllowedException("game.handler.gameJoinPlayerRequest.noPlayer");

        final int clientId = connectionId.getInt();

        gameManager
            .getGameHandlerForClientId(connectionId.getInt())
            .addShipPlacement(clientId, message.getPositions());
        clientManager.sendMessageToId(ResponseBuilder.placeShipsResponse(), connectionId);
    }
}
