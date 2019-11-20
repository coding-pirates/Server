package de.upb.codingpirates.battleships.server.gui.event;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Window;

import org.jetbrains.annotations.NotNull;

/**
 * @author Andre Blanke
 */
public final class EventUtils {

    private EventUtils() {
    }

    public static Window getWindowFromTarget(@NotNull final ActionEvent event) {
        return ((Node) event.getTarget()).getScene().getWindow();
    }
}
