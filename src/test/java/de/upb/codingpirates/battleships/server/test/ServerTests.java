package de.upb.codingpirates.battleships.server.test;


import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.network.ConnectionHandler;
import de.upb.codingpirates.battleships.network.NetworkApplication;
import de.upb.codingpirates.battleships.network.Properties;
import de.upb.codingpirates.battleships.network.connectionmanager.ClientConnectionManager;
import de.upb.codingpirates.battleships.network.exceptions.BattleshipException;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.request.GameJoinPlayerRequest;
import de.upb.codingpirates.battleships.network.message.request.ServerJoinRequest;
import de.upb.codingpirates.battleships.network.network.module.ClientNetworkModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;

public class ServerTests {

    private static final Logger LOGGER = LogManager.getLogger();

    @Test
    public void test() throws IllegalAccessException, IOException, InstantiationException {
        LOGGER.debug("start");
        NetworkApplication client = ServerTests.Client.main();
        ClientConnector connector = (ClientConnector) client.getHandler();
        connector.connect(InetAddress.getLocalHost().getHostAddress(), Properties.PORT);
        connector.sendMessageToServer(new ServerJoinRequest("peter", ClientType.SPECTATOR));
        connector.sendMessageToServer(new GameJoinPlayerRequest(0));
        LOGGER.debug("finished");
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

            this.bind(ConnectionHandler.class).toInstance(new ServerTests.ClientConnector());
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
