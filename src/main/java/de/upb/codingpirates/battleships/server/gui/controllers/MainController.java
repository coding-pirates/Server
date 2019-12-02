package de.upb.codingpirates.battleships.server.gui.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;

public final class MainController extends AbstractController<Parent> {

    @FXML
    private Parent configuration;
    @FXML
    private ConfigurationController configurationController;

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
