package de.upb.codingpirates.battleships.server.exceptions;

public class InvalidGameSizeException extends Exception {

    private final int minSize;
    private final int actualGameSize;

    public InvalidGameSizeException(int actualGameSize, int minSize) {
        this.minSize = minSize;
        this.actualGameSize = actualGameSize;
    }

    public int getMinSize() {
        return minSize;
    }

    public int getActualGameSize() {
        return actualGameSize;
    }
}
