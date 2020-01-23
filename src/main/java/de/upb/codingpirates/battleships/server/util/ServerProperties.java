package de.upb.codingpirates.battleships.server.util;

public class ServerProperties {

    /**
     * The time a finished {@link de.upb.codingpirates.battleships.logic.Game} should be available
     * before it will be removed
     *
     * @see de.upb.codingpirates.battleships.server.GameManager#removeGames()
     */
    public static final long MAX_FINISHED_GAME_EXIST_TIME = 120000;

    /**
     * The minimum amount of {@link de.upb.codingpirates.battleships.logic.Client}s with {@link de.upb.codingpirates.battleships.logic.ClientType#PLAYER} required in order to launch a game using
     * the {@link de.upb.codingpirates.battleships.server.game.GameHandler#launchGame()} method.
     *
     * @see de.upb.codingpirates.battleships.server.game.GameHandler#launchGame()
     */
    public static final int MIN_PLAYER_COUNT = 2;

    /**
     * The maximum amount of {@link de.upb.codingpirates.battleships.logic.Client}s with {@link de.upb.codingpirates.battleships.logic.ClientType#SPECTATOR} which can spectate a {@link de.upb.codingpirates.battleships.logic.Game}.
     *
     * @see de.upb.codingpirates.battleships.server.game.GameHandler#addClient(de.upb.codingpirates.battleships.logic.ClientType, de.upb.codingpirates.battleships.logic.Client)
     */
    public static final int MAX_SPECTATOR_COUNT = Integer.MAX_VALUE;

    public static final long TOURNAMENT_GAMEFINISH_TIME = 10000;

    public static final long AUTO_GAME_START = 20000;
}
