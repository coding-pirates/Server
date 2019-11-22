package de.upb.codingpirates.battleships.server.gui.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.IntStream;

import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import com.google.gson.Gson;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import de.upb.codingpirates.battleships.logic.util.Configuration;
import de.upb.codingpirates.battleships.logic.util.PenaltyType;
import de.upb.codingpirates.battleships.server.gui.control.Alerts;

import static java.util.stream.Collectors.toList;

/**
 * @author Andre Blanke
 */
public final class MainController extends AbstractController<BorderPane> {

    /**
     * The file extension of configuration files.
     *
     * It needs to be appended to the {@link File} object returned by a {@link FileChooser}.
     * 
     * @see #onImportButtonAction()
     * @see #onExportButtonAction()
     */
    private String          configurationFileExtension;

    private ExtensionFilter configurationExtensionFilter;

    // <editor-fold desc="Configuration controls">
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
    private ComboBox<PenaltyType> penaltyTypeComboBox;

    @FXML
    private Label penaltyMinusPointsLabel;

    @FXML
    private Spinner<Integer> penaltyMinusPointsSpinner;

    @FXML
    private Spinner<Integer> shipTypeCountSpinner;

    @FXML
    private ComboBox<String> shipTypeEditingComboBox;

    @FXML
    private GridPane shipConfigurationContainer;
    // </editor-fold>

    @FXML
    private Collection<Spinner<? extends Number>> spinners;

    private final Gson gson;

    /**
     * The default amount of {@link Rectangle}s contained within each row and column of the
     * {@link #shipConfigurationContainer}.
     *
     * @see #setupShipConfigurationContainer()
     */
    private static final int DEFAULT_SHIP_CONFIGURATION_GRID_SIZE = 5;

    private static final Logger LOGGER = LogManager.getLogger();

    public MainController(@NotNull final Gson gson) {
        this.gson = gson;
    }

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

    private static final int LATIN_ALPHABET_LENGTH = 26;
    @NotNull
    private String toShipTypeLabel(int n) {
        final StringBuilder shipTypeLabelBuilder = new StringBuilder();

        for (; n >= 0; n = (n / LATIN_ALPHABET_LENGTH) - 1) {
            int rem = n % LATIN_ALPHABET_LENGTH;

            shipTypeLabelBuilder.insert(0, (char) ('A' + rem));
        }
        return shipTypeLabelBuilder.toString();
    }

    private void populateShipTypeEditingComboBox() {
        final List<String> shipTypeLabels =
                IntStream
                    .range(0, shipTypeCountSpinner.getValue())
                    .boxed()
                    .map(this::toShipTypeLabel)
                    .collect(toList());

        shipTypeEditingComboBox
                .getItems()
                .setAll(shipTypeLabels);
    }

    private void setupShipConfigurationContainer() {
        populateShipTypeEditingComboBox();

        shipTypeEditingComboBox
            .getSelectionModel()
            .select(0);
        shipTypeEditingComboBox
            .minWidthProperty()
            .bind(shipTypeCountSpinner.widthProperty());
        shipTypeCountSpinner
            .valueProperty()
            .addListener(((observableValue, oldValue, newValue) -> populateShipTypeEditingComboBox()));

        for (int x = 0; x < DEFAULT_SHIP_CONFIGURATION_GRID_SIZE; ++x) {
            for (int y = 0; y < DEFAULT_SHIP_CONFIGURATION_GRID_SIZE; ++y) {
                final Rectangle rectangle = new Rectangle(30, 30);

                rectangle.setFill(Color.RED);

                shipConfigurationContainer.add(rectangle, x, y);
            }
        }
    }

