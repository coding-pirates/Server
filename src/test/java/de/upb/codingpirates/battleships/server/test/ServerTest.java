package de.upb.codingpirates.battleships.server.test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.upb.codingpirates.battleships.network.dispatcher.MessageDispatcher;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.exceptions.InvalidGameSizeException;
import de.upb.codingpirates.battleships.server.network.ServerModule;

import static de.upb.codingpirates.battleships.server.test.GameTest.TEST_CONFIG;

public abstract class ServerTest {

    private static Thread serverThread;
    private static boolean failed = false;

    protected static void checkServer(){
        if(serverThread != null){
            serverThread.stop();
        }

        if (failed)
            throw new IllegalStateException("Server could not create game");
    }

    protected static void serverStart(){
        if(!TestProperties.isServerOnline) {
            serverThread = new Thread(() -> {
                Injector injector = Guice.createInjector(new ServerModule());
                injector.getInstance(MessageDispatcher.class);
                GameManager manager = injector.getInstance(GameManager.class);
                try {
                    manager.createGame(TEST_CONFIG, "test");
                } catch (InvalidGameSizeException e) {
                    failed = true;
                    serverThread.stop();
                }
                while (true) {

                }
            });
            serverThread.start();
        }
    }

    protected static boolean serverHasFailed(){
        return failed;
    }
}
