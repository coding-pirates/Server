package de.upb.codingpirates.battleships.server.gui.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import javax.inject.Inject;

import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jetbrains.annotations.NotNull;

import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.Game;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.game.GameHandler;

/**
 * The controller associated with the {@code main.fxml} file.
 *
 * @author Andre Blanke
 */
public final class MainController extends AbstractController<Parent> {

    @FXML
    private Parent configuration;
    @FXML
    private ConfigurationController configurationController;

    @FXML
    private TableView<Game>   gameTableView;
    @FXML
    private TableView<Client> playerTableView;

    private final ClientManager clientManager;
    private final GameManager   gameManager;

    private static final DataFormat SERIALIZED_MIME_TYPE =
        new DataFormat("application/x-java-serialized-object");

    private static final Logger LOGGER = LogManager.getLogger();

    @Inject
    public MainController(@NotNull final ClientManager clientManager, @NotNull final GameManager gameManager) {
        this.clientManager = clientManager;
        this.gameManager   = gameManager;
    }

    // <editor-fold desc="Initialization">
    @Override
    public void initialize(final URL url, final ResourceBundle resourceBundle) {
        clientManager
            .getPlayerMappings()
            .addListener(this::onPlayerMappingsChange);
        gameManager
            .getGameMappings()
            .addListener(this::onGameMappingsChange);

        initializeTableViews();
    }

    private void onPlayerMappingsChange(
            @NotNull final MapChangeListener.Change<? extends Integer, ? extends Client> change) {
        if (change.wasAdded())
            playerTableView.getItems().add(change.getValueAdded());
        else if (change.wasRemoved())
            playerTableView.getItems().remove(change.getValueRemoved());
    }

    private void onGameMappingsChange(
            @NotNull final MapChangeListener.Change<? extends Integer, ? extends GameHandler> change) {
        if (change.wasAdded())
            gameTableView.getItems().add(change.getValueAdded().getGame());
        else if (change.wasRemoved())
            gameTableView.getItems().remove(change.getValueRemoved().getGame());

    }

    private void initializeTableViews() {
        playerTableView.getItems().add(new Client(0, "test"));

        gameTableView.setRowFactory(tableView -> {
            final TableRow<Game> row = new TableRow<>();

            row.setOnDragOver(event -> {
                final Dragboard dragboard = event.getDragboard();

                if (!row.isEmpty() && dragboard.hasContent(SERIALIZED_MIME_TYPE)) {
                    event.acceptTransferModes(TransferMode.LINK);
                    event.consume();
                }
            });
            row.setOnDragDropped(event -> {
                final Dragboard dragboard = event.getDragboard();
                final Object    content   = dragboard.getContent(SERIALIZED_MIME_TYPE);

                if (row.isEmpty() || !(content instanceof Client))
                    return;

                try {
                    gameManager.getGame(row.getItem().getId()).addClient(ClientType.PLAYER, (Client) content);
                    tableView.refresh();
                } catch (final InvalidActionException exception) {
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
