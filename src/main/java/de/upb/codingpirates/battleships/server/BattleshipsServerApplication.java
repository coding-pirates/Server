package de.upb.codingpirates.battleships.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.upb.codingpirates.battleships.network.dispatcher.MessageDispatcher;
import de.upb.codingpirates.battleships.server.gui.util.ResourceBundleWrapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * The entry point of the server.
 *
 * @author Andre Blanke
 */
public final class BattleshipsServerApplication extends Application {

    @Nonnull
    private final Injector injector = Guice.createInjector(new ServerModule());

    /**
     * The title of this JavaFX application {@link Stage}.
     *
     * @see #start(Stage)
     */
    private static final String TITLE = "Battleships Server";

    public static void main(final String... args) {
        launch(args);
    }

    public BattleshipsServerApplication() {
        injector.getInstance(MessageDispatcher.class);
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
    @SuppressWarnings("SameParameterValue")
    private <T extends Parent> T loadView(@Nonnull final String name)
            throws IOException, MissingResourceException {
        final String fxmlPath       = String.format("/fxml/%s.fxml", name);
        final String bundleBaseName = String.format("lang.%s", name);

        final FXMLLoader loader = new FXMLLoader(
            BattleshipsServerApplication.class.getResource(fxmlPath),
            new ResourceBundleWrapper(ResourceBundle.getBundle(bundleBaseName))
        );
        loader.setControllerFactory(injector::getInstance);

        return loader.load();
    }

    @Override
    public void start(@Nonnull final Stage stage) throws Exception {
        stage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });
        stage.setScene(new Scene(loadView("main")));
        stage.setTitle(TITLE);

        stage.centerOnScreen();
        stage.show();

        stage.setResizable(false);
    }
}
