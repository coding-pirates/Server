package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.logic.Game;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.exceptions.game.NotAllowedException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.LobbyRequest;
import de.upb.codingpirates.battleships.network.message.response.ResponseBuilder;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.game.GameHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MessageHandler for {@link LobbyRequest}
 */
public final class LobbyRequestHandler extends AbstractServerMessageHandler<LobbyRequest> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Inject
    public LobbyRequestHandler(@Nonnull final ClientManager clientManager, @Nonnull final GameManager gameManager) {
        super(clientManager, gameManager, LobbyRequest.class);
    }

    @Override
    public void handleMessage(@Nonnull final LobbyRequest message, @Nonnull final Id connectionId) throws NotAllowedException, InvalidActionException {
        LOGGER.debug("Handling LobbyRequest for clientId {}.", connectionId);


        if (!this.clientManager.existsClient(connectionId.getInt()))
            throw new NotAllowedException("game.handler.lobbyRequestHandler.notRegistered");

        final List<Game> games =
                this.gameManager
                        .getGameHandlers()
                        .stream()
                        .map(GameHandler::getGame)
                        .collect(Collectors.toList());

        this.clientManager.sendMessage(ResponseBuilder.lobbyResponse(games), connectionId);
    }
}
