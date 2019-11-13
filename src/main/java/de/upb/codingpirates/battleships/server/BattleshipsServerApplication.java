package de.upb.codingpirates.battleships.server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jetbrains.annotations.NotNull;

public class BattleshipsServerApplication extends Application {

    private static final String TITLE = "Battleships Server";

    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(final String[] args) {
        launch(args);
    }

    @Override
    public void start(@NotNull final Stage stage) throws Exception {
        final Parent root = new FXMLLoader(getClass().getResource("/fxml/overview.fxml")).load();

        stage.setScene(new Scene(root));
        stage.setTitle(TITLE);
        stage.show();
    }
}
