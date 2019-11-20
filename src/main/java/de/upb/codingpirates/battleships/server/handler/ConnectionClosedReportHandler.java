package de.upb.codingpirates.battleships.server.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.MessageHandler;
import de.upb.codingpirates.battleships.network.message.report.ConnectionClosedReport;
import de.upb.codingpirates.battleships.server.network.ClientManager;

public class ConnectionClosedReportHandler implements MessageHandler<ConnectionClosedReport> {

    @Inject
    private ClientManager clientManager;

    @Override
    public void handle(ConnectionClosedReport message, Id connectionId) {
        this.clientManager.disconnect(connectionId);//TODO test
    }

    @Override
    public boolean canHandle(Message message) {
        return false;
    }
}
