package de.upb.codingpirates.battleships.server.game;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.logic.Configuration;
import de.upb.codingpirates.battleships.logic.GameState;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.message.notification.NotificationBuilder;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.exceptions.GameFullExeption;
import de.upb.codingpirates.battleships.server.exceptions.InvalidGameSizeException;
import de.upb.codingpirates.battleships.server.util.ServerMarker;
import de.upb.codingpirates.battleships.server.util.ServerProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

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
    private final int rounds;
    private boolean finished = false;

    private boolean started;
    private int roundCounter = 0;
    private List<GameHandler> newGames = Collections.synchronizedList(Lists.newArrayList());
    private int gameSize = 0;
    private long start = -1;

    public TournamentHandler(@Nonnull String name, @Nonnull ClientManager clientManager, @Nonnull GameManager gameManager,@Nonnull Configuration configuration, int id, int rounds) {
        this.clientManager = clientManager;
        this.configuration = configuration;
        this.gameManager = gameManager;
        this.name = name;
        this.tournamentId = id;
        this.rounds = rounds;
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
        this.start = -1;
        this.roundCounter++;
        this.started = true;
        List<Client> players = Lists.newArrayList(player.values());
        Collections.shuffle(players);
        this.createGames((int)(((float)players.size() / (float)this.configuration.getMaxPlayerCount())+0.5f));
        LOGGER.debug(ServerMarker.TOURNAMENT, "Create {} games for tournament {}. {} / {} = {} + {} = {} results in {}", this.games.size(), this.getTournamentId(),(float)players.size(),(float)this.configuration.getMaxPlayerCount(),(float)players.size() / (float)this.configuration.getMaxPlayerCount(),0.5f,((float)players.size() / (float)this.configuration.getMaxPlayerCount())+0.5f, (int)((float)players.size() / (float)this.configuration.getMaxPlayerCount())+0.5f);
        try {
            while (!players.isEmpty()) {
                for (GameHandler manager : this.newGames) {
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
        this.newGames.removeIf(gameHandler-> {
           if(gameHandler.getPlayers().size() < 2){
               Collection<Client> player = Lists.newArrayList(gameHandler.getPlayers());
               player.forEach((player1)-> {
                   this.player.remove(player1.getId());
                   gameHandler.removeClient(player1.getId());
               });
               return true;
           }
           this.games.put(gameHandler.getGame().getId(),gameHandler);
           return false;
        });
        this.newGames.forEach(GameHandler::launchGame);
    }

    private void createGames(int amount) throws InvalidGameSizeException {
        for (int i = 0;i< amount;i++) {
            GameHandler handler = gameManager.createGame(configuration, name + "_" + gameSize++, this);
            newGames.add(handler);
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
        if(!this.started | finished)return;
        if(System.currentTimeMillis() % 100 == 0){
            if(this.start != -1 && start < System.currentTimeMillis()){
                try {
                    this.start();
                } catch (InvalidGameSizeException | InvalidActionException e) {
                    LOGGER.error(ServerMarker.TOURNAMENT, "Could not start new games for tournament {} for round {}", this.tournamentId, this.roundCounter);
                }
            }
            if(this.newGames.stream().allMatch(newGame -> newGame.getGame().getState().equals(GameState.FINISHED))) {
                this.newGames.clear();
                if(this.rounds -1 <= roundCounter){
                    this.finishTournament();
                }else {
                    this.start = System.currentTimeMillis() + ServerProperties.TOURNAMENT_GAMEFINISH_TIME;
                }
            }
        }
    }

    private void finishTournament(){
        this.finished = true;
        OptionalInt winnerScore = score.values().stream().mapToInt(value -> value).max();
        Collection<Integer> winner;
        if(winnerScore.isPresent())
            winner = score.entrySet().stream().filter(entry -> entry.getValue() == winnerScore.getAsInt()).map(Map.Entry::getKey).collect(Collectors.toList());
        else
            winner = Lists.newArrayList();
        LOGGER.debug("Tournament {} has finished", tournamentId);
        this.clientManager.sendMessageToClients(NotificationBuilder.tournamentFinishNotification(winner),getAllClients());
    }
}