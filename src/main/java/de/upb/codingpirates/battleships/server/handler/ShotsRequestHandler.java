package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.network.ConnectionHandler;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.ExceptionMessageHandler;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.game.GameHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

public class ShotsRequestHandler extends ExceptionMessageHandler<ShotsRequest> {
    private static final Logger LOGGER = LogManager.getLogger();
    @Nonnull
    private ClientManager clientManager;
    @Nonnull
    private GameManager gameManager;

    @Inject
    public ShotsRequestHandler(@Nonnull ConnectionHandler handler, @Nonnull GameManager gameManager) {
        this.clientManager = (ClientManager) handler;
        this.gameManager = gameManager;
    }

    @Override
    public void handleMessage(ShotsRequest message, Id connectionId) throws GameException {
        GameHandler gamehandler = gameManager.getGameHandlerForClientId(connectionId.getInt());
        gamehandler.addShotPlacement(connectionId.getInt(), message.getShots());
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof ShotsRequest;
    }
}
