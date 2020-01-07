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
import de.upb.codingpirates.battleships.server.exceptions.GameFullExeption;
import de.upb.codingpirates.battleships.server.exceptions.InvalidGameSizeException;
import de.upb.codingpirates.battleships.server.util.ServerMarker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TournamentHandler implements Handler, Runnable{
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
    private final Map<Integer, Integer> score = Collections.synchronizedMap(Maps.newHashMap());
    @Nonnull
    private final String name;
    private final int tournamentId;
    private boolean started;

    private int gameSize = 0;

    public TournamentHandler(@Nonnull String name, @Nonnull ClientManager clientManager, @Nonnull GameManager gameManager,@Nonnull Configuration configuration, int id) {
        this.clientManager = clientManager;
        this.configuration = configuration;
        this.gameManager = gameManager;
        this.name = name;
        this.tournamentId = id;
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
        this.started = true;
        List<Client> players = Lists.newArrayList(player.values());
        Collections.shuffle(players);
        this.createGames((int)(((float)players.size() / (float)this.configuration.getMaxPlayerCount())+0.5f));
        LOGGER.debug(ServerMarker.TOURNAMENT, "Create {} games for tournament {}. {} / {} = {} + {} = {}", this.games.size(), this.getTournamentId(),(float)players.size(),(float)this.configuration.getMaxPlayerCount(),(float)players.size() / (float)this.configuration.getMaxPlayerCount(),0.5f,((float)players.size() / (float)this.configuration.getMaxPlayerCount())+0.5f);
        try {
            while (!players.isEmpty()) {
                for (GameHandler manager : games.values()) {
                    if (players.isEmpty()) {
                        break;
                    }
                    LOGGER.debug(ServerMarker.TOURNAMENT, "try to add player {} to game {}", players.get(0).getId(), manager.getGame().getId());
                    manager.addClient(ClientType.PLAYER, players.get(0));
                    players.remove(0);
                }
            }

        }catch (GameFullExeption e){
            LOGGER.info(ServerMarker.TOURNAMENT, "Could not add player {} to game, because all games are full", players.get(0));
        }
        //test for games with only one player
        this.games.entrySet().removeIf(entry-> {
           if(entry.getValue().getPlayers().size() < 2){
               Collection<Client> player = Lists.newArrayList(entry.getValue().getPlayers());
               player.forEach((player1)-> {
                   this.player.remove(player1.getId());
                   entry.getValue().removeClient(player1.getId());
               });
               return true;
           }
           return false;
        });
        this.games.values().forEach(GameHandler::launchGame);
    }

    private void createGames(int amount) throws InvalidGameSizeException {
        for (int i = 0;i< amount;i++) {
            GameHandler handler = gameManager.createGame(configuration, name + "_" + gameSize++, this);
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

    @Nonnull
    public Map<Integer, GameHandler> getGames() {
        return games;
    }

    public int getTournamentId() {
        return tournamentId;
    }

    public void gameFinished(int id){
        Map<Integer, Integer> score = this.games.get(id).getScore();
        score.forEach((client, score1)-> this.score.compute(client,(client1, score2)->score1+score2));
    }

    @Override
    public void run() {
        if(!this.started)return;
    }
}