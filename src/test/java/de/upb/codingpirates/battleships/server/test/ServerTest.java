//package de.upb.codingpirates.battleships.server.test;
//
//import com.google.common.collect.Lists;
//import com.google.inject.Guice;
//import com.google.inject.Injector;
//import de.upb.codingpirates.battleships.logic.Configuration;
//import de.upb.codingpirates.battleships.logic.PenaltyType;
//import de.upb.codingpirates.battleships.logic.Point2D;
//import de.upb.codingpirates.battleships.logic.ShipType;
//import de.upb.codingpirates.battleships.network.dispatcher.MessageDispatcher;
//import de.upb.codingpirates.battleships.server.ClientManager;
//import de.upb.codingpirates.battleships.server.GameManager;
//import de.upb.codingpirates.battleships.server.ServerModule;
//import de.upb.codingpirates.battleships.server.TournamentManager;
//
//import java.util.HashMap;
//
//public abstract class ServerTest {
//    public static final Configuration TEST_CONFIG = new Configuration(TestProperties.playerCount, 10, 10, 3, 1, 1, 5000, 100, new HashMap<Integer, ShipType>(){{put(0,new ShipType(Lists.newArrayList(new Point2D(1,1),new Point2D(2,1),new Point2D(1,2))));}}, 1, PenaltyType.POINTLOSS);
//
//
//    private Thread serverThread;
//    private boolean failed = false;
//    protected ClientManager clientManager;
//    protected GameManager gameManager;
//    protected TournamentManager tournamentManager;
//
//    protected void checkServer(){
//        if(serverThread != null){
//            serverThread.stop();
//        }
//
//        if (failed)
//            throw new IllegalStateException("Server could not create game");
//    }
//
//    protected void serverStart(){
//        if(!TestProperties.isServerOnline) {
//            serverThread = new Thread(() -> {
//                Injector injector = Guice.createInjector(new ServerModule());
//                injector.getInstance(MessageDispatcher.class);
//                gameManager = injector.getInstance(GameManager.class);
//                clientManager = injector.getInstance(ClientManager.class);
//                tournamentManager = injector.getInstance(TournamentManager.class);
//                gameManager.createGame(TEST_CONFIG, "test");
//                gameManager.createGame(TEST_CONFIG, "test2");
//                while (true) {
//
//                }
//            });
//            serverThread.start();
//        }
//    }
//
//    protected boolean serverHasFailed(){
//        return failed;
//    }
//}
