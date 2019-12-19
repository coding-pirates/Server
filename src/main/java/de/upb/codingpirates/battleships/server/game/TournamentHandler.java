package de.upb.codingpirates.battleships.server.game;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.logic.Configuration;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.exceptions.InvalidGameSizeException;
import de.upb.codingpirates.battleships.server.util.ServerMarker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TournamentHandler implements Handler{
    private static final Logger LOGGER = LogManager.getLogger();

    @Nonnull
    private final ClientManager clientManager;
    @Nonnull
    private final Configuration configuration;
    @Nonnull
    private final GameManager gameManager;

    /**
     * maps client id to player
     */
    @Nonnull
    private final Map<Integer, Client> player = Collections.synchronizedMap(Maps.newHashMap());
    @Nonnull
    private final Map<Integer, GameHandler> games = Collections.synchronizedMap(Maps.newHashMap());
    @Nonnull
    private final String name;
    private int gameSize = 0;

    public TournamentHandler(@Nonnull String name, @Nonnull ClientManager clientManager, @Nonnull GameManager gameManager,@Nonnull Configuration configuration) {
        this.clientManager = clientManager;
        this.configuration = configuration;
        this.gameManager = gameManager;
        this.name = name;
    }

    @Override
    public void addClient(@Nonnull ClientType type, @Nonnull Client client) throws InvalidActionException {
        if(type.equals(ClientType.SPECTATOR)){
            LOGGER.warn(ServerMarker.TOURNAMENT,"Could not add Spectator to Tournament");
        }else {
            player.put(client.getId(),client);
        }

    }

    public void start() throws InvalidGameSizeException, InvalidActionException {
        List<Client> players = Lists.newArrayList(player.values());
        Collections.shuffle(players);
        this.createGames((int)(((float)players.size() / (float)this.configuration.getMaxPlayerCount())+0.5f));
        while (!players.isEmpty()){
            for (GameHandler manager: games.values()){
                if(players.isEmpty()){
                    break;
                }
                manager.addClient(ClientType.PLAYER,players.remove(0));
            }
        }
    }

    private void createGames(int amount) throws InvalidGameSizeException {
        for (int i = 0;i< amount;i++) {
            GameHandler handler = gameManager.createGame(configuration, name + "_" + gameSize++, true);
            games.put(handler.getGame().getId(), handler);
        }
    }

    @Override
    public void removeClient(int client) throws InvalidActionException {
        player.remove(client);
        gameManager.getGameHandlerForClientId(client).removeClient(client);
    }

    @Nonnull
    @Override
    public List<Client> getAllClients() {
        return Lists.newArrayList(getPlayers());
    }

    @Nonnull
    @Override
    public Collection<Client> getPlayers() {
        return this.player.values();
    }

    @Nonnull
    @Override
    public Collection<Client> getSpectators() {
        return ImmutableList.of();
    }

    @Nonnull
    @Override
    public Configuration getConfiguration() {
        return this.configuration;
    }
}