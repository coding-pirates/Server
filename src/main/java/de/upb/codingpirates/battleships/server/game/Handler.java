package de.upb.codingpirates.battleships.server.game;

import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.logic.Configuration;
import de.upb.codingpirates.battleships.logic.Game;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.server.util.Translator;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public interface Handler extends Translator {

    void addClient(@Nonnull ClientType type, @Nonnull Client client) throws InvalidActionException;

    void removeClient(int client) throws InvalidActionException;

    @Nonnull
    List<Client> getAllClients();

    /**
     * @return all players
     */
    @Nonnull
    Collection<Client> getPlayers();

    /**
     * @return all spectators
     */
    @Nonnull
    Collection<Client> getSpectators();

    /**
     * @return the {@link Configuration} from the {@link Game} object
     */
    @Nonnull
    Configuration getConfiguration();
}