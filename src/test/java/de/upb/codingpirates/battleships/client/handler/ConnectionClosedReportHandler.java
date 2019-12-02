package de.upb.codingpirates.battleships.client.handler;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.MessageHandler;
import de.upb.codingpirates.battleships.network.message.report.ConnectionClosedReport;
import de.upb.codingpirates.battleships.server.test.TestLogger;

public class ConnectionClosedReportHandler implements MessageHandler<ConnectionClosedReport> , TestLogger {
    @Override
    public void handle(ConnectionClosedReport message, Id connectionId) throws GameException {
        LOGGER.info("Connection Closed");
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof ConnectionClosedReport;
    }
}
