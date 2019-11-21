package de.upb.codingpirates.battleships.server.network;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import de.upb.codingpirates.battleships.network.ConnectionHandler;
import de.upb.codingpirates.battleships.network.network.module.ServerNetworkModule;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.handler.*;

public class ServerModule extends AbstractModule {
    @Override
    protected void configure() {
        this.install(new ServerNetworkModule());

        //bind interface Connectionhandler to Class ClientManager in one instance
        this.bind(ConnectionHandler.class).to(ClientManager.class).in(Singleton.class);
        //bind class GameManager in one instance
        this.bind(GameManager.class).in(Singleton.class);

        //bind all message handler to one instance
        this.bind(ConnectionClosedReportHandler.class).in(Singleton.class);
        this.bind(GameJoinPlayerRequestHandler.class).in(Singleton.class);
        this.bind(GameJoinSpectatorRequestHandler.class).in(Singleton.class);
        this.bind(LobbyRequestHandler.class).in(Singleton.class);
        this.bind(PlaceShipsRequestHandler.class).in(Singleton.class);
        this.bind(PlayerGameStateRequestHandler.class).in(Singleton.class);
        this.bind(PointsRequestHandler.class).in(Singleton.class);
        this.bind(RemainingTimeRequestHandler.class).in(Singleton.class);
        this.bind(ServerJoinRequestHandler.class).in(Singleton.class);
        this.bind(ShotsRequestHandler.class).in(Singleton.class);
        this.bind(SpectatorGameStateRequestHandler.class).in(Singleton.class);

    }
}
