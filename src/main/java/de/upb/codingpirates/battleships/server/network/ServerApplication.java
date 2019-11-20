package de.upb.codingpirates.battleships.server.network;

import de.upb.codingpirates.battleships.network.NetworkApplication;

import javax.annotation.Nonnull;

public class ServerApplication extends NetworkApplication {

    private final ClientManager clientManager;

    @SuppressWarnings({"RedundantCast", "ConstantConditions"})
    public ServerApplication() throws InstantiationException, IllegalAccessException, IllegalStateException {
        this.useModule(ServerModule.class).run();
        ClientManager clientManager = (ClientManager) this.getHandler();
        if (clientManager == null) throw new IllegalStateException("ClientManager is null");
        this.clientManager = clientManager;
    }

    @Nonnull
    @Override
    public ClientManager getHandler() {
        return clientManager;
    }
}
