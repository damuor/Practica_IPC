package mapademo.controllers;

import java.io.IOException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Modality;
import javafx.stage.Stage;

/*
 * Nota de autoria: el equipo ha usado IA como apoyo en la estructura general
 * del controlador. Los metodos marcados con "IA:" identifican fragmentos
 * integrados integramente con ayuda de IA por su complejidad tecnica.
 */
public class TopBarController {

    private MainLayoutController mainController;

    @FXML Label currentUserLabel;
    @FXML Label userTextLabel;
    @FXML Label languageTextLabel;
    @FXML Label statusLabel;
    @FXML ImageView currentAvatarImageView;
    @FXML Button loginButton;
    @FXML Button registerButton;
    @FXML Button logoutButton;
    @FXML ComboBox<MainLayoutController.LanguageOption> languageComboBox;
    @FXML Menu fileMenu;
    @FXML Menu activityMenu;
    @FXML MenuItem importActivityMenuItem;
    @FXML MenuItem renameActivityMenuItem;
    @FXML MenuItem removeActivityMenuItem;
    @FXML MenuItem logoutMenuItem;
    @FXML MenuItem exitMenuItem;

    void setMainController(MainLayoutController mainController) {
        this.mainController = mainController;
    }

    void configureLanguageSelector() {
        languageComboBox.getItems().setAll(mainController.getLanguageOptions());
        languageComboBox.getSelectionModel().select(mainController.getCurrentLanguageOption());
        languageComboBox.valueProperty().addListener((obs, oldValue, newValue) -> mainController.setLanguage(newValue));
        applyLanguage();
    }

    void configureMenuAccelerators() {
        importActivityMenuItem.setAccelerator(shortcut(KeyCode.I));
        renameActivityMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F2));
        logoutMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        exitMenuItem.setAccelerator(shortcut(KeyCode.Q));
    }

    private KeyCodeCombination shortcut(KeyCode code) {
        return new KeyCodeCombination(code, KeyCombination.SHORTCUT_DOWN);
    }

    void applyLanguage() {
        fileMenu.setText(mainController.tr("menu.file"));
        activityMenu.setText(mainController.tr("menu.activity"));
        importActivityMenuItem.setText(mainController.tr("menu.importGpx"));
        renameActivityMenuItem.setText(mainController.tr("menu.renameActivity"));
        removeActivityMenuItem.setText(mainController.tr("menu.removeActivity"));
        logoutMenuItem.setText(mainController.tr("menu.logout"));
        exitMenuItem.setText(mainController.tr("menu.exit"));
        userTextLabel.setText(mainController.tr("user.label"));
        languageTextLabel.setText(mainController.tr("language.label"));
        loginButton.setText(mainController.tr("button.login"));
        registerButton.setText(mainController.tr("button.register"));
        logoutButton.setText(mainController.tr("button.logout"));

        if (languageComboBox.getSelectionModel().isEmpty()) {
            languageComboBox.getSelectionModel().select(mainController.getCurrentLanguageOption());
        }
    }

    @FXML
    private void onLogin() {
        boolean logged = showLoginWindowAndAuthenticate();
        if (logged) {
            mainController.refreshAllData();
            mainController.setStatus(mainController.tr("status.login"));
        }
    }

    @FXML
    private void onRegister() {
        boolean registered = showRegisterWindowAndRegister();
        if (registered) {
            mainController.setStatus(mainController.tr("status.register"));
        }
    }

    @FXML
    private void onLogout() {
        if (mainController.app.getCurrentUser() == null) {
            return;
        }

        mainController.logoutCurrentSession();
        mainController.refreshAllData();
        mainController.setStatus(mainController.tr("status.logout"));
    }

    @FXML
    private void onExit() {
        mainController.shutdown();
        Platform.exit();
    }

    @FXML private void onImportActivity() { mainController.onImportActivity(); }
    @FXML private void onRenameActivity() { mainController.onRenameActivity(); }
    @FXML private void onRemoveActivity() { mainController.onRemoveActivity(); }

    void requestLogin() {
        if (!loginButton.isDisabled()) {
            onLogin();
        }
    }

    void requestRegister() {
        if (!registerButton.isDisabled()) {
            onRegister();
        }
    }

    // IA: carga ventanas modales FXML manteniendo la autenticacion separada del layout principal.
    private boolean showLoginWindowAndAuthenticate() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../views/LoginDialog.fxml"));
            Parent root = loader.load();
            Stage stage = createModalStage(root, mainController.tr("dialog.login.title"));
            LoginDialogController controller = loader.getController();
            controller.setMainController(mainController, stage);
            stage.showAndWait();
            return controller.isLoggedIn();
        } catch (IOException ex) {
            mainController.showError(mainController.tr("error.openLogin"));
            return false;
        }
    }

    private boolean showRegisterWindowAndRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../views/RegisterDialog.fxml"));
            Parent root = loader.load();
            Stage stage = createModalStage(root, mainController.tr("dialog.register.title"));
            RegisterDialogController controller = loader.getController();
            controller.setMainController(mainController, stage);
            stage.showAndWait();
            return controller.isRegistered();
        } catch (IOException ex) {
            mainController.showError(mainController.tr("error.openRegister"));
            return false;
        }
    }

    private Stage createModalStage(Parent root, String title) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initModality(Modality.WINDOW_MODAL);
        if (mainController.getOwnerWindow() != null) {
            stage.initOwner(mainController.getOwnerWindow());
        }
        stage.setResizable(false);
        stage.setScene(new Scene(root));
        return stage;
    }
}
