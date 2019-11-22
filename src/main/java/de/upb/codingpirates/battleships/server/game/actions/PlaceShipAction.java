package de.upb.codingpirates.battleships.server.game.actions;

import de.upb.codingpirates.battleships.logic.PlacementInfo;
import de.upb.codingpirates.battleships.server.game.ActionType;

import java.util.Map;

public class PlaceShipAction extends Action {

    private Map<Integer, PlacementInfo> ships;

    public PlaceShipAction(int clientId, Map<Integer, PlacementInfo> ships) {
        super(ActionType.PLACESHIP, clientId);
        this.ships = ships;
    }

    public Map<Integer, PlacementInfo> getShips() {
        return ships;
    }
}
