package de.upb.codingpirates.battleships.server.test;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.client.Handler;
import de.upb.codingpirates.battleships.client.network.ClientApplication;
import de.upb.codingpirates.battleships.client.network.ClientConnector;
import de.upb.codingpirates.battleships.client.network.ClientModule;
import de.upb.codingpirates.battleships.logic.*;
import de.upb.codingpirates.battleships.network.Properties;
import de.upb.codingpirates.battleships.network.exceptions.BattleshipException;
import de.upb.codingpirates.battleships.network.message.notification.*;
import de.upb.codingpirates.battleships.network.message.report.ConnectionClosedReport;
import de.upb.codingpirates.battleships.network.message.request.*;
import de.upb.codingpirates.battleships.network.message.response.*;
import de.upb.codingpirates.battleships.server.network.ServerApplication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerTests {
    public static final Configuration TEST_CONFIG = new Configuration(TestProperties.playerCount, 10, 10, 4, 1, 1, 5000, 100, new HashMap<Integer, ShipType>(){{put(0,new ShipType(Lists.newArrayList(new Point2D(1,1),new Point2D(2,1),new Point2D(1,2))));}}, 1, PenaltyType.POINTLOSS);

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
    public void test() throws IOException {
        if(!TestProperties.isServerOnline) {
            new Thread(() -> {
                ServerApplication server = null;
                try {
                    server = new ServerApplication();
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                server.getGameManager().createGame(TEST_CONFIG, "test", false);
                long timer = System.currentTimeMillis();
                while (timer > System.currentTimeMillis() - 20000) {

                }
            }).start();
        }


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
            connectorstmp.get(i).sendMessageToServer(new ServerJoinRequest(names[i], ClientType.PLAYER));
            timer = System.currentTimeMillis();
            while (timer > System.currentTimeMillis() - 10){
            }
        }

        for(int i = 0;i< TestProperties.playerCount;i++) {
            connectorstmp.get(i).sendMessageToServer(new LobbyRequest());
            timer = System.currentTimeMillis();
            while (timer > System.currentTimeMillis() - 100){
            }
        }

        for(int i = 0;i< TestProperties.playerCount;i++) {
            connectorstmp.get(i).sendMessageToServer(new GameJoinPlayerRequest(lobbySize-1));
            timer = System.currentTimeMillis();
            while (timer > System.currentTimeMillis() - 100){
            }
        }

        while (timer > System.currentTimeMillis() - 10000){
        }
        while (!finished.get()){
        }

        LOGGER.debug("finished connection test");
    }

    public static class TestClientModule extends ClientModule<ClientConnector> {

        public TestClientModule() {
            super(ClientConnector.class,MessageHandler.class);
        }
    }

    public static class MessageHandler implements Handler {
        @Override
        public void handleGameInitNotification(GameInitNotification message, int clientId) {
            LOGGER.info("GameInitNotification");
            synchronized (finished) {
                finished.set(false);
            }
            configuration = message.getConfiguration();
            try {
                connectors.get(clientId).sendMessageToServer(new PlaceShipsRequest(getPlacement(message.getConfiguration())));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void handleGameStartNotification(GameStartNotification message, int clientId) {
            LOGGER.info("GameStartNotification");
            try {
                connectors.get(clientId).sendMessageToServer(new ShotsRequest(getShots(configuration)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void handleFinishNotification(FinishNotification message, int clientId) {
            LOGGER.info("FinishNotification");
            synchronized (finished) {
                finished.set(true);
            }
        }

        @Override
        public void handleLobbyResponse(LobbyResponse message, int clientId) {
            LOGGER.info("LobbyResponse");
            lobbySize = message.getGames().size();
        }

        static int count = 0;


        @Override
        public void handleServerJoinResponse(ServerJoinResponse message, int clientId) {
            LOGGER.info("ServerJoinResponse");
            ids.add(message.getClientId());
            connectors.put(message.getClientId(), connectorstmp.get(count++));
        }

        @Override
        public void handleBattleshipException(BattleshipException exception, int clientId) {
            LOGGER.info("BattleshipException", exception);
        }

        @Override
        public void handleErrorNotification(ErrorNotification message, int clientId) {
            LOGGER.info("ErrorNotification");
            LOGGER.info("MessageId: {}, ErrorType: {}, Reason: {}", message.getReferenceMessageId(), message.getErrorType(), message.getReason());
        }

        @Override
        public void handleRoundStartNotification(RoundStartNotification message, int clientId) {
            LOGGER.info("RoundStartNotification");
            try {
                connectors.get(clientId).sendMessageToServer(new ShotsRequest(getShots(configuration)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }




        @Override
        public void handleContinueNotification(ContinueNotification message, int clientId) {
            LOGGER.info("ContinueNotification");

        }

        @Override
        public void handleConnectionClosedReport(ConnectionClosedReport message, int clientId) {
            LOGGER.info("ConnectionClosedReport");

        }



        @Override
        public void handleGameJoinPlayer(GameJoinPlayerResponse message, int clientId) {
            LOGGER.info("GameJoinPlayer");

        }

        @Override
        public void handleGameJoinSpectator(GameJoinSpectatorResponse message, int clientId) {
            LOGGER.info("GameJoinSpectator");

        }

        @Override
        public void handleLeaveNotification(LeaveNotification message, int clientId) {
            LOGGER.info("LeaveNotification");

        }



        @Override
        public void handlePauseNotification(PauseNotification message, int clientId) {
            LOGGER.info("PauseNotification");

        }

        @Override
        public void handlePlaceShipsResponse(PlaceShipsResponse message, int clientId) {
            LOGGER.info("PlaceShipsResponse");

        }

        @Override
        public void handlePlayerUpdateNotification(PlayerUpdateNotification message, int clientId) {
            LOGGER.info("PlayerUpdateNotification");

        }

        @Override
        public void handleSpectatorUpdateNotification(SpectatorUpdateNotification message, int clientId) {
            LOGGER.info("SpectatorUpdateNotification");

        }

        @Override
        public void handlePointsResponse(PointsResponse message, int clientId) {
            LOGGER.info("SpectatorPointsResponse");

        }

        @Override
        public void handleRemainingTimeResponse(RemainingTimeResponse message, int clientId) {
            LOGGER.info("RemainingTimeResponse");

        }

        @Override
        public void handleShotsResponse(ShotsResponse message, int clientId) {
            LOGGER.info("ShotsResponse");

        }

        @Override
        public void handleSpectatorGameStateResponse(SpectatorGameStateResponse message, int clientId) {
            LOGGER.info("SpectatorGameStateResponse");

        }

        @Override
        public void handlePlayerGameStateResponse(PlayerGameStateResponse message, int clientId) {
            LOGGER.info("PlayerGameStateResponse");

        }


    }

    private static Map<Integer, PlacementInfo> getPlacement(Configuration configuration){
        Map<Integer, PlacementInfo> ships = Maps.newConcurrentMap();
        if(TestProperties.simple){
            ships.put(0,new PlacementInfo(new Point2D(2,2), Rotation.NONE));
        }else {
            PlacementInfo info = new PlacementInfo(new Point2D(RANDOM.nextInt(configuration.getWidth() - 3), RANDOM.nextInt(configuration.getHeight() - 3)), Rotation.NONE);
            ships.put(0, info);
            placementInfos.add(info);
        }
        return ships;
    }

    private static List<PlacementInfo> placementInfos = Lists.newArrayList();

    private static List<Shot> getShots(Configuration configuration){
        List<Shot> shots = Lists.newArrayList();
        if(TestProperties.simple){
            shots.add(new Shot(ids.get(0), new Point2D(3, 4)));
            shots.add(new Shot(ids.get(0), new Point2D(3, 3)));
            shots.add(new Shot(ids.get(0), new Point2D(4, 3)));
        }else {
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
