package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.notification.NotificationBuilder;
import de.upb.codingpirates.battleships.network.message.report.ConnectionClosedReport;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.util.ServerMarker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collection;

/**
 * MessageHandler for {@link ConnectionClosedReport}
 */
public final class ConnectionClosedReportHandler extends AbstractServerMessageHandler<ConnectionClosedReport> {
    private static final Logger LOGGER = LogManager.getLogger();

    @Inject
    public ConnectionClosedReportHandler(@Nonnull final ClientManager clientManager, @Nonnull final GameManager gameManager) {
        super(clientManager, gameManager, ConnectionClosedReport.class);
    }

    @Override
    public void handleMessage(final ConnectionClosedReport message, final Id connectionId) throws InvalidActionException {
        LOGGER.debug(ServerMarker.HANDLER, "Handling ConnectionClosedReport for clientId {}.", connectionId);

        switch (this.clientManager.getClient(connectionId.getInt()).handleClientAs()){
            case PLAYER:
                Collection<Client> player = gameManager.getGameHandlerForClientId(connectionId.getInt()).getPlayers();
                clientManager.sendMessageToClients(NotificationBuilder.leaveNotification(connectionId.getInt()), player);
            case SPECTATOR:
                this.gameManager.removeClientFromGame(connectionId.getInt());
                this.clientManager.disconnect(connectionId.getInt());
        }
    }
}
