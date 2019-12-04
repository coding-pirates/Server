package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.network.ConnectionHandler;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.exceptions.game.NotAllowedException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.ExceptionMessageHandler;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.request.GameJoinSpectatorRequest;
import de.upb.codingpirates.battleships.network.message.response.GameJoinSpectatorResponse;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.util.ServerMarker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

public class GameJoinSpectatorRequestHandler extends ExceptionMessageHandler<GameJoinSpectatorRequest> {
    private static final Logger LOGGER = LogManager.getLogger();

    @Nonnull
    private final ClientManager clientManager;
    @Nonnull
    private final GameManager gameManager;

    @Inject
    public GameJoinSpectatorRequestHandler(@Nonnull ConnectionHandler handler, @Nonnull GameManager gameManager) {
        this.clientManager = (ClientManager) handler;
        this.gameManager = gameManager;
    }

    @Override
    public void handleMessage(GameJoinSpectatorRequest message, Id connectionId) throws GameException {
        LOGGER.debug(ServerMarker.CLIENT, "Handle GameJoinSpactatorRequest from {}, for game {}", connectionId, message.getGameId());

        if (!clientManager.getClientTypeFromID(connectionId.getInt()).equals(ClientType.SPECTATOR)) {
            throw new NotAllowedException("game.handler.gameJoinSpectatorRequest.noSpectator");
        }
        Client client = clientManager.getClient(connectionId.getInt());
        if (client == null) {
            LOGGER.error("Cannot get Client for id {}", connectionId);
            return;
        }
        gameManager.addClientToGame(message.getGameId(), client, ClientType.SPECTATOR);
        clientManager.sendMessageToClient(new GameJoinSpectatorResponse(message.getGameId()), clientManager.getClient(connectionId.getInt()));
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof GameJoinSpectatorRequest;
    }
}
