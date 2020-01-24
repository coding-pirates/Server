package de.upb.codingpirates.battleships.server.test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.client.ListenerHandler;
import de.upb.codingpirates.battleships.client.listener.*;
import de.upb.codingpirates.battleships.client.network.ClientApplication;
import de.upb.codingpirates.battleships.client.network.ClientConnector;
import de.upb.codingpirates.battleships.client.network.ClientModule;
import de.upb.codingpirates.battleships.logic.*;
import de.upb.codingpirates.battleships.network.Properties;
import de.upb.codingpirates.battleships.network.exceptions.BattleshipException;
import de.upb.codingpirates.battleships.network.message.notification.*;
import de.upb.codingpirates.battleships.network.message.request.RequestBuilder;
import de.upb.codingpirates.battleships.network.message.response.GameJoinPlayerResponse;
import de.upb.codingpirates.battleships.network.message.response.LobbyResponse;
import de.upb.codingpirates.battleships.network.message.response.ServerJoinResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameTest extends ServerTest{

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();

    private static List<ClientConnector> connectorstmp = Collections.synchronizedList(Lists.newArrayList());
    private static Map<Integer, ClientConnector> connectors = Collections.synchronizedMap(Maps.newHashMap());
    private static String[] names = new String[]{"paul", "fynn", "raphael", "caro","lukas","andre","benjamin", "leonie","roman","simon"};
    private static List<Integer> ids = Collections.synchronizedList(Lists.newArrayList());
    private static Configuration configuration;


    private static int lobbySize = 0;
    private static final AtomicBoolean finished = new AtomicBoolean(true);

    @Test
    public void gameTest() {
        serverStart();

        long timer = System.currentTimeMillis();
        while (timer > System.currentTimeMillis() - 1000){
        }
        LOGGER.debug("start connection test");

        for(int i = 0;i< TestProperties.playerCount;i++) {
            ClientConnector connector = ClientApplication.create(new TestClientModule());
            connector.connect(TestProperties.hostAddress, Properties.PORT);
            connectorstmp.add(connector);
            timer = System.currentTimeMillis();
            while (timer > System.currentTimeMillis() - 100){
            }
        }

        for(int i = 0;i< TestProperties.playerCount;i++) {
            connectorstmp.get(i).sendMessageToServer(RequestBuilder.serverJoinRequest(names[i], ClientType.PLAYER));
            timer = System.currentTimeMillis();
            while (timer > System.currentTimeMillis() - 10){
            }
        }

        for(int i = 0;i< TestProperties.playerCount;i++) {
            connectorstmp.get(i).sendMessageToServer(RequestBuilder.lobbyRequest());
            timer = System.currentTimeMillis();
            while (timer > System.currentTimeMillis() - 100){
            }
        }

        for(int i = 0;i< TestProperties.playerCount;i++) {
            connectorstmp.get(i).sendMessageToServer(RequestBuilder.gameJoinPlayerRequest(lobbySize-1));
            timer = System.currentTimeMillis();
            while (timer > System.currentTimeMillis() - 100){
            }
        }

        while (timer > System.currentTimeMillis() - 10000){
        }
        while (!finished.get() && !serverHasFailed()){
        }

        gameManager.pauseGame(1);
        gameManager.continueGame(1);
        gameManager.abortGame(1,true);
        checkServer();

        LOGGER.debug("finished connection test");

        System.exit(0);
    }

    public static class TestClientModule extends ClientModule {

        private static MessageHandler d = new MessageHandler();

        public TestClientModule() {
            super(ClientConnector.class,null);
            ListenerHandler.registerListener(d);
        }
    }

    private static int removePlayer = 0;
    public static class MessageHandler implements GameInitNotificationListener, GameStartNotificationListener, FinishNotificationListener, LobbyResponseListener, ServerJoinResponseListener,RoundStartNotificationListener,ErrorNotificationListener,BattleshipsExceptionListener,GameJoinPlayerResponseListener {
        @Override
        public void onGameInitNotification(GameInitNotification message, int clientId) {
            if(clientId == TestProperties.playerCount-1)return;
            LOGGER.info("GameInitNotification");
            synchronized (finished) {
                finished.set(false);
            }
            configuration = message.getConfiguration();
            connectors.get(clientId).sendMessageToServer(RequestBuilder.placeShipsRequest(getPlacement(message.getConfiguration())));
        }

        @Override
        public void onGameJoinPlayerResponse(GameJoinPlayerResponse message, int clientId) {
            if(++removePlayer==TestProperties.playerCount){
                connectors.get(clientId).sendMessageToServer(RequestBuilder.gameLeaveRequest());
            }
        }

        @Override
        public void onGameStartNotification(GameStartNotification message, int clientId) {
            if(clientId == TestProperties.playerCount-1)return;
            LOGGER.info("GameStartNotification");
//            connectors.get(clientId).sendMessageToServer(RequestBuilder.shotsRequest(getShots(configuration, 1)));
//            connectors.get(clientId).sendMessageToServer(RequestBuilder.shotsRequest(getShots(configuration, 2)));
            connectors.get(clientId).sendMessageToServer(RequestBuilder.shotsRequest(getShots(configuration, 0)));

        }

        @Override
        public void onFinishNotification(FinishNotification message, int clientId) {
            if(clientId == TestProperties.playerCount-1)return;
            LOGGER.info("FinishNotification");
            synchronized (finished) {
                finished.set(true);
            }
        }

        @Override
        public void onLobbyResponse(LobbyResponse message, int clientId) {
            LOGGER.info("LobbyResponse");
            lobbySize = message.getGames().size();
        }

        static int count = 0;


        @Override
        public void onServerJoinResponse(ServerJoinResponse message, int clientId) {
            LOGGER.info("ServerJoinResponse");
            ids.add(message.getClientId());
            connectors.put(message.getClientId(), connectorstmp.get(count++));
        }

        @Override
        public void onBattleshipException(BattleshipException exception, int clientId) {
            LOGGER.info("BattleshipException", exception);
        }

        @Override
        public void onErrorNotification(ErrorNotification message, int clientId) {
            LOGGER.info("ErrorNotification");
            LOGGER.info("MessageId: {}, ErrorType: {}, Reason: {}", message.getReferenceMessageId(), message.getErrorType(), message.getReason());
        }

        @Override
        public void onRoundStartNotification(RoundStartNotification message, int clientId) {
            if(clientId == TestProperties.playerCount-1)return;
            LOGGER.info("RoundStartNotification");
            connectors.get(clientId).sendMessageToServer(RequestBuilder.remainingTimeRequest());
//            connectors.get(clientId).sendMessageToServer(RequestBuilder.shotsRequest(getShots(configuration, 1)));
//            connectors.get(clientId).sendMessageToServer(RequestBuilder.shotsRequest(getShots(configuration, 2)));
            connectors.get(clientId).sendMessageToServer(RequestBuilder.shotsRequest(getShots(configuration, 0)));

        }
    }

    private static Map<Integer, PlacementInfo> getPlacement(Configuration configuration){
        Map<Integer, PlacementInfo> ships = Maps.newConcurrentMap();
        if(TestProperties.simple){
            ships.put(0,new PlacementInfo(new Point2D(0,0), Rotation.NONE));
        }else {
            PlacementInfo info = new PlacementInfo(new Point2D(RANDOM.nextInt(configuration.getWidth() - 3), RANDOM.nextInt(configuration.getHeight() - 3)), Rotation.NONE);
            ships.put(0, info);
            placementInfos.add(info);
        }
        return ships;
    }

    private static List<PlacementInfo> placementInfos = Lists.newArrayList();

    private static List<Shot> getShots(Configuration configuration, int flag){
        List<Shot> shots = Lists.newArrayList();
        if (TestProperties.simple){
            shots.add(new Shot(ids.get(0), new Point2D(0, 0)));
            shots.add(new Shot(ids.get(0), new Point2D(1, 0)));
            shots.add(new Shot(ids.get(0), new Point2D(0, 1)));
            if(flag == 1){
                shots.add(new Shot(ids.get(0), new Point2D(1, 1)));
            }
        } else {
            switch (TestProperties.testCase){
                case 0:
                    if (RANDOM.nextBoolean()) {
                        shots.add(new Shot(ids.get(RANDOM.nextInt(ids.size())), new Point2D(3, 4)));
                    } else {
                        shots.add(new Shot(ids.get(RANDOM.nextInt(ids.size())), new Point2D(3, 3)));
                        shots.add(new Shot(ids.get(RANDOM.nextInt(ids.size())), new Point2D(4, 3)));
                    }
                    break;
                case 1:
//                    if(RANDOM.nextInt(3) == 0){
                        Point2D hit = placementInfos.get(RANDOM.nextInt(placementInfos.size())).getPosition();
                        LOGGER.error(hit);
                        shots.add(new Shot(ids.get(RANDOM.nextInt(ids.size())),hit.getPointWithOffset(RANDOM.nextInt(3),RANDOM.nextInt(3))));
                    shots.add(new Shot(ids.get(RANDOM.nextInt(ids.size())),hit.getPointWithOffset(RANDOM.nextInt(3),RANDOM.nextInt(3))));
                    shots.add(new Shot(ids.get(RANDOM.nextInt(ids.size())),hit.getPointWithOffset(RANDOM.nextInt(3),RANDOM.nextInt(3))));
                    break;
//                    }
                default:
                    shots.add(new Shot(ids.get(RANDOM.nextInt(ids.size())),new Point2D(RANDOM.nextInt(configuration.getWidth()),RANDOM.nextInt(configuration.getHeight()))));
                    shots.add(new Shot(ids.get(RANDOM.nextInt(ids.size())),new Point2D(RANDOM.nextInt(configuration.getWidth()),RANDOM.nextInt(configuration.getHeight()))));
                    shots.add(new Shot(ids.get(RANDOM.nextInt(ids.size())),new Point2D(RANDOM.nextInt(configuration.getWidth()),RANDOM.nextInt(configuration.getHeight()))));
                    break;
            }
        }
        return shots;
    }
}
