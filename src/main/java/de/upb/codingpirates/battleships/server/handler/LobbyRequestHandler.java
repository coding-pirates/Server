package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.logic.Game;
import de.upb.codingpirates.battleships.network.ConnectionHandler;
import de.upb.codingpirates.battleships.network.exceptions.game.NotAllowedException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.ExceptionMessageHandler;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.request.LobbyRequest;
import de.upb.codingpirates.battleships.network.message.response.LobbyResponse;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.game.GameHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class LobbyRequestHandler extends ExceptionMessageHandler<LobbyRequest> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final ClientManager clientManager;
    private final GameManager gameManager;

    @Inject
    public LobbyRequestHandler(ConnectionHandler handler, GameManager gameManager) {
        this.clientManager = (ClientManager) handler;
        this.gameManager = gameManager;
    }

    @Override
    public void handleMessage(LobbyRequest message, Id connectionId) throws NotAllowedException {
        if (clientManager.getClient(connectionId.getInt()) != null) {
            List<Game> games = gameManager.getAllGames().stream().map((GameHandler::getGame)).collect(Collectors.toList());
            clientManager.sendMessageToId(new LobbyResponse(games), connectionId);
        } else {
            throw new NotAllowedException("game.handler.lobbyRequestHandler.notRegistered");
        }
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof LobbyRequest;
    }
}
