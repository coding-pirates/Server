package de.upb.codingpirates.battleships.server.test;


import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.network.ConnectionHandler;
import de.upb.codingpirates.battleships.network.NetworkApplication;
import de.upb.codingpirates.battleships.network.Properties;
import de.upb.codingpirates.battleships.network.connectionmanager.ClientConnectionManager;
import de.upb.codingpirates.battleships.network.exceptions.BattleshipException;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.request.GameJoinPlayerRequest;
import de.upb.codingpirates.battleships.network.message.request.LobbyRequest;
import de.upb.codingpirates.battleships.network.message.request.ServerJoinRequest;
import de.upb.codingpirates.battleships.network.network.module.ClientNetworkModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;

public class ServerTests {

    private static final Logger LOGGER = LogManager.getLogger();
    private static NetworkApplication client;
    private static NetworkApplication client2;
    private static ClientConnector connector;
    private static ClientConnector connector2;
    private static int id1;
    private static int id2;
    private static int lobbySize;

    public static boolean gameinit1;
    public static boolean gameinit2;

    public static boolean gamestart1;
    public static boolean gamestart2;

    @Test
    public void test() throws IllegalAccessException, IOException, InstantiationException {
        long timer = 0;
        LOGGER.debug("start connection test");

        client = ServerTests.Client.main();
        client2 = ServerTests.Client.main();

        connector = (ClientConnector) client.getHandler();
        connector2 = (ClientConnector) client2.getHandler();


        connector.connect(InetAddress.getLocalHost().getHostAddress(), Properties.PORT);
        connector2.connect(InetAddress.getLocalHost().getHostAddress(), Properties.PORT);

        timer = System.currentTimeMillis();
        while (timer > System.currentTimeMillis() - 1000){
        }

        connector.sendMessageToServer(new ServerJoinRequest("peter", ClientType.PLAYER));
        connector2.sendMessageToServer(new ServerJoinRequest("hans", ClientType.PLAYER));

        timer = System.currentTimeMillis();
        while (timer > System.currentTimeMillis() - 1000){
        }

        connector.sendMessageToServer(new LobbyRequest());
        connector2.sendMessageToServer(new LobbyRequest());

        timer = System.currentTimeMillis();
        while (timer > System.currentTimeMillis() - 1000){
        }

        connector.sendMessageToServer(new GameJoinPlayerRequest(lobbySize-1));
        connector2.sendMessageToServer(new GameJoinPlayerRequest(lobbySize-1));

        timer = System.currentTimeMillis();
        while (timer > System.currentTimeMillis() - 50000){
        }

        while (true){

        }
//        LOGGER.debug("finished connection test");
    }

    public static ClientConnector getConnector() {
        return connector;
    }

    public static NetworkApplication getClient() {
        return client;
    }

    public static ClientConnector getConnector2() {
        return connector2;
    }

    public static NetworkApplication getClient2() {
        return client2;
    }

    public static int getId1() {
        return id1;
    }

    public static int getId2() {
        return id2;
    }

    public static void setLobbySize(int size){
        lobbySize = size;
    }

    private static class Client {
        public static NetworkApplication main() throws IllegalAccessException, IOException, InstantiationException {
            LOGGER.info("Start client network module");

            NetworkApplication application = new NetworkApplication();
            application.useModule(ServerTests.ClientModule.class).run();
            return application;
        }
    }

    public static class ClientModule extends AbstractModule {
        @Override
        protected void configure() {
            this.install(new ClientNetworkModule());

            this.bind(ConnectionHandler.class).to(ClientConnector.class).in(Singleton.class);
        }
    }

    public static class ClientConnector implements ConnectionHandler {
        @Inject
        private ClientConnectionManager clientConnector;

        public void connect(String host, int port) throws IOException {
            this.clientConnector.create(host, port);
        }

        public void sendMessageToServer(Message message) throws IOException {
            this.clientConnector.send(message);
        }

        public void disconnect() throws IOException {
            this.clientConnector.disconnect();
        }

        @Override
        public void handleBattleshipException(BattleshipException e) {

        }
    }
}
