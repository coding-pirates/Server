package de.upb.codingpirates.battleships.server.gui.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;

import com.google.common.annotations.VisibleForTesting;

import com.google.gson.Gson;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jetbrains.annotations.Contract;

import de.upb.codingpirates.battleships.logic.*;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.gui.controllers.ConfigurationValidator.InsufficientFieldSizeException;
import de.upb.codingpirates.battleships.server.gui.util.AlertBuilder;

import static java.util.stream.Collectors.*;

/**
 * The controller associated with the {@code configuration.fxml} file which allows the creation of {@link Configuration}
 * objects and starting of configured games.
 *
 * @author Andre Blanke
 */
public final class ConfigurationController extends AbstractController<Parent> {

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
    private Spinner<Integer> roundTimeSpinner;

    @FXML
    private Spinner<Integer> visualizationTimeSpinner;

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

    @FXML
    private TextField gameNameTextField;

    private final Gson gson;

    private final GameManager gameManager;

    private final ConfigurationValidator validator;

    private static final Logger LOGGER = LogManager.getLogger();

    @Inject
    private ConfigurationController(
            @Nonnull final Gson gson,
            @Nonnull final GameManager gameManager,
            @Nonnull final ConfigurationValidator validator) {
        this.gson        = gson;
        this.gameManager = gameManager;
        this.validator   = validator;
    }

    // <editor-fold desc="toShipTypeLabel">
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
    @Nonnull
    @VisibleForTesting
    static String toShipTypeLabel(int n) {
        final StringBuilder shipTypeLabelBuilder = new StringBuilder();

        for (; n >= 0; n = (n / LATIN_ALPHABET_LENGTH) - 1) {
            final int remainder = n % LATIN_ALPHABET_LENGTH;

            shipTypeLabelBuilder.insert(0, (char) ('A' + remainder));
        }
        return shipTypeLabelBuilder.toString();
    }
    // </editor-fold>

    // <editor-fold desc="Initialization">
    private ShipTypeConfiguration getSelectedShipTypeConfiguration() {
        return shipTypeEditingComboBox.getSelectionModel().getSelectedItem();
    }

    private static final int SHIP_CONFIGURATION_GRID_CELL_SIZE = 30;

    private static final Color COLOR_MARKED   = Color.DARKGRAY;
    private static final Color COLOR_UNMARKED = Color.TEAL;

