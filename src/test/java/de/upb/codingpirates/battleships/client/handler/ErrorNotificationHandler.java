package de.upb.codingpirates.battleships.client.handler;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.MessageHandler;
import de.upb.codingpirates.battleships.network.message.notification.ErrorNotification;
import de.upb.codingpirates.battleships.server.test.TestLogger;

public class ErrorNotificationHandler implements MessageHandler<ErrorNotification> , TestLogger {

    @Override
    public void handle(ErrorNotification message, Id connectionId) throws GameException {
        LOGGER.debug("message: {}, type: {} error: {}",message.getReferenceMessageId(),message.getErrorType(),message.getReason());
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof ErrorNotification;
    }
}
