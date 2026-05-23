package mapademo.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/*
 * Nota de autoria: el equipo ha usado IA como apoyo en la estructura general
 * del controlador. Los metodos marcados con "IA:" identifican fragmentos
 * integrados integramente con ayuda de IA por su complejidad tecnica.
 */
public class LoginDialogController implements Initializable {

    private static final double ERROR_SLOT_HEIGHT = 17.0;

    private MainLayoutController mainController;
    private Stage stage;
    private boolean loggedIn;
    private final BooleanProperty validNick = new SimpleBooleanProperty(false);
    private final BooleanProperty validPassword = new SimpleBooleanProperty(false);

    @FXML private Label headerLabel;
    @FXML private Text welcomeText;
    @FXML private Label nickLabel;
    @FXML private TextField nickField;
    @FXML private Label nickErrorLabel;
    @FXML private Label passwordLabel;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordPreviewField;
    @FXML private Label passwordErrorLabel;
    @FXML private CheckBox showPasswordCheckBox;
    @FXML private Label authErrorLabel;
    @FXML private Button loginActionButton;
    @FXML private Button cancelButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        reserveErrorSpace(nickErrorLabel, passwordErrorLabel, authErrorLabel);

        passwordPreviewField.textProperty().bindBidirectional(passwordField.textProperty());
        passwordField.visibleProperty().bind(showPasswordCheckBox.selectedProperty().not());
        passwordField.managedProperty().bind(showPasswordCheckBox.selectedProperty().not());
        passwordPreviewField.visibleProperty().bind(showPasswordCheckBox.selectedProperty());
        passwordPreviewField.managedProperty().bind(showPasswordCheckBox.selectedProperty());

        loginActionButton.disableProperty().bind(Bindings.not(Bindings.and(validNick, validPassword)));
        nickField.setOnAction(event -> {
            if (showPasswordCheckBox.isSelected()) {
                passwordPreviewField.requestFocus();
            } else {
                passwordField.requestFocus();
            }
        });
        passwordField.setOnAction(event -> onLogin());
        passwordPreviewField.setOnAction(event -> onLogin());
        nickField.textProperty().addListener((obs, oldValue, newValue) -> updateNickValidity(false));
        passwordField.textProperty().addListener((obs, oldValue, newValue) -> updatePasswordValidity(false));
        nickField.focusedProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue) {
                updateNickValidity(true);
            }
        });
        passwordField.focusedProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue) {
                updatePasswordValidity(true);
            }
        });
    }

    void setMainController(MainLayoutController mainController, Stage stage) {
        this.mainController = mainController;
        this.stage = stage;
        stage.setTitle(mainController.tr("dialog.login.title"));
        applyLanguage();
        hideErrors();
        updateNickValidity(false);
        updatePasswordValidity(false);
    }

    boolean isLoggedIn() {
        return loggedIn;
    }

    private void applyLanguage() {
        welcomeText.setText(mainController.tr("dialog.login.welcome"));
        headerLabel.setText(mainController.tr("dialog.login.header"));
        nickLabel.setText(mainController.tr("dialog.login.nick"));
        passwordLabel.setText(mainController.tr("dialog.login.password"));
        showPasswordCheckBox.setText(mainController.tr("dialog.showPassword"));
        loginActionButton.setText(mainController.tr("button.enter"));
        cancelButton.setText(mainController.tr("button.cancel"));
    }

    private void hideErrors() {
        clearErrorLabels(nickErrorLabel, passwordErrorLabel, authErrorLabel);
        mainController.setErrorState(nickField, false);
        mainController.setPasswordErrorState(passwordField, passwordPreviewField, false);
    }

    // IA: mantiene espacio fijo para errores y evita que el dialogo salte al validar campos.
    private void reserveErrorSpace(Label... labels) {
        for (Label label : labels) {
            label.setManaged(true);
            label.setVisible(false);
            label.setText("");
            label.setWrapText(false);
            label.setTextOverrun(OverrunStyle.ELLIPSIS);
            label.setMinHeight(ERROR_SLOT_HEIGHT);
            label.setPrefHeight(ERROR_SLOT_HEIGHT);
            label.setMaxHeight(ERROR_SLOT_HEIGHT);
            label.setMaxWidth(Double.MAX_VALUE);
        }
    }

    private void clearErrorLabels(Label... labels) {
        for (Label label : labels) {
            label.setText("");
            label.setVisible(false);
        }
    }

    private boolean updateNickValidity(boolean showError) {
        boolean valid = nickField.getText() != null && !nickField.getText().trim().isEmpty();
        validNick.set(valid);
        if (showError) {
            mainController.showFieldError(valid, nickField, nickErrorLabel, mainController.tr("error.loginNick"));
        } else if (valid) {
            mainController.setErrorState(nickField, false);
            nickErrorLabel.setVisible(false);
        }
        authErrorLabel.setVisible(false);
        return valid;
    }

    private boolean updatePasswordValidity(boolean showError) {
        boolean valid = passwordField.getText() != null && !passwordField.getText().isEmpty();
        validPassword.set(valid);
        if (showError) {
            mainController.showFieldError(valid, passwordField, passwordErrorLabel, mainController.tr("error.loginPassword"));
            mainController.setErrorState(passwordPreviewField, !valid);
        } else if (valid) {
            mainController.setPasswordErrorState(passwordField, passwordPreviewField, false);
            passwordErrorLabel.setVisible(false);
        }
        authErrorLabel.setVisible(false);
        return valid;
    }

    @FXML
    private void onLogin() {
        boolean nickOk = updateNickValidity(true);
        boolean passwordOk = updatePasswordValidity(true);
        if (!nickOk || !passwordOk) {
            return;
        }

        boolean okLogin;
        try {
            okLogin = mainController.app.login(nickField.getText().trim(), passwordField.getText());
        } catch (RuntimeException ex) {
            okLogin = false;
        }

        if (!okLogin) {
            authErrorLabel.setText(mainController.tr("error.loginAuth"));
            authErrorLabel.setVisible(true);
            mainController.setErrorState(nickField, true);
            mainController.setPasswordErrorState(passwordField, passwordPreviewField, true);
            return;
        }

        loggedIn = true;
        stage.close();
    }

    @FXML
    private void onCancel() {
        loggedIn = false;
        stage.close();
    }
}
