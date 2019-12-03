package de.upb.codingpirates.battleships.client.handler;


import com.google.inject.Inject;
import de.upb.codingpirates.battleships.logic.PlacementInfo;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.Rotation;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.MessageHandler;
import de.upb.codingpirates.battleships.network.message.notification.GameInitNotification;
import de.upb.codingpirates.battleships.network.message.request.PlaceShipsRequest;
import de.upb.codingpirates.battleships.server.test.ServerTests;
import de.upb.codingpirates.battleships.server.test.TestLogger;

import java.io.IOException;
import java.util.HashMap;

public class GameInitNotificationHandler implements MessageHandler<GameInitNotification> , TestLogger {
    @Inject
    private ServerTests.ClientConnector connector;
    @Override
    public void handle(GameInitNotification message, Id connectionId) throws GameException {
        LOGGER.info("GameInit");

        ServerTests.clients = message.getClientList();
        try {
            LOGGER.info("Send PlaceShipsRequest");
            connector.sendMessageToServer(new PlaceShipsRequest(new HashMap<Integer, PlacementInfo>(){{
                put(0,new PlacementInfo(new Point2D(2,2), Rotation.NONE));
            }}));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof GameInitNotification;
    }
}
