package de.upb.codingpirates.battleships.server;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.upb.codingpirates.battleships.network.ConnectionHandler;
import de.upb.codingpirates.battleships.network.network.module.ServerNetworkModule;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.TournamentManager;
import de.upb.codingpirates.battleships.server.gui.controllers.BoundingBoxConfigurationValidator;
import de.upb.codingpirates.battleships.server.gui.controllers.ConfigurationValidator;
import de.upb.codingpirates.battleships.server.handler.*;
import de.upb.codingpirates.battleships.server.util.ServerMarker;

public final class ServerModule extends AbstractModule {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void configure() {
        this.install(new ServerNetworkModule());

        LOGGER.info(ServerMarker.CONNECTION, "Binding server classes");

        bind(ConfigurationValidator.class).to(BoundingBoxConfigurationValidator.class);

        bind(ConnectionHandler.class).to(ClientManager.class).in(Singleton.class);
        bind(ClientManager.class).in(Singleton.class);
        bind(GameManager.class).in(Singleton.class);
        bind(TournamentManager.class).in(Singleton.class);

        //bind all message handler to one instance
        bind(ConnectionClosedReportHandler.class).in(Singleton.class);
        bind(GameJoinPlayerRequestHandler.class).in(Singleton.class);
        bind(GameJoinSpectatorRequestHandler.class).in(Singleton.class);
        bind(LobbyRequestHandler.class).in(Singleton.class);
        bind(PlaceShipsRequestHandler.class).in(Singleton.class);
        bind(PlayerGameStateRequestHandler.class).in(Singleton.class);
        bind(PointsRequestHandler.class).in(Singleton.class);
        bind(RemainingTimeRequestHandler.class).in(Singleton.class);
        bind(ServerJoinRequestHandler.class).in(Singleton.class);
        bind(ShotsRequestHandler.class).in(Singleton.class);
        bind(SpectatorGameStateRequestHandler.class).in(Singleton.class);
    }
}
