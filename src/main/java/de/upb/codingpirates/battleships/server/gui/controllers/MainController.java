package de.upb.codingpirates.battleships.server.gui.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.ResourceBundle;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import de.upb.codingpirates.battleships.server.gui.control.Alerts;
import de.upb.codingpirates.battleships.server.gui.event.EventUtils;

/**
 * @author Andre Blanke
 */
public final class MainController extends AbstractController<BorderPane> {

    /**
     * The file extension of configuration files.
     *
     * It needs to be appended to the {@link File} object returned by a {@link FileChooser}.
     * 
     * @see #onImportButtonAction(ActionEvent)
     * @see #onExportButtonAction(ActionEvent) 
     */
    private String          configurationFileExtension;

    private ExtensionFilter configurationExtensionFilter;

    // <editor-fold desc="Configuration">
    @FXML
    private Spinner<Integer> maxPlayerCountSpinner;

    @FXML
    private Spinner<Integer> shotCountSpinner;

    @FXML
    private Spinner<Integer> hitPointsSpinner;

    @FXML
    private Spinner<Integer> sunkPointsSpinner;

    @FXML
    private Spinner<Long> roundTimeSpinner;

    @FXML
    private Spinner<Long> visualizationTimeSpinner;

    @FXML
    private Spinner<Integer> heightSpinner;

    @FXML
    private Spinner<Integer> widthSpinner;

    @FXML
    private ComboBox<Object> penaltyKindComboBox;

    @FXML
    private Label penaltyMinusPointsLabel;

    @FXML
    private Spinner<Integer> penaltyMinusPointsSpinner;
    // </editor-fold>

    @FXML
    private Collection<Spinner<? extends Number>> spinners;

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Initializes all {@link Spinner}s which are part of the {@link #spinners} collection as
     * follows:
     */
    @SuppressWarnings("unchecked")
    private void initializeSpinners() {
        for (final Spinner<? extends Number> spinner : spinners) {
            final SpinnerValueFactory<? extends Number> factory = spinner.getValueFactory();

            /*
             * We don't know the concrete type parameter of TextFormatter because the spinner
             * collection contains both Spinners of type Integer and of type Long.
             */
            final TextFormatter formatter = new TextFormatter(factory.getConverter(), factory.getValue());

            spinner
                .getEditor()
                .setTextFormatter(formatter);

            factory.valueProperty().bindBidirectional(formatter.valueProperty());
        }
    }

    /**
     * Setup bindings on the {@link Node#disableProperty()} of both {@link #penaltyMinusPointsLabel}
     * and {@link #penaltyMinusPointsSpinner} to disable them as long as {@link Object} is not selected
     * inside of {@link #penaltyKindComboBox}.
     */
    private void setupPenaltyMinusPointsBindings() {
        final ObservableValue<Boolean> isPenaltyKindNotPointloss =
                penaltyKindComboBox
                        .getSelectionModel()
                        .selectedItemProperty()
                        .isNotEqualTo(new Object());

        penaltyMinusPointsLabel
                .disableProperty()
                .bind(isPenaltyKindNotPointloss);
        penaltyMinusPointsSpinner
                .disableProperty()
                .bind(isPenaltyKindNotPointloss);
    }

    @Override
    public void initialize(final URL url, final ResourceBundle resourceBundle) {
        super.initialize(url, resourceBundle);

        configurationFileExtension = resourceBundle.getString("configuration.fileExtension");

        configurationExtensionFilter = new ExtensionFilter(
            resourceBundle.getString("configuration.fileExtension.description"),
            resourceBundle.getString("configuration.fileExtension.glob")
        );
        initializeSpinners();

        setupPenaltyMinusPointsBindings();
    }

    // <editor-fold desc="Configuration import and export">
    @NotNull
    @Contract(pure = true)
    private FileChooser newConfigurationFileChooser(final String title) {
        final FileChooser fileChooser = new FileChooser();

        fileChooser.getExtensionFilters().add(configurationExtensionFilter);
        fileChooser.setTitle(title);

        return fileChooser;
    }

    @FXML
    private void onExportButtonAction(@NotNull final ActionEvent event) {
        final FileChooser chooser =
            newConfigurationFileChooser(resourceBundle.getString("configuration.export.dialog.title"));

        final File target = chooser.showSaveDialog(EventUtils.getWindowFromTarget(event));
        if (target != null) {
            final Path exportPath = target.toPath().resolveSibling(configurationFileExtension);

            try {
                LOGGER.info("Exporting configuration to '{}.", target);

                Files.write(exportPath, "".getBytes(StandardCharsets.UTF_8));
            } catch (final IOException exception) {
                final String contentText =
                    String.format(resourceBundle.getString("configuration.export.exceptionAlert.contentText"), target);

                LOGGER.error(exception);

                Alerts
                    .exceptionAlert(
                        resourceBundle.getString("configuration.export.exceptionAlert.title"),
                        resourceBundle.getString("configuration.export.exceptionAlert.headerText"),
                        contentText,
                        resourceBundle.getString("configuration.export.exceptionAlert.labelText"),
                        exception)
                    .showAndWait();
            }
        }
    }

    @FXML
    private void onImportButtonAction(@NotNull final ActionEvent event) {
        final FileChooser chooser =
            newConfigurationFileChooser(resourceBundle.getString("configuration.import.dialog.title"));

        final File target = chooser.showOpenDialog(EventUtils.getWindowFromTarget(event));
        if (target != null) {
            final Path importPath = target.toPath().resolveSibling(configurationFileExtension);

            try {
                LOGGER.info("Importing configuration from '{}'.", target);

                new String(Files.readAllBytes(importPath), StandardCharsets.UTF_8);
            } catch (final IOException exception) {
                final String contentText =
                    String.format(resourceBundle.getString("configuration.import.exceptionAlert.contentText"), target);

                LOGGER.error(exception);

                Alerts
                    .exceptionAlert(
                        resourceBundle.getString("configuration.import.exceptionAlert.title"),
                        resourceBundle.getString("configuration.import.exceptionAlert.headerText"),
                        contentText,
                        resourceBundle.getString("configuration.import.exceptionAlert.labelText"),
                        exception)
                    .showAndWait();
            }
        }
    }
    // </editor-fold>

    @FXML
    private void onStartNewGameButtonAction() {
    }
}
