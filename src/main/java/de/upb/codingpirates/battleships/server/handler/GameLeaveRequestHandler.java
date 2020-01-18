package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.logic.AbstractClient;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.notification.NotificationBuilder;
import de.upb.codingpirates.battleships.network.message.request.GameLeaveRequest;
import de.upb.codingpirates.battleships.network.message.response.ResponseBuilder;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collection;

public final class GameLeaveRequestHandler extends AbstractServerMessageHandler<GameLeaveRequest> {

    @Inject
    public GameLeaveRequestHandler(@Nonnull ClientManager clientManager, @Nonnull GameManager gameManager) {
        super(clientManager, gameManager, GameLeaveRequest.class);
    }

    @Override
    protected void handleMessage(GameLeaveRequest message, Id connectionId) throws GameException {
        AbstractClient client = this.clientManager.getClient(connectionId.getInt());
        switch (client.handleClientAs()){
            case PLAYER:
                Collection<Client> players = this.gameManager.getGameHandlerForClientId(connectionId.getInt()).getPlayers();
                players.removeIf(player -> connectionId.getInt() == player.getId());
                this.clientManager.sendMessageToClients(NotificationBuilder.leaveNotification(connectionId.getInt()), players);
            case SPECTATOR:
                this.gameManager.removeClientFromGame(connectionId.getInt());
                this.clientManager.sendMessageToId(ResponseBuilder.gameLeaveResponse(), connectionId);
        }
    }
}
