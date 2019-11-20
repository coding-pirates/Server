package de.upb.codingpirates.battleships.server.gui.event;

import javafx.event.ActionEvent;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.stage.Window;

import org.jetbrains.annotations.NotNull;

/**
 * @author Andre Blanke
 */
public final class EventUtils {

    private EventUtils() {
    }

    /**
     * Retrieves the {@link Window} associated with the {@code event}'s {@link EventTarget},
     * provided the target is of tye {@link Node}, otherwise throws an
     * {@link UnsupportedOperationException}.
     *
     * @param event The event whose associated {@link Window} is to be retrieved.
     *
     * @return The {@link Window} associated with the {@code event}'s {@link EventTarget}.
     *
     * @throws UnsupportedOperationException If the {@link EventTarget} is not a {@link Node}.
     */
    public static Window getWindowFromTarget(@NotNull final ActionEvent event) {
        final EventTarget target = event.getTarget();

        if (target instanceof Node)
            return ((Node) event.getTarget()).getScene().getWindow();
        throw new UnsupportedOperationException("EventTarget not a Node.");
    }
}
