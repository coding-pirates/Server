package de.upb.codingpirates.battleships.server.handler;

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
import java.util.List;

public final class GameLeaveRequestHandler extends AbstractServerMessageHandler<GameLeaveRequest> {

    @Inject
    public GameLeaveRequestHandler(@Nonnull ClientManager clientManager, @Nonnull GameManager gameManager) {
        super(clientManager, gameManager, GameLeaveRequest.class);
    }

    @Override
    protected void handleMessage(GameLeaveRequest message, Id connectionId) throws GameException {
        final List<Client> clients = gameManager
                        .getGameHandlerForClientId(connectionId.getInt())
                        .getAllClients();
        this.clientManager.sendMessageToClients(NotificationBuilder.leaveNotification(connectionId.getInt()), clients);
        this.clientManager.sendMessageToId(ResponseBuilder.gameLeaveResponse(), connectionId);

        this.clientManager.disconnect(connectionId.getInt());
        this.gameManager.removeClientFromGame(connectionId.getInt());
    }
}
