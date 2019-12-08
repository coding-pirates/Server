package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.network.ConnectionHandler;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.ExceptionMessageHandler;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.request.PlaceShipsRequest;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.game.GameHandler;
import de.upb.codingpirates.battleships.server.util.ServerMarker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

public class PlaceShipsRequestHandler extends ExceptionMessageHandler<PlaceShipsRequest> {
    private static final Logger LOGGER = LogManager.getLogger();
    @Nonnull
    private final ClientManager clientManager;
    @Nonnull
    private final GameManager gameManager;

    @Inject
    public PlaceShipsRequestHandler(@Nonnull ConnectionHandler handler, @Nonnull GameManager gameManager) {
        this.clientManager = (ClientManager) handler;
        this.gameManager = gameManager;
    }

    @Override
    public void handleMessage(PlaceShipsRequest message, Id connectionId) throws GameException {
        LOGGER.debug(ServerMarker.HANDLER, "handle PlaceShipsRequest from {}", connectionId.getInt());
        GameHandler gamehandler = gameManager.getGameHandlerForClientId(connectionId.getInt());
        gamehandler.addShipPlacement(connectionId.getInt(), message.getPositions());
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof PlaceShipsRequest;
    }
}
