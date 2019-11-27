package de.upb.codingpirates.battleships.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import de.upb.codingpirates.battleships.logic.*;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.id.IntIdManager;
import de.upb.codingpirates.battleships.network.message.notification.ContinueNotification;
import de.upb.codingpirates.battleships.network.message.notification.PauseNotification;
import de.upb.codingpirates.battleships.server.game.GameHandler;
import de.upb.codingpirates.battleships.server.network.ServerApplication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * handles all game related tasks
 * <p>
 * get an instance with {@link ServerApplication#getGameManager()}
 */
public class GameManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private final ClientManager clientManager;
    private final IntIdManager idManager;

    /**
     * maps game id to gamehandler
     */
    private final Map<Integer, GameHandler> games = Maps.newHashMap();
    /**
     * maps client id to gameid
     */
    private final Map<Integer, Integer> clientToGame = Maps.newHashMap();

    @Inject
    public GameManager(ConnectionHandler handler, IntIdManager idManager) {
        clientManager = (ClientManager)handler;
        this.idManager = idManager;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                GameManager.this.run();
            }
        },1L,1L);
    }

    /**
     * creates game based on parameter
     * @param configuration
     * @param name
     * @param tournament
     * @return {@code -1} if game was created successful, {@code > 0} if the selected field size of the Configuration is too small
     */
    public int createGame(@Nonnull Configuration configuration, @Nonnull String name, boolean tournament) {
        LOGGER.debug("Create game: {}", name);
        int size = checkField(configuration);
        if(size != -1){
            return size;
        }
        int id = this.idManager.generate().getInt();
        games.putIfAbsent(id, new GameHandler(name, id, configuration, tournament, clientManager));
        return -1;
    }

    /**
     * adds client with clientType to the specific game
     * @param gameId
     * @param client
     * @param clientType
     * @throws InvalidActionException if game does not exist
     */
    public void addClientToGame(int gameId, Client client, ClientType clientType) throws InvalidActionException {
        LOGGER.debug("Adding client {}, with type {}, to game {}", client.getId(), clientType,gameId);
        if (games.containsKey(gameId)) {
            games.get(gameId).addClient(clientType, client);
            clientToGame.putIfAbsent(client.getId(), gameId);
        } else {
            LOGGER.error("Can't find game {}", gameId);
            throw new InvalidActionException("game.gameManager.noGame");
        }
    }

    /**
     * removes client from participating games
     * @param client
     * @throws InvalidActionException if client does not participate
     */
    public void removeClientFromGame(int client) throws InvalidActionException {
        LOGGER.debug("Remove client {} from active game", client);
        if (clientToGame.containsKey(client)) {
            games.get(clientToGame.remove(client)).removeClient(client);
        } else {
            LOGGER.warn("Client {} does not participate in a game", client);
            throw new InvalidActionException("game.gameManager.noGameForClient");
        }
    }

    /**
     * launches game with id
     * @param gameId
     * @return {@code false} if player count is under 2
     */
    public boolean launchGame(int gameId) {
        LOGGER.debug("launched game {}, {}", gameId, games.get(gameId).getGame().getName());
        return games.get(gameId).launchGame();
    }

    /**
     * pauses game with id
     * @param gameId
     */
    public void pauseGame(int gameId) {
        LOGGER.debug("paused game {}, {}", gameId, games.get(gameId).getGame().getName());
        games.get(gameId).pauseGame();
        clientManager.sendMessageToClients(new PauseNotification(), games.get(gameId).getAllClients());
    }

    /**
     * continue game with id
     * @param gameId
     */
    public void continueGame(int gameId) {
        LOGGER.debug("continued game {}, {}", gameId, games.get(gameId).getGame().getName());
        games.get(gameId).continueGame();
        clientManager.sendMessageToClients(new ContinueNotification(), games.get(gameId).getAllClients());
    }

    /**
     * @return all existing games
     */
    public Collection<GameHandler> getAllGames() {
        return games.values();
    }

    /**
     * @param clientId for which a game should be found
     * @return a game where the client participate
     * @throws InvalidActionException if the client does not participate in a game
     */
    public GameHandler getGameHandlerForClientId(int clientId) throws InvalidActionException {
        if (!clientToGame.containsKey(clientId)) {
            LOGGER.warn("Could not get game for client {}",clientId);
            throw new InvalidActionException("game.gameManager.noGameForClient");
        }
        return games.get(clientToGame.get(clientId));
    }

    /**
     * @param id id of the game
     * @return the game with the id
     * @throws InvalidActionException if the game does not exist
     */
    @Nonnull
    public GameHandler getGame(int id) throws InvalidActionException {
        if(!games.containsKey(id)){
            LOGGER.warn("The game with id: {} does not exist",id);
            throw new InvalidActionException("game.gameManager.gameNotExist");
        }
        return games.get(id);
    }

    /**
     * run method for every game
     */
    private void run() {
        games.values().forEach(GameHandler::run);
    }

    /**
     * checks if the ships can fit into the field
     * @param configuration the configuration which should be checked
     * @return {@code -1} if it fits, else recommendation for a field size;
     */
    private int checkField(@Nonnull Configuration configuration){//TODO better algorithm
        Collection<ShipType> ships = configuration.getShips().values();

        List<BoundingBox> boxes = Lists.newArrayList();

        for (ShipType ship :ships){
            int x = ship.getPositions().stream().max((a,b)-> Math.max(Math.abs(a.getX()), Math.abs(b.getX()))).get().getX();
            int y = ship.getPositions().stream().max((a,b)-> Math.max(Math.abs(a.getY()), Math.abs(b.getY()))).get().getY();
            boxes.add(new BoundingBox(x +1,y+1));
        }

        int maxFields = boxes.stream().mapToInt(BoundingBox::getSize).sum();
        if(maxFields > configuration.getHeight()*configuration.getWidth()){
            return (int)Math.sqrt(maxFields);
        }
        return -1;
    }
}
