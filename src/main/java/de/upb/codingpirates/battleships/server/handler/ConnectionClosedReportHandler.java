package de.upb.codingpirates.battleships.server.handler;

import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.notification.LeaveNotification;
import de.upb.codingpirates.battleships.network.message.report.ConnectionClosedReport;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.util.ServerMarker;

public final class ConnectionClosedReportHandler extends AbstractServerMessageHandler<ConnectionClosedReport> {
    private static final Logger LOGGER = LogManager.getLogger();

    @Inject
    public ConnectionClosedReportHandler(@Nonnull final ClientManager clientManager,
                                         @Nonnull final GameManager   gameManager) {
        super(clientManager, gameManager, ConnectionClosedReport.class);
    }

    @Override
    public void handleMessage(final ConnectionClosedReport message, final Id connectionId)
            throws InvalidActionException {
        LOGGER.debug(ServerMarker.HANDLER, "Handling ConnectionClosedReport for clientId {}.", connectionId);

        clientManager.disconnect(connectionId.getInt());
        gameManager.removeClientFromGame(connectionId.getInt());

        final List<Client> clients =
            gameManager
                .getGameHandlerForClientId(connectionId.getInt())
                .getAllClients();

        clientManager.sendMessageToClients(new LeaveNotification(connectionId.getInt()), clients);
    }
}
