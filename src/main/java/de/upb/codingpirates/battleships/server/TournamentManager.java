package de.upb.codingpirates.battleships.server;

import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.logic.Configuration;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.exceptions.game.NotAllowedException;
import de.upb.codingpirates.battleships.network.id.IdManager;
import de.upb.codingpirates.battleships.server.exceptions.InvalidGameSizeException;
import de.upb.codingpirates.battleships.server.game.TournamentHandler;
import de.upb.codingpirates.battleships.server.util.ConfigurationChecker;
import de.upb.codingpirates.battleships.server.util.ServerMarker;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;

public class TournamentManager implements ConfigurationChecker {
    private static final Logger LOGGER = LogManager.getLogger();

    @Nonnull
    private final GameManager gameManager;
    @Nonnull
    private final ClientManager clientManager;
    @Nonnull
    private final IdManager idManager;

    @Nonnull
    private final ObservableMap<Integer, TournamentHandler> tournamentHandlerByInt = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());
    @Nonnull
    private final Map<Integer, Integer> clientToTournament = Collections.synchronizedMap(Maps.newHashMap());

    @Inject
    public TournamentManager(@Nonnull GameManager gameManager, @Nonnull ClientManager clientManager, @Nonnull IdManager idManager) {
        this.gameManager = gameManager;
        this.clientManager = clientManager;
        this.idManager = idManager;
    }

    public TournamentHandler createTournament(@Nonnull Configuration configuration, @Nonnull String name) throws InvalidGameSizeException {
        checkField(configuration);
        int id = this.idManager.generate().getInt();
        LOGGER.info(ServerMarker.TOURNAMENT, "Create Tournament: {} with id: {}", name, id);
        TournamentHandler tournamentHandler = new TournamentHandler(name, clientManager, gameManager, configuration, id);
        this.tournamentHandlerByInt.put(id, tournamentHandler);
        return tournamentHandler;
    }

    public void addClientToTournament(int tournamentId, @Nonnull Client client, @Nonnull ClientType clientType) throws NotAllowedException, InvalidActionException {
        if(clientType.equals(ClientType.SPECTATOR)) throw new NotAllowedException("You cannot join as spectator");
        LOGGER.debug(ServerMarker.TOURNAMENT, "Adding client {}, with type {}, to tournament {}", client.getId(), clientType, tournamentId);
        if(this.clientToTournament.containsKey(client.getId())){
            if(clientType.equals(ClientType.PLAYER)){
                throw new NotAllowedException("you cannot join a game while participating");
            }
        }
        this.clientToTournament.put(client.getId(),tournamentId);
        if(!this.tournamentHandlerByInt.containsKey(tournamentId)){
            throw new InvalidActionException("this tournament does not exist");
        }
        this.tournamentHandlerByInt.get(tournamentId).addClient(clientType,client);
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
}
