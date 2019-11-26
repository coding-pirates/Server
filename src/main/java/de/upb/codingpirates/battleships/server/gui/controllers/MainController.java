package de.upb.codingpirates.battleships.server.gui.controllers;

import com.google.gson.Gson;
import de.upb.codingpirates.battleships.logic.Configuration;
import de.upb.codingpirates.battleships.logic.PenaltyType;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.ShipType;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.gui.control.Alerts;
import de.upb.codingpirates.battleships.server.network.ServerApplication;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * The controller associated with the {@code main.fxml} file.
 *
 * @author Andre Blanke
 */
public final class MainController extends AbstractController<BorderPane> {

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
    private ComboBox<ShipTypeConfiguration> shipTypeEditingComboBox;

    @FXML
    private Spinner<Integer> shipTypeHeightSpinner;

    @FXML
    private Spinner<Integer> shipTypeWidthSpinner;

    @FXML
    private GridPane shipConfigurationGrid;
    // </editor-fold>

    private final Gson gson;
    private final ServerApplication server;

    private static final Logger LOGGER = LogManager.getLogger();

    public MainController(@NotNull final Gson gson, ServerApplication server) {
        this.gson = gson;
        this.server = server;
    }

    private static final int LATIN_ALPHABET_LENGTH = 26;

    /**
     * Computes the unique label string for the n-th {@link ShipType}.
     *
     * A {@code ShipType} label consists of one or more letters of the latin alphabet.
     *
     * The label for {@code n + 1} is computed by either 'increasing' the last label letter by one if we have not yet
     * reached the end of the alphabet or by introduction of a new letter to the string.
     *
     * Examples:
     *
     * <pre>
     * toShipTypeLabel(0);  // "A"
     * toShipTypeLabel(25); // "Z"
     * toShipTypeLabel(26); // "AA"
     * </pre>
     *
     * @param n The index of the {@link ShipType} for which to compute the label.
     *
     * @return A unique label for the n-th {@link ShipType}.
     */
    @NotNull
    private String toShipTypeLabel(int n) {
        final StringBuilder shipTypeLabelBuilder = new StringBuilder();

        for (; n >= 0; n = (n / LATIN_ALPHABET_LENGTH) - 1) {
            final int remainder = n % LATIN_ALPHABET_LENGTH;

            shipTypeLabelBuilder.insert(0, (char) ('A' + remainder));
        }
        return shipTypeLabelBuilder.toString();
    }

    // <editor-fold desc="Initialization">
    private ShipTypeConfiguration getSelectedShipTypeConfiguration() {
        return shipTypeEditingComboBox.getSelectionModel().getSelectedItem();
    }

    private static final int SHIP_CONFIGURATION_GRID_CELL_SIZE = 30;

    private static final Color COLOR_MARKED   = Color.DARKGRAY;
    private static final Color COLOR_UNMARKED = Color.TEAL;

    private void populateShipConfigurationGrid(@NotNull final ShipTypeConfiguration config) {
        shipConfigurationGrid.getChildren().clear();

        for (int x = 0; x < config.width; ++x) {
            for (int y = 0; y < config.height; ++y) {
                final Rectangle rectangle =
                    new Rectangle(SHIP_CONFIGURATION_GRID_CELL_SIZE, SHIP_CONFIGURATION_GRID_CELL_SIZE);
                final Point2D coordinate = new Point2D(x, y);

                rectangle.setUserData(coordinate);
                rectangle.setOnMouseClicked(event -> {
                    //noinspection SuspiciousMethodCalls
                    if (config.marks.remove(rectangle.getUserData())) {
                        rectangle.setFill(COLOR_UNMARKED);
                    } else {
                        rectangle.setFill(COLOR_MARKED);
                        config.marks.add(coordinate);
                    }
                });

                if (config.marks.contains(coordinate))
                    rectangle.setFill(COLOR_MARKED);
                else
                    rectangle.setFill(COLOR_UNMARKED);

                shipConfigurationGrid.add(rectangle, x, y);
            }
        }
    }

