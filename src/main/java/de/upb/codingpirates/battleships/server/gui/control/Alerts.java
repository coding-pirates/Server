package de.upb.codingpirates.battleships.server.gui.control;

import javax.annotation.Nonnull;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;

import com.google.common.base.Throwables;

/**
 * A utility class containing static methods for creating ready-to-show JavaFX {@link Alert}s.
 *
 * @author Andre Blanke
 */
public final class Alerts {

    /* Prevent instantiation. */
    private Alerts() {
    }

    @Nonnull
    public static Alert alert(
            final String        title,
            final String        headerText,
            final String        contentText,
            final AlertType     alertType,
            final ButtonType... buttonTypes) {
        final Alert alert = new Alert(alertType, contentText, buttonTypes);

        alert.setTitle(title);
        alert.setHeaderText(headerText);

        return alert;
    }

    @Nonnull
    public static Alert exceptionAlert(
            final String title,
            final String headerText,
            final String contentText,
            final String labelText,
            final Throwable throwable) {
        final Alert alert = alert(title, headerText, contentText, AlertType.ERROR, ButtonType.OK);

        final GridPane root     = new GridPane();
        final Label    label    = new Label();
        final TextArea textArea = new TextArea(Throwables.getStackTraceAsString(throwable));

        root.addRow(0, label);
        root.addRow(1, textArea);

        label.setText(labelText);

        textArea.setEditable(false);
        textArea.setWrapText(true);

        return alert;
    }
}
