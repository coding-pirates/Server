package de.upb.codingpirates.battleships.server.game.actions;

import de.upb.codingpirates.battleships.logic.Shot;
import de.upb.codingpirates.battleships.server.game.ActionType;

import java.util.Collection;

public class ShotsAction extends Action {
    private Collection<Shot> shots;

    public ShotsAction(int clientId, Collection<Shot> shots) {
        super(ActionType.SHOTS, clientId);
        this.shots = shots;
    }

    public Collection<Shot> getShots() {
        return shots;
    }
}
