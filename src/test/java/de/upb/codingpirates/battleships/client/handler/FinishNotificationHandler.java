package de.upb.codingpirates.battleships.client.handler;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.MessageHandler;
import de.upb.codingpirates.battleships.network.message.notification.FinishNotification;
import de.upb.codingpirates.battleships.server.test.TestLogger;

public class FinishNotificationHandler implements MessageHandler<FinishNotification> , TestLogger {
    @Override
    public void handle(FinishNotification message, Id connectionId) throws GameException {
        LOGGER.info("Winner {}",message.getWinner());
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof FinishNotification;
    }
}
