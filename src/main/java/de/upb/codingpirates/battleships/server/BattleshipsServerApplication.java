package de.upb.codingpirates.battleships.server;

import de.upb.codingpirates.battleships.server.gui.util.ResourceBundleWrapper;
import de.upb.codingpirates.battleships.server.network.ServerApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author Andre Blanke
 */
public final class BattleshipsServerApplication extends Application {

    private final ServerApplication server;

    /**
     * The title of the JavaFX application {@link Stage}.
     * 
     * @see #start(Stage) 
     */
    private static final String TITLE = "Battleships Server";

    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(final String[] args) {
        launch(args);
    }

    public BattleshipsServerApplication() {
        try {
            server = new ServerApplication();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new IllegalStateException("Server could not be created");
        }
    }

    /**
     * Loads a view with the provided {@code name} and resolves keys inside the FXML file using
     * a {@link ResourceBundle} with the same name.
     *
     * @param name The name of the view to load. It is used to resolve the path to the FXML file
     *             inside the {@code fxml} folder and the correct {@link ResourceBundle} to go
     *             along with it.
     *
     * @param <T> The type of the object stored inside the FXML file.
     *
     * @return The {@link Parent} node of the view which was stored inside the FXML file.
     *
     * @throws IOException If the loader was unable to load the FXML file associated with this view.
     *
     * @throws MissingResourceException If a {@code ResourceBundle} with the same name as the view
     *                                  could not be found.
     */
    private <T extends Parent> T loadView(@NotNull final String name)
            throws IOException, MissingResourceException {
        final String fxmlPath       = String.format("/fxml/%s.fxml", name);
        final String bundleBaseName = String.format("lang.%s", name);

        final FXMLLoader loader = new FXMLLoader(
            BattleshipsServerApplication.class.getResource(fxmlPath),
            new ResourceBundleWrapper(ResourceBundle.getBundle(bundleBaseName))
        );
        loader.setControllerFactory(server.getInjector()::getInstance);

        return loader.load();
    }

    @Override
    public void start(@NotNull final Stage stage) throws Exception {
        stage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });
        stage.setScene(new Scene(loadView("main")));
        stage.setTitle(TITLE);

        stage.centerOnScreen();
        stage.show();
    }
}
