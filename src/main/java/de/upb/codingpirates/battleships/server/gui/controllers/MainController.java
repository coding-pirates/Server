package de.upb.codingpirates.battleships.server.gui.controllers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener.Change;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jetbrains.annotations.Contract;

import de.upb.codingpirates.battleships.ai.AI;
import de.upb.codingpirates.battleships.ai.gameplay.StandardShotPlacementStrategy;
import de.upb.codingpirates.battleships.logic.AbstractClient;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.Game;
import de.upb.codingpirates.battleships.logic.GameState;
import de.upb.codingpirates.battleships.network.Properties;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.server.BattleshipsServerApplication;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.game.GameHandler;
import de.upb.codingpirates.battleships.server.gui.util.AlertBuilder;
import de.upb.codingpirates.battleships.server.util.ServerProperties;

/**
 * The controller associated with the {@code main.fxml} file.
 *
 * Its main task is updating the {@link #gameTableView} and {@link #playerTableView} when updates from the backing
 * {@link GameManager} and {@link ClientManager} arrive.
 *
 * @author Andre Blanke
 */
public final class MainController extends AbstractController<Parent> {

    @FXML
    private Parent configuration;
    /**
     * The controller associated with the {@code configuration.fxml} file.
     */
    @FXML
    private ConfigurationController configurationController;

    @FXML
    private TableView<GameHandler> gameTableView;
    @FXML
    private TableColumn<GameHandler, String> playerCountTableColumn;

    @FXML
    private TableView<Client> playerTableView;

    @Nonnull
    private final ClientManager clientManager;

    @Nonnull
    private final GameManager gameManager;

    private final ExecutorService aiExecutorService = Executors.newCachedThreadPool();

    /**
     * The MIME type used for serialized Java objects, namely instances of {@link Client}.
     *
     * @see #initializeTableViews()
     */
    private static final DataFormat SERIALIZED_MIME_TYPE =
        new DataFormat("application/x-java-serialized-object");

    private static final Logger LOGGER = LogManager.getLogger();

    @Inject
    public MainController(@Nonnull final ClientManager clientManager, @Nonnull final GameManager gameManager) {
        this.clientManager = clientManager;
        this.gameManager   = gameManager;
    }

    // <editor-fold desc="Initialization">
    @Override
    public void initialize(final URL url, final ResourceBundle resourceBundle) {
        super.initialize(url, resourceBundle);

        clientManager
            .getPlayerMappings()
            .addListener(this::onPlayerMappingsChange);
        gameManager
            .getGameMappings()
            .addListener(this::onGameMappingsChange);

        initializeTableViews();
    }

    // <editor-fold desc="on*MappingsChange">
    /**
     * Updates the user interface whenever the {@link ClientManager#getPlayerMappings()} map changes.
     *
     * @param change The change which occurred to the {@link javafx.collections.ObservableMap}.
     */
    private void onPlayerMappingsChange(@Nonnull final Change<? extends Integer, ? extends Client> change) {
        if (change.wasAdded())
            playerTableView.getItems().add(change.getValueAdded());
        else if (change.wasRemoved())
            playerTableView.getItems().remove(change.getValueRemoved());
    }

    /**
     * Updates the user interface whenever the {@link GameManager#getGameMappings()} map changes.
     *
     * @param change The change which occurred to the {@link javafx.collections.ObservableMap}.
     */
    private void onGameMappingsChange(@Nonnull final Change<? extends Integer, ? extends GameHandler> change) {
        if (change.wasAdded())
            gameTableView.getItems().add(change.getValueAdded());
        else if (change.wasRemoved())
            gameTableView.getItems().remove(change.getValueRemoved());
    }
    // </editor-fold>