    private void populateShipConfigurationGrid(@Nonnull final ShipTypeConfiguration config) {
        shipConfigurationGrid.getChildren().clear();

        for (int x = 0; x < config.width; ++x) {
            for (int y = 0; y < config.height; ++y) {
                final Rectangle rectangle =
                    new Rectangle(SHIP_CONFIGURATION_GRID_CELL_SIZE, SHIP_CONFIGURATION_GRID_CELL_SIZE);
                final Point2D coordinate = new Point2D(x, y);

                rectangle.setUserData(coordinate);
                rectangle.getStyleClass().add("ship-configuration-cell");
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
                .map(ConfigurationController::toShipTypeLabel)
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
                    /*
                     * We cannot directly use the result of the subList(0, newCount) call, as the subList method
                     * returns a view of the original list rather than a new one, which would result in a
                     * ConcurrentModificationException in the next line when invoking setAll(configurationsToKeep);
                     */
                    final Collection<ShipTypeConfiguration> configurationsToKeep =
                        new ArrayList<>(shipTypeEditingComboBox.getItems().subList(0, newCount));

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
                            .map(ConfigurationController::toShipTypeLabel)
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
                /*
                 * newSelection might be null if we decrease the value of the shipTypeCountSpinner and the selected
                 * ShipTypeConfiguration is one of the ones to be removed from the ComboBox.
                 *
                 * In that case select the last possible entry which is thus closest to the one that was previously
                 * selected.
                 */
                if (newSelection == null) {
                    final ShipTypeConfiguration last =
                        shipTypeEditingComboBox
                            .getItems()
                            .get(shipTypeEditingComboBox.getItems().size() - 1);

                    shipTypeEditingComboBox
                        .getSelectionModel()
                        .select(last);
                    return;
                }
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

        //setupSpinners();
        setupShipTypeConfigurationControls();
        setupPenaltyMinusPointsControls();
    }
    // </editor-fold>

    private void displayInvalidShipTypeConfigurationAlert(@Nonnull final ShipTypeConfiguration invalidConfiguration) {
        final Alert alert = new Alert(AlertType.ERROR);

        alert.setTitle(
            resourceBundle.getString("configuration.ships.invalidConfigurationAlert.title"));
        alert.setHeaderText(
            resourceBundle.getString("configuration.ships.invalidConfigurationAlert.headerText"));

        if (!invalidConfiguration.hasMinimumSize()) {
            alert.setContentText(String.format(
                resourceBundle.getString("configuration.ships.invalidConfigurationAlert.contentText.sizeInsufficient"),
                invalidConfiguration.label,
                ShipTypeConfiguration.MINIMUM_SHIP_TYPE_SIZE,
                invalidConfiguration.marks.size()
            ));
        } else {
            alert.setContentText(String.format(
                resourceBundle.getString("configuration.ships.invalidConfigurationAlert.contentText.notConnected"),
                invalidConfiguration.label
            ));
        }
        alert.showAndWait();
    }

    private void displayInsufficientFieldSizeAlert(final int recommendedSize) {
        final Alert alert = new Alert(AlertType.WARNING);

        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        alert.setTitle(
            resourceBundle.getString("configuration.insufficientFieldSizeAlert.title"));
        alert.setHeaderText(
            resourceBundle.getString("configuration.insufficientFieldSizeAlert.headerText"));
        alert.setContentText(
            String.format(
                resourceBundle.getString("configuration.insufficientFieldSizeAlert.contentText"),
                recommendedSize));

        alert.showAndWait().ifPresent(result -> {
            widthSpinner
                .getValueFactory()
                .setValue(recommendedSize);
            heightSpinner
                .getValueFactory()
                .setValue(recommendedSize);
        });
    }

    // <editor-fold desc="Configuration import and export">
    /**
     * The file extension of configuration files.
     *
     * It needs to be appended to the {@link File} object returned by a {@link FileChooser} when exporting,
     * as {@link FileChooser#showOpenDialog(Window)} does not append the extension associated with the
     * {@link #configurationExtensionFilter}.
     *
     * @see #exportConfiguration()
     */
    private String          configurationFileExtension;

    private ExtensionFilter configurationExtensionFilter;

    @Nonnull
    @Contract(pure = true)
    private FileChooser newConfigurationFileChooser(final String title) {
        final FileChooser fileChooser = new FileChooser();

        fileChooser.getExtensionFilters().add(configurationExtensionFilter);
        fileChooser.setTitle(title);

        return fileChooser;
    }

    private Map<Integer, ShipType> getShipTypesFromControls() throws InvalidShipTypeConfigurationException {
        final List<ShipTypeConfiguration> configurations = shipTypeEditingComboBox.getItems();

        return IntStream
            .range(0, configurations.size())
            .boxed()
            .collect(toMap(Function.identity(), i -> configurations.get(i).toShipType()));
    }

    private void setControlsFromShipTypes(@Nonnull final Map<Integer, ShipType> shipTypes) {
        shipTypeEditingComboBox
            .getItems()
            .setAll(ShipTypeConfiguration.fromShipTypes(shipTypes.values()));
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
    @Nonnull
    @Contract(" -> new")
    private Configuration getConfigurationFromControls()
            throws InsufficientFieldSizeException, InvalidShipTypeConfigurationException {
        final Configuration configuration =
            new Configuration.Builder()
                .maxPlayerCount(maxPlayerCountSpinner.getValue())
                .width(widthSpinner.getValue())
                .height(heightSpinner.getValue())
                .shotCount(shotCountSpinner.getValue())
                .hitPoints(hitPointsSpinner.getValue())
                .sunkPoints(sunkPointsSpinner.getValue())
                .roundTime((long) roundTimeSpinner.getValue()                 * 1_000L) /* Convert s to ms. */
                .visualizationTime((long) visualizationTimeSpinner.getValue() * 1_000L)
                .ships(getShipTypesFromControls())
                .penaltyMinusPoints(penaltyMinusPointsSpinner.getValue())
                .penaltyKind(penaltyTypeComboBox.getValue())
                .build();
        validator.validate(configuration);

        return configuration;
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
    private void setControlsFromConfiguration(@Nonnull final Configuration configuration) {
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

        /* Convert ms to s. */
        roundTimeSpinner
                .getValueFactory()
                .setValue((int) configuration.getRoundTime() / 1_000);
        visualizationTimeSpinner
                .getValueFactory()
                .setValue((int) configuration.getVisualizationTime() / 1_000);

        setControlsFromShipTypes(configuration.getShips());

        penaltyMinusPointsSpinner
                .getValueFactory()
                .setValue(configuration.getPenaltyMinusPoints());
        penaltyTypeComboBox
                .getSelectionModel()
                .select(configuration.getPenaltyKind());
    }

    /**
     * @see #getConfigurationFromControls()
     */
    void exportConfiguration() {
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
        } catch (final InvalidShipTypeConfigurationException exception) {
            displayInvalidShipTypeConfigurationAlert(exception.getInvalidConfiguration());
        } catch (final InsufficientFieldSizeException exception) {
            displayInsufficientFieldSizeAlert(exception.getRecommendedSize());
        } catch (final IOException exception) {
            final String contentText =
                String.format(resourceBundle.getString("configuration.export.exceptionAlert.contentText"), exportPath);

            LOGGER.error(exception);

            AlertBuilder
                .ofThrowable(exception, resourceBundle.getString("configuration.export.exceptionAlert.labelText"))
                .title(resourceBundle.getString("configuration.export.exceptionAlert.title"))
                .headerText(resourceBundle.getString("configuration.export.exceptionAlert.headerText"))
                .contentText(contentText)
                .build()
                .showAndWait();
        }
    }

    /**
     * @see #setControlsFromConfiguration(Configuration) 
     */
    void importConfiguration() {
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

            AlertBuilder
                .ofThrowable(exception, resourceBundle.getString("configuration.import.exceptionAlert.labelText"))
                .title(resourceBundle.getString("configuration.import.exceptionAlert.title"))
                .headerText(resourceBundle.getString("configuration.import.exceptionAlert.headerText"))
                .contentText(contentText)
                .build()
                .showAndWait();
        }
    }
    // </editor-fold>

    private void displayInvalidNameAlert(final String invalidName) {
        final Alert alert = new Alert(AlertType.ERROR);

        alert.setContentText(resourceBundle.getString("game.name.invalidNameAlert.contentText"));
        alert.setTitle(resourceBundle.getString("game.name.invalidNameAlert.title"));
        alert.setHeaderText(String.format(resourceBundle.getString("game.name.invalidNameAlert.header"), invalidName));

        alert.showAndWait();
    }

    @FXML
    @SuppressWarnings("unused")
    private void onStartNewGameButtonAction() {
        final Configuration configuration;

        final String gameName = gameNameTextField.getText().trim();
        if (gameName.isEmpty()) {
            displayInvalidNameAlert(gameName);
            return;
        }

        try {
            configuration = getConfigurationFromControls();

            gameManager.createGame(configuration,gameNameTextField.getText(),false);
        } catch (final InvalidShipTypeConfigurationException exception) {
            displayInvalidShipTypeConfigurationAlert(exception.getInvalidConfiguration());
        } catch (final InsufficientFieldSizeException exception) {
            displayInsufficientFieldSizeAlert(exception.getRecommendedSize());
        }
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
    @VisibleForTesting
    static final class ShipTypeConfiguration {

        private int width;
        private int height;

        private final Set<Point2D> marks;

        /**
         * The (preferably) unique label of this {@code ShipTypeConfiguration} displayed by the {@link #toString()}.
         * 
         * @see #toShipTypeLabel(int) 
         */
        private final String label;

        /**
         * The default {@link #width} and {@link #height} a newly instantiated {@code ShipTypeConfiguration} should
         * have.
         */
        private static final int DEFAULT_WIDTH_AND_HEIGHT = 5;

        /**
         * The minimum size of a {@link ShipType} according to the product vision.
         */
        private static final int MINIMUM_SHIP_TYPE_SIZE   = 2;

        @Contract(pure = true)
        private ShipTypeConfiguration(@Nonnull final String label) {
            this(label, new HashSet<>());
        }

        @Contract(pure = true)
        private ShipTypeConfiguration(@Nonnull final String label, @Nonnull final Collection<Point2D> marks) {
            this(label, new HashSet<>(marks));
        }

        @Contract(pure = true)
        @VisibleForTesting
        ShipTypeConfiguration(@Nonnull final String label, @Nonnull final Set<Point2D> marks) {
            this.label = label;
            this.marks = marks;

            /*
             * Fit the width/height to the marks with the greatest x/y coordinate if marks contains elements,
             * otherwise use DEFAULT_WIDTH_AND_HEIGHT.
             */
            if (!marks.isEmpty()) {
                final Point2D maxX = Collections.max(marks, Comparator.comparingInt(Point2D::getX));
                final Point2D maxY = Collections.max(marks, Comparator.comparingInt(Point2D::getY));

                width  = maxX.getX();
                height = maxY.getY();
            }
            width  = Math.max(width,  DEFAULT_WIDTH_AND_HEIGHT);
            height = Math.max(height, DEFAULT_WIDTH_AND_HEIGHT);
        }

        @Nonnull
        private static Collection<ShipTypeConfiguration> fromShipTypes(@Nonnull final Collection<ShipType> shipTypes) {
            final List<ShipTypeConfiguration> shipTypeConfigurations = new ArrayList<>(shipTypes.size());

            int i = 0;
            for (ShipType shipType : shipTypes) {
                shipTypeConfigurations.add(new ShipTypeConfiguration(toShipTypeLabel(i), shipType.getPositions()));
                ++i;
            }
            return shipTypeConfigurations;
        }

        @VisibleForTesting
        static Set<Point2D> normalize(@Nonnull final Set<Point2D> marks) {
            if (marks.isEmpty())
                return marks;
            final Point2D minX = Collections.min(marks, Comparator.comparingInt(Point2D::getX));
            final Point2D minY = Collections.min(marks, Comparator.comparingInt(Point2D::getY));

            return marks
                .stream()
                .map(mark -> new Point2D(mark.getX() - minX.getX(), mark.getY() - minY.getY()))
                .collect(toSet());
        }

        /**
         * @inheritDoc
         *
         * @return The label of this {@code ShipType}, used for display inside of {@link #shipTypeEditingComboBox}.
         */
        @Override
        @Contract(pure = true)
        public String toString() {
            return label;
        }

        /**
         * Removes all {@link Point2D} objects from the {@link #marks} collection which lie outside the bounds set by
         * the {@link #width} and {@link #height} attributes.
         */
        private void removeInvalidMarks() {
            marks.removeIf(point -> (point.getX() >= width) || (point.getY() >= height));
        }

        // <editor-fold desc="toShipType()">
        /**
         * Checks whether the depth-first search should be continued for the point {@code (x, y)}.
         *
         * The search should be continued if the provided point {@code (x, y)} is within the bounds of the matrix,
         * a {@link Point2D} object representing this point exists inside the {@link #marks} collection, and if it has
         * not yet been visited.
         *
         * @param marks A matrix representation of the {@link #marks} collection.
         *
         * @param visited A matrix representing the {@link Point2D} objects of the {@link #marks} collection which
         *                have already been visited by a previous iteration of the algorithm.
         *
         * @param x The x coordinate of the point which is to be checked for a continuation of the depth-first search.
         *
         * @param y The y coordinate of the point which is to be checked for a continuation of the depth-first search.
         *
         * @return {@code true} if the depth-first search should be continued for the point {@code (x, y)},
         *         otherwise {@code false}.
         *
         * @see #dfs(boolean[][], boolean[][], int, int) 
         */
        @Contract(pure = true)
        private boolean shouldTraverse(
                @Nonnull final boolean[][] marks,
                @Nonnull final boolean[][] visited,
                final int x,
                final int y) {
            return ((x >= 0) && (x < width)) && ((y >= 0) && (y < height)) && marks[x][y] && !visited[x][y];
        }

        /**
         * An array of {@link Point2D} objects containing the offsets used to reach a neighboring {@code Point2D} from
         * another {@code Point2D} object.
         */
        private static final Point2D[] NEIGHBOR_OFFSETS = {
                new Point2D(-1, 0), /* Left   */
                new Point2D( 0, 1), /* Top    */
                new Point2D( 1, 0), /* Right  */
                new Point2D( 0, -1) /* Bottom */
        };

        /**
         * Recursively executes a depth-first search starting at the point {@code (x, y)}.
         *
         * @param marks A matrix representation of the {@link #marks} collection.
         *
         * @param visited A matrix representing the {@link Point2D} objects of the {@link #marks} collection which
         *                have already been visited by a previous iteration of the algorithm.
         *
         * @param x The x starting coordinate of the point from which to start the depth-first search.
         *
         * @param y The y starting coordinate of the point from which to start the depth-first search.
         *
         * @see #checkMarksConnected()
         */
        private void dfs(
                @Nonnull final boolean[][] marks,
                @Nonnull final boolean[][] visited,
                final int x,
                final int y) {
            visited[x][y] = true;

            for (final Point2D offset : NEIGHBOR_OFFSETS) {
                final int xx = x + offset.getX();
                final int yy = y + offset.getY();

                if (shouldTraverse(marks, visited, xx, yy))
                    dfs(marks, visited, xx, yy);
            }
        }

        /**
         * Checks whether or not the individual {@link Point2D} objects inside the {@link #marks} collection are all
         * directly adjacent to each other.
         *
         * Two {@link Point2D}s are considered directly adjacent if and only if one can be reached from the other by
         * going up, down, left, or right and vice versa.
         *
         * @return {@code true} if all {@link Point2D} objects inside the {@link #marks} collection are adjacent,
         *         otherwise {@code false}.
         *
         * @see #toShipType()
         */
        @VisibleForTesting
        boolean checkMarksConnected() {
            final boolean[][] markMatrix = new boolean[width][height];
            final boolean[][] visited    = new boolean[width][height];

            for (Point2D mark : marks)
                markMatrix[mark.getX()][mark.getY()] = true;

            int segmentCount = 0;
            for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height; ++y) {
                    if (markMatrix[x][y] && !visited[x][y]) {
                        dfs(markMatrix, visited, x, y);

                        if (++segmentCount > 1)
                            return false;
                    }
                }
            }
            return true;
        }

        /**
         * Checks whether this {@code ShipTypeConfiguration} consists of the minimum amount of {@link Point2D} objects
         * according to the product vision.
         *
         * @return {@code true} if this {@code ShipTypeConfiguration} has the minimum size, otherwise {@code false}.
         *
         * @see #toShipType()
         */
        @Contract(pure = true)
        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        private boolean hasMinimumSize() {
            return marks.size() >= MINIMUM_SHIP_TYPE_SIZE;
        }

        /**
         * Converts this {@code ShipTypeConfiguration} into a {@link ShipType}
         *
         * @return A new {@link ShipType} representing this {@link ShipTypeConfiguration}.
         *
         * @throws InvalidShipTypeConfigurationException If this {@code ShipTypeConfiguration} is not of the minimum
         *                                               size or the individual {@link Point2D} are not directly
         *                                               adjacent to each other.
         *
         * @see InvalidShipTypeConfigurationException
         */
        @Nonnull
        @Contract(value = " -> new", pure = true)
        private ShipType toShipType() throws InvalidShipTypeConfigurationException {
            if (!hasMinimumSize() || !checkMarksConnected())
                throw new InvalidShipTypeConfigurationException(this);
            return new ShipType(marks);
        }
        // </editor-fold>
    }

    /**
     * An exception thrown to indicate that a {@link ShipTypeConfiguration} is not valid (e.g. because some it is too
     * small or because the individual parts that make up the ship are not connected) and thus cannot be converted
     * into a {@link ShipType}.
     *
     * @see ShipTypeConfiguration#toShipType()
     */
    private static final class InvalidShipTypeConfigurationException extends RuntimeException {

        private final ShipTypeConfiguration invalidConfiguration;

        /**
         * Instantiates a new {@code InvalidShipTypeConfigurationException}.
         *
         * @param invalidConfiguration The invalid {@link ShipTypeConfiguration} which caused this exception.
         */
        private InvalidShipTypeConfigurationException(@Nonnull final ShipTypeConfiguration invalidConfiguration) {
            this.invalidConfiguration = invalidConfiguration;
        }

        private ShipTypeConfiguration getInvalidConfiguration() {
            return invalidConfiguration;
        }
    }
}
