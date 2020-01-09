package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.logic.AbstractClient;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.logic.GameState;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.exceptions.game.NotAllowedException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.GameJoinPlayerRequest;
import de.upb.codingpirates.battleships.network.message.response.ResponseBuilder;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.util.ServerMarker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public final class GameJoinPlayerRequestHandler extends AbstractServerMessageHandler<GameJoinPlayerRequest> {
    private static final Logger LOGGER = LogManager.getLogger();

    @Inject
    public GameJoinPlayerRequestHandler(@Nonnull final ClientManager clientManager,
                                        @Nonnull final GameManager   gameManager) {
        super(clientManager, gameManager, GameJoinPlayerRequest.class);
    }

    @Override
    public void handleMessage(@Nonnull final GameJoinPlayerRequest message, @Nonnull final Id connectionId) throws GameException {
        LOGGER.debug(ServerMarker.CLIENT, "Handling GameJoinPlayerRequest from clientId {}, for gameId {}.", connectionId, message.getGameId());

        if (!clientManager.getClientTypeFromID(connectionId.getInt()).equals(ClientType.PLAYER))
            throw new NotAllowedException("game.handler.gameJoinPlayerRequest.noPlayer");

        final GameState gameState =
            gameManager
                .getGameHandler(message.getGameId())
                .getGame()
                .getState();

        switch (gameState) {
        case IN_PROGRESS:
            throw new NotAllowedException("game.handler.gameJoinPlayerRequest.gameAlreadyStarted");
        case FINISHED:
            throw new NotAllowedException("game.handler.gameJoinPlayerRequest.gameIsFinished");
        }

        final AbstractClient client = clientManager.getClient(connectionId.getInt());
        if (client == null) {
            LOGGER.error("Cannot get Client for id {}", connectionId);
            return;
        } else if(client.getClientType() != ClientType.PLAYER){
            LOGGER.error("Spectator {} tried to join as player", connectionId);
            return;
        }

        final int gameId = message.getGameId();

        gameManager.addClientToGame(gameId, client);
        clientManager.sendMessageToClient(ResponseBuilder.gameJoinPlayerResponse(gameId), client);
    }
}
