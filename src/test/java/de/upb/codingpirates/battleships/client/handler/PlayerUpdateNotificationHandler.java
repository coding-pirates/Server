package de.upb.codingpirates.battleships.client.handler;

import com.google.inject.Inject;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.MessageHandler;
import de.upb.codingpirates.battleships.network.message.notification.PlayerUpdateNotification;
import de.upb.codingpirates.battleships.server.test.ServerTests;
import de.upb.codingpirates.battleships.server.test.TestLogger;

public class PlayerUpdateNotificationHandler implements MessageHandler<PlayerUpdateNotification>, TestLogger {

    @Inject
    private ServerTests.ClientConnector connector;
    @Override
    public void handle(PlayerUpdateNotification message, Id connectionId) throws GameException {
        LOGGER.info("Handle PlayerUpdateNotification {}",connectionId);
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof PlayerUpdateNotification;
    }
}
