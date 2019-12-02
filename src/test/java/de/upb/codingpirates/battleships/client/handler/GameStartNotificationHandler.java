package de.upb.codingpirates.battleships.client.handler;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.Shot;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.MessageHandler;
import de.upb.codingpirates.battleships.network.message.notification.GameStartNotification;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;
import de.upb.codingpirates.battleships.server.test.ServerTests;
import de.upb.codingpirates.battleships.server.test.TestLogger;

import java.io.IOException;

public class GameStartNotificationHandler implements MessageHandler<GameStartNotification>, TestLogger {
    @Inject
    private ServerTests.ClientConnector clientConnector;
    @Override
    public void handle(GameStartNotification message, Id connectionId) throws GameException {
        try {
            clientConnector.sendMessageToServer(new ShotsRequest(Lists.newArrayList(new Shot(1, new Point2D(3, 4)), new Shot(1, new Point2D(4, 4)), new Shot(0, new Point2D(3, 4)), new Shot(0, new Point2D(4, 4)))));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof GameStartNotification;
    }
}