    private void setupShipTypeConfigurationControls() {
        /* Initially populate the shipTypeEditingComboBox. */
        final List<ShipTypeConfiguration> initialConfigurations =
            IntStream
                .range(0, shipTypeCountSpinner.getValue())
                .boxed()
                .map(this::toShipTypeLabel)
                .map(ShipTypeConfiguration::new)
                .collect(toList());
        shipTypeEditingComboBox
                .getItems()
                .setAll(initialConfigurations);

        /* Ensure the shipTypeEditingComboBox is (at least) as wide as the shipTypeCountSpinner. */
        shipTypeEditingComboBox
            .minWidthProperty()
            .bind(shipTypeCountSpinner.widthProperty());

        shipTypeCountSpinner
            .valueProperty()
            .addListener(((observableValue, oldCount, newCount) -> {
                /*
                 * Avoid discarding all ShipTypeConfigurations and attempt to keep the first newCount configurations
                 * if the new amount of configurations is smaller than the old one.
                 */
                if (newCount < oldCount) {
                    final Collection<ShipTypeConfiguration> configurationsToKeep =
                        shipTypeEditingComboBox.getItems().subList(0, newCount);

                    shipTypeEditingComboBox.getItems().setAll(configurationsToKeep);
                /*
                 * Otherwise, if the new amount is larger than the old one we keep the current configurations and
                 * add newCount - oldCount new configurations to the existing ones.
                 */
                } else {
                    final Collection<ShipTypeConfiguration> newConfigurations =
                        IntStream
                            .range(oldCount, newCount)
                            .boxed()
                            .map(this::toShipTypeLabel)
                            .map(ShipTypeConfiguration::new)
                            .collect(toList());

                    shipTypeEditingComboBox
                        .getItems()
                        .addAll(newConfigurations);
                }
            }));

        /* Repopulate the shipTypeConfigurationGrid to display the newly selected ShipTypeConfiguration. */
        shipTypeEditingComboBox
            .getSelectionModel()
            .selectedItemProperty()
            .addListener((observable, oldSelection, newSelection) -> {
                populateShipConfigurationGrid(newSelection);

                shipTypeHeightSpinner.getValueFactory().setValue(newSelection.height);
                shipTypeWidthSpinner.getValueFactory().setValue(newSelection.width);
            });

        /* Select the first ShipTypeConfiguration. */
        shipTypeEditingComboBox
            .getSelectionModel()
            .select(0);

        /*
         * Adjust selected ShipTypeConfiguration and repopulate shipConfigurationGrid accordingly when changes to
         * the shipTypeHeightSpinner and shipTypeWidthSpinner occur.
         */

        shipTypeWidthSpinner
            .valueProperty()
            .addListener((observable, oldWidth, newWidth) -> {
                final ShipTypeConfiguration selected = getSelectedShipTypeConfiguration();

                selected.width = newWidth;
                selected.removeInvalidMarks();

                populateShipConfigurationGrid(selected);
            });
        shipTypeHeightSpinner
            .valueProperty()
            .addListener((observable, oldHeight, newHeight) -> {
                final ShipTypeConfiguration selected = getSelectedShipTypeConfiguration();

                selected.height = newHeight;
                selected.removeInvalidMarks();

                populateShipConfigurationGrid(selected);
            });
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

        configurationFileExtension   = resourceBundle.getString("configuration.fileExtension");
        configurationExtensionFilter = new ExtensionFilter(
            resourceBundle.getString("configuration.fileExtension.description"),
            resourceBundle.getString("configuration.fileExtension.glob")
        );

        setupShipTypeConfigurationControls();
        setupPenaltyMinusPointsControls();
    }
    // </editor-fold>

    // <editor-fold desc="Configuration import and export">
    /**
     * The file extension of configuration files.
     *
     * It needs to be appended to the {@link File} object returned by a {@link FileChooser} when exporting,
     * as {@link FileChooser#showOpenDialog(Window)} does not append the extension associated with the
     * {@link #configurationExtensionFilter}.
     *
     * @see #onExportButtonAction()
     */
    private String          configurationFileExtension;

    private ExtensionFilter configurationExtensionFilter;

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
        GameManager manager = server.getGameManager();
        manager.createGame(getConfigurationFromControls(),"TestGame",false);//TODO named game & tournament games
    }

    /**
     * An internal class used to represent the information associated with a {@link ShipType} configuration,
     * notably a {@link #label} to identify it, a collection of {@link Point2D} objects named {@link #marks}
     * to later represent the positions occupied by the {@code ShipType}, as well as {@link #width} and
     * {@link #height} for the amount of columns and rows of the {@link #shipConfigurationGrid}.
     *
     * A {@code ShipTypeConfiguration} may be converted to a {@code ShipType} via the {@link #toShipType()}
     * method.
     *
     * @author Andre Blanke
     */
    private static final class ShipTypeConfiguration {

        private int width  = DEFAULT_WIDTH_AND_HEIGHT;
        private int height = DEFAULT_WIDTH_AND_HEIGHT;

        private final Set<Point2D> marks = new HashSet<>();

        private final String label;

        private static final int DEFAULT_WIDTH_AND_HEIGHT = 5;

        @Contract(pure = true)
        private ShipTypeConfiguration(@NotNull final String label) {
            this.label = label;
        }

        /**
         * @inheritDoc
         *
         * @return The label of this {@code ShipType} when displayed inside of {@link #shipTypeEditingComboBox}.
         */
        @Override
        @Contract(pure = true)
        public String toString() {
            return label;
        }

        private void removeInvalidMarks() {
            marks.removeIf(point -> (point.getX() >= width) || (point.getY() >= height));
        }

        @NotNull
        @Contract(value = " -> new", pure = true)
        private ShipType toShipType() {
            return new ShipType(marks);
        }
    }
}
