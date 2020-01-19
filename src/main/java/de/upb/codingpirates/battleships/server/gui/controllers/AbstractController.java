package de.upb.codingpirates.battleships.server.gui.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * @param <T> The type of the object contained within the FXML file.
 *            Must be a subtype of {@link Parent}, as it is expected to belong to a view.
 * @author Andre Blanke
 */
abstract class AbstractController<T extends Parent> implements Initializable {

    ResourceBundle resourceBundle;

    @FXML
    T root;

    static final Marker CONTROLLER_MARKER = MarkerManager.getMarker("UI Controller");

    @Override
    public void initialize(final URL url, final ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }
}
