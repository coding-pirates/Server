package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.network.ConnectionHandler;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.MessageHandler;
import de.upb.codingpirates.battleships.network.message.notification.LeaveNotification;
import de.upb.codingpirates.battleships.network.message.report.ConnectionClosedReport;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.game.GameHandler;

public class ConnectionClosedReportHandler implements MessageHandler<ConnectionClosedReport> {

    private final ClientManager clientManager;
    private final GameManager gameManager;

    @Inject
    public ConnectionClosedReportHandler(ConnectionHandler handler, GameManager gameManager) {
        this.clientManager = (ClientManager) handler;
        this.gameManager = gameManager;
    }

    @Override
    public void handle(ConnectionClosedReport message, Id connectionId) throws InvalidActionException {
        this.clientManager.disconnect(connectionId.getInt());//TODO test
        gameManager.removeClientFromGame(connectionId.getInt());
        GameHandler handler = gameManager.getGameHandlerForClientId(connectionId.getInt());
        clientManager.sendMessageToClients(new LeaveNotification(connectionId.getInt()), handler.getAllClients());
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof ConnectionClosedReport;
    }
}
