package de.upb.codingpirates.battleships.server.game;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.logic.Configuration;
import de.upb.codingpirates.battleships.logic.Game;
import de.upb.codingpirates.battleships.logic.GameStage;
import de.upb.codingpirates.battleships.logic.TournamentState;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.message.notification.NotificationBuilder;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.util.GameListener;
import de.upb.codingpirates.battleships.server.util.ServerMarker;
import de.upb.codingpirates.battleships.server.util.ServerProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class TournamentHandler implements Runnable, GameListener {
    private static final Logger LOGGER = LogManager.getLogger();

    @Nonnull
    private final ClientManager clientManager;
    @Nonnull
    private final List<Configuration> configuration;
    @Nonnull
    private final GameManager gameManager;

    @Nonnull
    private final List<Game> games = Collections.synchronizedList(Lists.newArrayList());
    @Nonnull
    private final Map<Integer, Integer> score = Collections.synchronizedMap(Maps.newHashMap());
    @Nonnull
    private final String name;
    private final int tournamentId;
    private final int rounds;

    private TournamentState tournamentState = TournamentState.PREGAME;
    private GameHandler gameHandler;
    private int roundCount;
    private long timer;

    public TournamentHandler(@Nonnull String name, @Nonnull ClientManager clientManager, @Nonnull GameManager gameManager, @Nonnull List<Configuration> configuration, int id, int rounds) {
        this.clientManager = clientManager;
        this.configuration = configuration;
        this.gameManager = gameManager;
        this.name = name;
        this.tournamentId = id;
        this.rounds = rounds;
    }

    @Nonnull
    public List<Game> getGames() {
        return games;
    }

    public int getTournamentId() {
        return tournamentId;
    }

    @Override
    public void onGameFinished() {
        Map<Integer, Integer> score = this.gameHandler.getScore();
        score.forEach((client, score1)-> this.score.compute(client,(client1, score2)->score2 == null ? score1 : score1+score2));
        this.tournamentState = TournamentState.POSTGAME;
        this.timer = System.currentTimeMillis();
    }

    private Configuration getConfiguration() {
        return configuration.get(roundCount % configuration.size());
    }

    private void newGame(){
        GameHandler handler = gameManager.createGame(getConfiguration(),name+"_"+roundCount, this);
        this.games.add(handler.getGame());
        if(gameHandler != null) {
            gameHandler.getPlayers().forEach(player -> {
                try {
                    handler.addClient(player);
                } catch (InvalidActionException e) {
                    LOGGER.error(ServerMarker.TOURNAMENT, "Could not add player {} to next game ({})", player.getId(), roundCount);
                }
            });
            gameHandler.getSpectators().forEach(spectator -> {
                try {
                    handler.addClient(spectator);
                } catch (InvalidActionException e) {
                    LOGGER.error(ServerMarker.TOURNAMENT, "Could not add spectator {} to next game ({})", spectator.getId(), roundCount);
                }
            });
        }
        gameHandler = handler;
    }

    @Override
    public void run() {
        switch (tournamentState) {
            case PREGAME:
                this.newGame();
                this.tournamentState = TournamentState.INPROGRESS;
                this.timer = System.currentTimeMillis();
                break;
            case INPROGRESS:
                if (this.gameHandler.getStage().equals(GameStage.START) && timer < System.currentTimeMillis() - ServerProperties.AUTO_GAME_START){
                    this.gameHandler.launchGame();
                }
                break;
            case POSTGAME:
                if (roundCount + 1< rounds ){
                    this.tournamentState = TournamentState.FINISHED;
                    return;
                }
                if (timer > System.currentTimeMillis() + ServerProperties.TOURNAMENT_GAMEFINISH_TIME){
                    this.roundCount++;
                    this.tournamentState = TournamentState.PREGAME;
                }
                break;
            default:
                finishTournament();
        }
    }

    private void finishTournament(){
        OptionalInt winnerScore = score.values().stream().mapToInt(value -> value).max();
        Collection<Integer> winner;
        if (winnerScore.isPresent())
            winner = score.entrySet().stream().filter(entry -> entry.getValue() == winnerScore.getAsInt()).map(Map.Entry::getKey).collect(Collectors.toList());
        else
            winner = Lists.newArrayList();
        LOGGER.debug("Tournament {} has finished", tournamentId);
        this.clientManager.sendMessageToClients(NotificationBuilder.tournamentFinishNotification(winner), gameHandler.getAllClients());
    }

    @Nonnull
    public Map<Integer, Integer> getScore(){
        return score;
    }

    @Nonnull
    public String getName() {
        return name;
    }
}