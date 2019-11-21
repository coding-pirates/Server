package de.upb.codingpirates.battleships.server.network;

import de.upb.codingpirates.battleships.network.NetworkApplication;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;

import javax.annotation.Nonnull;

public class ServerApplication extends NetworkApplication {

    @Nonnull
    private final ClientManager clientManager;
    @Nonnull
    private final GameManager gameManager;

    @SuppressWarnings({"RedundantCast", "ConstantConditions"})
    public ServerApplication() throws InstantiationException, IllegalAccessException, IllegalStateException {
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
