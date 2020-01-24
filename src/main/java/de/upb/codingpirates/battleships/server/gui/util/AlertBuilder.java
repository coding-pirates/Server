package de.upb.codingpirates.battleships.server.gui.util;

import javax.annotation.Nonnull;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.util.Builder;

import com.google.common.base.Throwables;

import org.jetbrains.annotations.Contract;

/** @author Andre Blanke */
public class AlertBuilder implements Builder<Alert> {

    private String title = "";

    private String headerText = "";

    private String contentText = "";

    private AlertType alertType;

    private ButtonType[] buttonTypes = new ButtonType[0];

    private Node dialogPaneContent;

    private AlertBuilder() {
    }

    public static @Nonnull
    AlertBuilder of(@Nonnull final AlertType alertType) {
        return new AlertBuilder().alertType(alertType);
    }

    public static @Nonnull
    AlertBuilder ofThrowable(@Nonnull final Throwable throwable,
                             @Nonnull final String stacktraceLabelText) {
        return new ExceptionAlertBuilder()
                .throwable(throwable)
                .stacktraceLabelText(stacktraceLabelText)
                .alertType(AlertType.ERROR)
                .buttonTypes(ButtonType.OK);
    }

    @Override
    @Contract(pure = true)
    public @Nonnull
    Alert build() {
        final Alert alert = new Alert(alertType, contentText, buttonTypes);

        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.getDialogPane().setContent(dialogPaneContent);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        return alert;
    }

    public @Nonnull
    AlertBuilder title(@Nonnull final String title) {
        this.title = title;
        return this;
    }

    public @Nonnull
    AlertBuilder headerText(@Nonnull final String headerText) {
        this.headerText = headerText;
        return this;
    }

    public @Nonnull
    AlertBuilder contentText(@Nonnull final String contentText) {
        this.contentText = contentText;
        return this;
    }

    public @Nonnull
    AlertBuilder alertType(@Nonnull final AlertType alertType) {
        this.alertType = alertType;
        return this;
    }

    public @Nonnull
    AlertBuilder buttonTypes(@Nonnull final ButtonType... buttonTypes) {
        this.buttonTypes = buttonTypes;
        return this;
    }

    public @Nonnull AlertBuilder dialogPaneContent(@Nonnull final Node dialogPaneContent) {
        this.dialogPaneContent = dialogPaneContent;
        return this;
    }

    private static final class ExceptionAlertBuilder extends AlertBuilder {

        private Throwable throwable;

        private String stacktraceLabelText;

        @Override
        public @Nonnull
        Alert build() {
            final Alert alert = super.build();

            final GridPane root = new GridPane();
            final Label label = new Label();
            final TextArea textArea = new TextArea(Throwables.getStackTraceAsString(throwable));

            root.addRow(0, label);
            root.addRow(1, textArea);

            label.setText(stacktraceLabelText);

            textArea.setEditable(false);
            textArea.setWrapText(true);

            alert.getDialogPane().setExpandableContent(root);

            return alert;
        }

        private @Nonnull
        ExceptionAlertBuilder stacktraceLabelText(@Nonnull final String stacktraceLabelText) {
            this.stacktraceLabelText = stacktraceLabelText;
            return this;
        }

        private @Nonnull
        ExceptionAlertBuilder throwable(@Nonnull final Throwable throwable) {
            this.throwable = throwable;
            return this;
        }
    }
}
