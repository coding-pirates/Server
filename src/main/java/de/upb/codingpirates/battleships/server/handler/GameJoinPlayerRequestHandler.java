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
import de.upb.codingpirates.battleships.network.message.request.GameJoinPlayerRequest;
import de.upb.codingpirates.battleships.network.message.response.GameJoinPlayerResponse;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.util.ServerMarker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

public class GameJoinPlayerRequestHandler extends ExceptionMessageHandler<GameJoinPlayerRequest> {
    private static final Logger LOGGER = LogManager.getLogger();

    @Nonnull
    private final ClientManager clientManager;
    @Nonnull
    private final GameManager gameManager;

    @Inject
    public GameJoinPlayerRequestHandler(@Nonnull ConnectionHandler handler, @Nonnull GameManager gameManager) {
        this.clientManager = (ClientManager) handler;
        this.gameManager = gameManager;
    }


    @Override
    public void handleMessage(@Nonnull GameJoinPlayerRequest message, Id connectionId) throws GameException {
        LOGGER.debug(ServerMarker.CLIENT, "Handle GameJoinPlayerRequest from {}, for game {}", connectionId, message.getGameId());
        if (!clientManager.getClientTypeFromID(connectionId.getInt()).equals(ClientType.PLAYER)) {
            throw new NotAllowedException("game.handler.gameJoinPlayerRequest.noPlayer");
        } else if (gameManager.getGame(message.getGameId()).getGame().getState().equals(GameState.IN_PROGRESS)) {
            throw new NotAllowedException("game.handler.gameJoinPlayerRequest.gameAlreadyStarted");
        } else if (gameManager.getGame(message.getGameId()).getGame().getState().equals(GameState.FINISHED)) {
            throw new NotAllowedException("game.handler.gameJoinPlayerRequest.gameIsFinished");
        }
        Client client = clientManager.getClient(connectionId.getInt());
        if (client == null) {
            LOGGER.error("Cannot get Client for id {}", connectionId);
            return;
        }
        gameManager.addClientToGame(message.getGameId(), client, ClientType.PLAYER);
        clientManager.sendMessageToClient(new GameJoinPlayerResponse(message.getGameId()), clientManager.getClient(connectionId.getInt()));
        long timer = System.currentTimeMillis();//TODO REMOVE
        //noinspection StatementWithEmptyBody//TODO REMOVE
        while (timer > System.currentTimeMillis() - 1000) {//TODO REMOVE
        }//TODO REMOVE
        if(!gameManager.launchGame(message.getGameId())){//TODO REMOVE
            LOGGER.error("to less player");//TODO REMOVE
        }//TODO REMOVE
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof GameJoinPlayerRequest;
    }
}