    @SuppressWarnings("unchecked")
    private void displayScoreStage(@Nonnull final GameHandler handler) {
        final Stage scoreStage = new Stage();
        final TableView<Entry<Integer, Integer>> scoreView = new TableView<>();

        final TableColumn<Entry<Integer, Integer>, String> nameColumn =
            new TableColumn<>(resourceBundle.getString("score.table.columns.name.text"));
        nameColumn.setCellValueFactory(cellDataFeatures -> {
            try {
                final AbstractClient client = clientManager.getClient(cellDataFeatures.getValue().getKey());

                return new ReadOnlyStringWrapper(client.getName());
            } catch (final InvalidActionException exception) {
                LOGGER.error(exception);
                return null;
            }
        });

        final TableColumn<Entry<Integer, Integer>, Integer> scoreColumn =
            new TableColumn<>(resourceBundle.getString("score.table.columns.score.text"));
        scoreColumn.setCellValueFactory(cellDataFeatures ->
            new ReadOnlyObjectWrapper<>(cellDataFeatures.getValue().getValue()));

        scoreView.getColumns().addAll(nameColumn, scoreColumn);

        scoreView.getItems().addAll(handler.getScore().entrySet());

        scoreStage.getIcons().add(BattleshipsServerApplication.APPLICATION_ICON);
        scoreStage.setScene(new Scene(scoreView));
        scoreStage.setTitle(String.format(resourceBundle.getString("score.stage.title"), handler.getGame().getName()));
        scoreStage.showAndWait();
        scoreStage.setWidth(360);
    }

    /**
     * Factory method for instantiating a new {@link ContextMenu} for the provided {@link TableRow}.
     *
     * The created {@code ContextMenu} supports starting, pausing, unpausing, an aborting {@link Game}s via their
     * associated {@link GameHandler}.
     *
     * @param row The {@code TableRow} for which a new {@code ContextMenu} is to be created.
     *
     * @return A new {@code ContextMenu} for the provided {@code row}.
     *
     * @see #initializeTableViews()
     */
    @Nonnull
    @Contract("_ -> new")
    private ContextMenu newGameHandlerTableRowContextMenu(@Nonnull final TableRow<GameHandler> row) {
        final MenuItem launchItem      = new MenuItem(resourceBundle.getString("overview.game.table.contextMenu.launch.text"));
        final MenuItem pauseResumeItem = new MenuItem();
        final MenuItem abortItem       = new MenuItem(resourceBundle.getString("overview.game.table.contextMenu.abort.text"));

        row.setOnMouseClicked(event -> {
            if ((event.getClickCount() != 2) || (row.getItem() == null))
                return;
            displayScoreStage(row.getItem());
        });
        row.itemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null)
                return;

            final GameHandler handler = row.getItem();

            final BooleanBinding inProgress = handler.stateProperty().isEqualTo(GameState.IN_PROGRESS);
            final BooleanBinding paused     = handler.stateProperty().isEqualTo(GameState.PAUSED);

            launchItem
                .disableProperty()
                .bind(handler.currentPlayerCountProperty().lessThan(ServerProperties.MIN_PLAYER_COUNT));
            launchItem
                .setOnAction(event -> {
                    LOGGER.trace(CONTROLLER_MARKER, "Attempting to launch game '{}'.", handler.getGame().getName());
                    gameManager.launchGame(handler.getGame().getId());
                });

            pauseResumeItem
                .disableProperty()
                .bind(Bindings.not(inProgress.or(paused)));
            pauseResumeItem
                .textProperty()
                .bind(
                    Bindings
                        .when(paused)
                        .then(resourceBundle.getString("overview.game.table.contextMenu.resume.text"))
                        .otherwise(resourceBundle.getString("overview.game.table.contextMenu.pause.text")));
            pauseResumeItem
                .setOnAction(event -> {
                    if (handler.getState() == GameState.PAUSED) {
                        LOGGER.trace(CONTROLLER_MARKER, "Attempting to resume game '{}'.", handler.getGame().getName());
                        gameManager.continueGame(handler.getGame().getId());
                        handler.continueGame();
                    } else {
                        LOGGER.trace(CONTROLLER_MARKER, "Attempting to pause game '{}'.", handler.getGame().getName());
                        gameManager.pauseGame(handler.getGame().getId());
                    }
                });

