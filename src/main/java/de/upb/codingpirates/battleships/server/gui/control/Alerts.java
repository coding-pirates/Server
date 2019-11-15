package de.upb.codingpirates.battleships.server.gui.control;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;

import com.google.common.base.Throwables;

import org.jetbrains.annotations.NotNull;

public final class Alerts {

    private Alerts() {
    }

    @NotNull
    public static Alert exceptionAlert(
            final String title,
            final String headerText,
            final String contentText,
            final String labelText,
            final Throwable throwable) {
        final Alert    alert    = new Alert(AlertType.ERROR);

        final GridPane root     = new GridPane();
        final Label    label    = new Label();
        final TextArea textArea = new TextArea(Throwables.getStackTraceAsString(throwable));

        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);

        root.addRow(0, label);
        root.addRow(1, textArea);

        label.setText(labelText);

        textArea.setEditable(false);
        textArea.setWrapText(true);

        return alert;
    }
}
