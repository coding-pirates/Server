package de.upb.codingpirates.battleships.server.exceptions;

import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;

public class GameFullExeption extends InvalidActionException {
    public GameFullExeption() {
        super("game.isFull");
    }
}
