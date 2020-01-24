package de.upb.codingpirates.battleships.server.exceptions;

import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;

/**
 * should be thrown when client should be added to a gabe, but it is full
 */
public class GameFullExeption extends InvalidActionException {
    public GameFullExeption() {
        super("game.isFull");
    }
}