            abortItem
                .disableProperty()
                .bind(handler.stateProperty().isEqualTo(GameState.FINISHED));
            abortItem
                .setOnAction(event ->
                    AlertBuilder
                        .of(AlertType.CONFIRMATION)
                        .title(resourceBundle.getString("overview.game.table.contextMenu.abort.alert.title"))
                        .headerText(resourceBundle.getString("overview.game.table.contextMenu.abort.alert.headerText"))
                        .contentText(resourceBundle.getString("overview.game.table.contextMenu.abort.alert.contentText"))
                        .buttonTypes(ButtonType.YES, ButtonType.NO)
                        .build()
                        .showAndWait()
                        .ifPresent(alertResult -> {
                            LOGGER.trace(CONTROLLER_MARKER, "Attempting to abort game '{}'.", handler.getGame().getName());
                            gameManager.abortGame(handler.getGame().getId(), alertResult == ButtonType.YES);
                        }));
        });
        return new ContextMenu(launchItem, pauseResumeItem, abortItem);
    }

    @FXML
    private void displayShotPlacementStrategyPrompt() {
        final int hBoxSpacing = 10;

        final Label shotPlacementStrategyLabel =
                new Label(resourceBundle.getString("overview.player.table.contextMenu.selectAiShotPlacementStrategyAlert.labelText"));
        final ComboBox<StandardShotPlacementStrategy> shotPlacementStrategyComboBox =
                new ComboBox<>(FXCollections.observableArrayList(StandardShotPlacementStrategy.values()));

        shotPlacementStrategyComboBox.setValue(StandardShotPlacementStrategy.HEAT_MAP);

        AlertBuilder
            .of(AlertType.CONFIRMATION)
            .title(resourceBundle.getString("overview.player.table.contextMenu.selectAiShotPlacementStrategyAlert.title"))
            .headerText(resourceBundle.getString("overview.player.table.contextMenu.selectAiShotPlacementStrategyAlert.headerText"))
            .buttonTypes(ButtonType.OK, ButtonType.CANCEL)
            .dialogPaneContent(new HBox(hBoxSpacing, shotPlacementStrategyLabel, shotPlacementStrategyComboBox))
            .build()
            .showAndWait()
            .ifPresent(buttonType -> {
                if (buttonType != ButtonType.OK)
                    return;

                aiExecutorService.submit(() -> {
                    final AI ai = new AI(UUID.randomUUID().toString(), shotPlacementStrategyComboBox.getValue());

                    try {
                        ai.connect(InetAddress.getLocalHost().getHostName(), Properties.PORT);
                    } catch (final IOException exception) {
                        LOGGER.error(exception);
                    }
                });
            });
    }

    private void initializeTableViews() {
        playerCountTableColumn.setCellValueFactory(cellDataFeatures ->
            Bindings.concat(
                cellDataFeatures
                    .getValue()
                    .currentPlayerCountProperty()
                    .asString(),
                "/",
                cellDataFeatures
                    .getValue()
                    .getGame()
                    .getConfig()
                    .getMaxPlayerCount()
            ));

        gameTableView.setRowFactory(tableView -> {
            final TableRow<GameHandler> row = new TableRow<>();

            row.setContextMenu(newGameHandlerTableRowContextMenu(row));
            row.setOnDragOver(event -> {
                final Dragboard   dragboard = event.getDragboard();
                final TableRow<?> source    = (TableRow<?>) event.getGestureSource();

                if (!source.isEmpty() && dragboard.hasContent(SERIALIZED_MIME_TYPE)) {
                    event.acceptTransferModes(TransferMode.LINK);
                    event.consume();
                }
            });
            row.setOnDragDropped(event -> {
                final Object content = event.getDragboard().getContent(SERIALIZED_MIME_TYPE);

                LOGGER.trace(CONTROLLER_MARKER, "Drag dropped for content '{}'.", content);

                if (row.isEmpty() || !(content instanceof Client) || (row.getItem().getState() != GameState.LOBBY))
                    return;

                try {
                    gameManager.addClientToGame(row.getItem().getGame().getId(), (Client) content);
                } catch (final GameException exception) {
                    LOGGER.error(exception);
                }
            });
            return row;
        });
        playerTableView.setRowFactory(tableView -> {
            final TableRow<Client> row = new TableRow<>();

            row.setOnDragDetected(event -> {
                if (row.isEmpty())
                    return;
                final ClipboardContent content   = new ClipboardContent();
                final Dragboard        dragboard = row.startDragAndDrop(TransferMode.LINK);

                LOGGER.trace(CONTROLLER_MARKER, "Detected drag for content '{}'.", row.getItem());

                content.put(SERIALIZED_MIME_TYPE, row.getItem());

                dragboard.setContent(content);
                dragboard.setDragView(row.snapshot(null, null));

                event.consume();
            });
            return row;
        });
    }
    // </editor-fold>

    // <editor-fold desc="MenuBar">
    @FXML
    private void onExportConfigurationMenuItemAction() {
        configurationController.exportConfiguration();
    }

    @FXML
    private void onImportConfigurationMenuItemAction() {
        configurationController.importConfiguration();
    }

    @FXML
    private void onExitMenuItemAction() {
        Platform.exit();
    }
    // </editor-fold>
}
