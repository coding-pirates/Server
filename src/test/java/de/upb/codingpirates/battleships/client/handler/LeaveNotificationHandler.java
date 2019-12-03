package de.upb.codingpirates.battleships.client.handler;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.MessageHandler;
import de.upb.codingpirates.battleships.network.message.notification.LeaveNotification;
import de.upb.codingpirates.battleships.server.test.TestLogger;

public class LeaveNotificationHandler implements MessageHandler<LeaveNotification>, TestLogger {
    @Override
    public void handle(LeaveNotification message, Id connectionId) throws GameException {
        LOGGER.info("left: {}",message.getPlayerId());
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof LeaveNotification;
    }
}
