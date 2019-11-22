package de.upb.codingpirates.battleships.server.game.actions;

import de.upb.codingpirates.battleships.server.game.ActionType;

public class Action {

    private ActionType type;
    private int sourceClient;

    public Action(ActionType type, int sourceClient) {
        this.type = type;
        this.sourceClient = sourceClient;
    }

    public ActionType getType() {
        return type;
    }

    public int getSourceClient() {
        return sourceClient;
    }
}
