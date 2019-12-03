package de.upb.codingpirates.battleships.server.network;

import de.upb.codingpirates.battleships.network.NetworkApplication;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

public class ServerApplication extends NetworkApplication {
    private static final Logger LOGGER = LogManager.getLogger();

    @Nonnull
    private final ClientManager clientManager;
    @Nonnull
    private final GameManager gameManager;

    @SuppressWarnings({"RedundantCast", "ConstantConditions"})
    public ServerApplication() throws InstantiationException, IllegalAccessException, IllegalStateException {
        LOGGER.debug("Starting server module");
        this.useModule(ServerModule.class).run();
        this.clientManager = (ClientManager) this.getHandler();
        if (clientManager == null) throw new IllegalStateException("ClientManager is null");
        this.gameManager = this.injector.getInstance(GameManager.class);
        if (gameManager == null) throw new IllegalStateException("GameManager is null");
    }

    @Nonnull
    public ClientManager getClientManager() {
        return clientManager;
    }

    @Nonnull
    public GameManager getGameManager() {
        return gameManager;
    }
}