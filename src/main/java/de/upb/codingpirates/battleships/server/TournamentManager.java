package de.upb.codingpirates.battleships.server;

import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.logic.Configuration;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.id.IdManager;
import de.upb.codingpirates.battleships.server.game.TournamentHandler;
import de.upb.codingpirates.battleships.server.util.ServerMarker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;

public class TournamentManager {
    private static final Logger LOGGER = LogManager.getLogger();

    @Nonnull
    private final GameManager gameManager;
    @Nonnull
    private final ClientManager clientManager;
    @Nonnull
    private final IdManager idManager;

    @Nonnull
    private final Map<Integer, TournamentHandler> tournamentHandlerByInt = new HashMap<>();

    @Nonnull
    private final Map<Integer, Integer> clientToTournament = Collections.synchronizedMap(Maps.newHashMap());

    @Inject
    public TournamentManager(@Nonnull GameManager gameManager, @Nonnull ClientManager clientManager, @Nonnull IdManager idManager) {
        this.gameManager = gameManager;
        this.clientManager = clientManager;
        this.idManager = idManager;
        new Timer("Server Main").schedule(new TimerTask() {
            @Override
            public void run() {
                TournamentManager.this.run();
            }
        }, 1L, 1L);
    }

    public TournamentHandler createTournament(@Nonnull List<Configuration> configuration, @Nonnull String name, int rounds) {
        int id = this.idManager.generate().getInt();
        LOGGER.info(ServerMarker.TOURNAMENT, "Create Tournament: {} with id: {}", name, id);
        TournamentHandler tournamentHandler = new TournamentHandler(name, clientManager, gameManager, configuration, id, rounds);
        this.tournamentHandlerByInt.put(id, tournamentHandler);
        return tournamentHandler;
    }

    @Nonnull
    public TournamentHandler getTournamentHandler(int tournamentId) throws InvalidActionException {
        if(!tournamentHandlerByInt.containsKey(tournamentId))
            throw new InvalidActionException("tournament does not exist");
        return tournamentHandlerByInt.get(tournamentId);
    }

    @Nonnull
    public TournamentHandler getTournamentByClient(int client) throws InvalidActionException {
        if(!clientToTournament.containsKey(client))
            throw new InvalidActionException("You do not participate in a tournament");
        return tournamentHandlerByInt.get(clientToTournament.get(client));
    }

    public boolean isParticipating(int clientId){
        return clientToTournament.containsKey(clientId);
    }

    /**
     * run method for every game
     */
    private void run() {
        this.tournamentHandlerByInt.values().forEach(TournamentHandler::run);
    }

    public Map<Integer, TournamentHandler> getTournamentMappings() {
        return tournamentHandlerByInt;
    }
}