    /**
     * Setup bindings on the {@link Node#disableProperty()} of both {@link #penaltyMinusPointsLabel}
     * and {@link #penaltyMinusPointsSpinner} to disable them as long as {@link PenaltyType#POINTLOSS}
     * is not selected inside of {@link #penaltyTypeComboBox}.
     */
    private void setupPenaltyMinusPointsControls() {
        final ObservableValue<Boolean> isPenaltyKindNotPointloss =
                penaltyTypeComboBox
                        .getSelectionModel()
                        .selectedItemProperty()
                        .isNotEqualTo(PenaltyType.POINTLOSS);

        penaltyTypeComboBox
            .getItems()
            .setAll(Arrays.asList(PenaltyType.values()));
        penaltyTypeComboBox
            .getSelectionModel()
            .select(PenaltyType.POINTLOSS);
        penaltyTypeComboBox
            .minWidthProperty()
            .bind(penaltyMinusPointsSpinner.widthProperty());

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

        setupShipConfigurationContainer();
        setupPenaltyMinusPointsControls();
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

    /**
     * Creates a new {@link Configuration} object from the JavaFX controls associated with the
     * configuration properties.
     *
     * The counterpart of this method is {@link #setControlsFromConfiguration(Configuration)}
     * which sets the JavaFX controls to the appropriate values for each configuration property.
     *
     * @return A new {@link Configuration} based on the current state of the GUI controls used
     *         for configuring it.
     *
     * @see #setControlsFromConfiguration(Configuration)
     */
    @NotNull
    @Contract(" -> new")
    private Configuration getConfigurationFromControls() {
        return new Configuration(
            maxPlayerCountSpinner.getValue(),
            heightSpinner.getValue(),
            widthSpinner.getValue(),
            shotCountSpinner.getValue(),
            hitPointsSpinner.getValue(),
            sunkPointsSpinner.getValue(),
            roundTimeSpinner.getValue(),
            visualizationTimeSpinner.getValue(),
            new HashMap<>(),
            penaltyMinusPointsSpinner.getValue(),
            penaltyTypeComboBox.getSelectionModel().getSelectedItem()
        );
    }

    /**
     * @see #getConfigurationFromControls()
     */
    @FXML
    @SuppressWarnings("unused")
    private void onExportButtonAction() {
        final FileChooser chooser =
            newConfigurationFileChooser(resourceBundle.getString("configuration.export.dialog.title"));

        final File target = chooser.showSaveDialog(root.getScene().getWindow());
        if (target == null)
            return;

        final Path exportPath = target.toPath().resolveSibling(target.getName() + configurationFileExtension);

        try {
            final String configurationJson = gson.toJson(getConfigurationFromControls());

            LOGGER.info("Exporting configuration to '{}.", exportPath);

            Files.write(exportPath, configurationJson.getBytes(StandardCharsets.UTF_8));
        } catch (final IOException exception) {
            final String contentText =
                String.format(resourceBundle.getString("configuration.export.exceptionAlert.contentText"), exportPath);

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

    /**
     * Sets the JavaFX controls associated with configuration properties to the appropriate values
     * based on the provided {@code configuration}.
     *
     * The counterpart of this method is {@link #getConfigurationFromControls()} which creates a
     * new {@link Configuration} object based on the state of the associated JavaFX controls.
     *
     * @param configuration The {@link Configuration} whose properties should be used to determine
     *                      the new state of the GUI controls.
     *
     * @see #getConfigurationFromControls()
     */
    private void setControlsFromConfiguration(@NotNull final Configuration configuration) {
        maxPlayerCountSpinner
            .getValueFactory()
            .setValue(configuration.getMaxPlayerCount());
        heightSpinner
            .getValueFactory()
            .setValue(configuration.getHeight());
        widthSpinner
            .getValueFactory()
            .setValue(configuration.getWidth());
        shotCountSpinner
            .getValueFactory()
            .setValue(configuration.getShotCount());
        hitPointsSpinner
            .getValueFactory()
            .setValue(configuration.getHitPoints());
        sunkPointsSpinner
            .getValueFactory()
            .setValue(configuration.getSunkPoints());
        roundTimeSpinner
            .getValueFactory()
            .setValue(configuration.getRoundTime());
        visualizationTimeSpinner
            .getValueFactory()
            .setValue(configuration.getVisualizationTime());
        penaltyMinusPointsSpinner
            .getValueFactory()
            .setValue(configuration.getPenaltyMinusPoints());

        penaltyTypeComboBox
            .getSelectionModel()
            .select(configuration.getPenaltyKind());
    }

    /**
     * @see #setControlsFromConfiguration(Configuration) 
     */
    @FXML
    @SuppressWarnings("unused")
    private void onImportButtonAction() {
        final FileChooser chooser =
            newConfigurationFileChooser(resourceBundle.getString("configuration.import.dialog.title"));

        final File target = chooser.showOpenDialog(root.getScene().getWindow());
        if (target == null)
            return;

        try {
            LOGGER.info("Importing configuration from '{}'.", target);

            final String configurationJson = new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8);

            setControlsFromConfiguration(gson.fromJson(configurationJson, Configuration.class));
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
    // </editor-fold>

    @FXML
    @SuppressWarnings("unused")
    private void onStartNewGameButtonAction() {
    }
}
