package de.upb.codingpirates.battleships.server;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.upb.codingpirates.battleships.network.ConnectionHandler;
import de.upb.codingpirates.battleships.network.network.module.ServerNetworkModule;
import de.upb.codingpirates.battleships.server.handler.*;
import de.upb.codingpirates.battleships.server.util.ServerMarker;

public class ServerModule extends AbstractModule {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void configure() {
        this.install(new ServerNetworkModule());

        LOGGER.info(ServerMarker.CONNECTION,"Binding server classes");

        //bind interface ConnectionHandler to Class ClientManager in one instance
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
