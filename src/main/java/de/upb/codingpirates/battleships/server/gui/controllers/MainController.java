package de.upb.codingpirates.battleships.server.gui.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.MapChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jetbrains.annotations.Contract;

import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.Game;
import de.upb.codingpirates.battleships.logic.GameState;
import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.game.GameHandler;
import de.upb.codingpirates.battleships.server.gui.util.AlertBuilder;

/**
 * The controller associated with the {@code main.fxml} file.
 *
 * Its main task is updating the {@link #gameTableView}, {@link #tournamentTableView}, and {@link #playerTableView}
 * when updates from the backing {@link GameManager}, {@link TournamentManager}, and {@link ClientManager} arrive.
 *
 * @author Andre Blanke
 */
public final class MainController extends AbstractController<Parent> {

    /**
     * The controller associated with the {@code configuration.fxml} file.
     */
    @FXML
    private ConfigurationController configurationController;

    @FXML
    private TableView<GameHandler> gameTableView;
    @FXML
    private TableView<?> tournamentTableView;

    @FXML
    private TableView<Client> playerTableView;

    private final ClientManager clientManager;
    private final GameManager   gameManager;

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

    /**
     * Updates the user interface whenever the {@link ClientManager#getPlayerMappings()} map changes.
     *
     * @param change The change which occurred to the {@link javafx.collections.ObservableMap}.
     */
    private void onPlayerMappingsChange(
            @Nonnull final MapChangeListener.Change<? extends Integer, ? extends Client> change) {
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
    private void onGameMappingsChange(
            @Nonnull final MapChangeListener.Change<? extends Integer, ? extends GameHandler> change) {
        if (change.wasAdded())
            gameTableView.getItems().add(change.getValueAdded());
        else if (change.wasRemoved())
            gameTableView.getItems().remove(change.getValueRemoved());

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
        final MenuItem addAiItem       = new MenuItem(resourceBundle.getString("overview.game.table.contextMenu.addAi.text"));

        row.itemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null)
                return;

            final GameHandler handler = row.getItem();

            final BooleanBinding inProgress = handler.stateProperty().isEqualTo(GameState.IN_PROGRESS);
            final BooleanBinding paused     = handler.stateProperty().isEqualTo(GameState.PAUSED);

            launchItem
                .disableProperty()
                .bind(handler.currentPlayerCountProperty().lessThan(GameHandler.MIN_PLAYER_COUNT));
            launchItem
                .setOnAction(event -> handler.launchGame());

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
                    if (handler.getState() == GameState.PAUSED)
                        handler.continueGame();
                    else
                        handler.pauseGame();
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
                        .ifPresent(alertResult -> handler.abortGame(alertResult == ButtonType.YES)));
            addAiItem
                .disableProperty()
                .bind(
                    handler.stateProperty().isNotEqualTo(GameState.LOBBY)
                        .or(handler.currentPlayerCountProperty().isEqualTo(handler.getMaxPlayerCount())));
            addAiItem
                .setOnAction(event -> aiExecutorService.submit(() -> {
                }));
        });
        return new ContextMenu(
            launchItem,
            pauseResumeItem,
            abortItem,
            new SeparatorMenuItem(),
            addAiItem);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void initializeTableViews() {
        final EventHandler<? super DragEvent> clientDragOverHandler = event -> {
            final Dragboard dragboard = event.getDragboard();
            final TableRow<?> row = (TableRow<?>) event.getGestureSource();

            if (!row.isEmpty() && dragboard.hasContent(SERIALIZED_MIME_TYPE)) {
                event.acceptTransferModes(TransferMode.LINK);
                event.consume();
            }
        };

        gameTableView.setRowFactory(tableView -> {
            final TableRow<GameHandler> row = new TableRow<>();

            row.setContextMenu(newGameHandlerTableRowContextMenu(row));

            row.setOnDragOver(clientDragOverHandler);
            row.setOnDragDropped(event -> {
                final Object content = event.getDragboard().getContent(SERIALIZED_MIME_TYPE);

                LOGGER.trace(CONTROLLER_MARKER, "Drag dropped for content '{}'.", content);

                if (row.isEmpty() || !(content instanceof Client) || (row.getItem().getState() != GameState.LOBBY))
                    return;

                try {
                    gameManager.addClientToGame(row.getItem().getId(), (Client) content);
                } catch (final GameException exception) {
                    LOGGER.error(exception);
                }
            });
            return row;
        });
        tournamentTableView.setRowFactory(tableView -> {
            final TableRow row = new TableRow<>();

            row.setOnDragOver(clientDragOverHandler);
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
