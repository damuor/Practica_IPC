package mapademo.controllers;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import upv.ipc.sportlib.User;

/*
 * Nota de autoria: el equipo ha usado IA como apoyo en la estructura general
 * del controlador. Los metodos marcados con "IA:" identifican fragmentos
 * integrados integramente con ayuda de IA por su complejidad tecnica.
 */
public class RegisterDialogController implements Initializable {

    private static final double ERROR_SLOT_HEIGHT = 17.0;

    private MainLayoutController mainController;
    private Stage stage;
    private boolean registered;
    private final BooleanProperty validNick = new SimpleBooleanProperty(false);
    private final BooleanProperty validEmail = new SimpleBooleanProperty(false);
    private final BooleanProperty validPassword = new SimpleBooleanProperty(false);
    private final BooleanProperty validBirthDate = new SimpleBooleanProperty(false);

    @FXML private Label headerLabel;
    @FXML private Label nickLabel;
    @FXML private TextField nickField;
    @FXML private Label nickErrorLabel;
    @FXML private Label emailLabel;
    @FXML private TextField emailField;
    @FXML private Label emailErrorLabel;
    @FXML private Label passwordLabel;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordPreviewField;
    @FXML private Label passwordErrorLabel;
    @FXML private CheckBox showPasswordCheckBox;
    @FXML private Label passwordRulesLabel;
    @FXML private Label birthDateLabel;
    @FXML private DatePicker birthDatePicker;
    @FXML private Label birthDateErrorLabel;
    @FXML private Label avatarLabel;
    @FXML private TextField avatarField;
    @FXML private Button browseAvatarButton;
    @FXML private Label formErrorLabel;
    @FXML private Button registerActionButton;
    @FXML private Button cancelButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        reserveErrorSpace(
                nickErrorLabel,
                emailErrorLabel,
                passwordErrorLabel,
                birthDateErrorLabel,
                formErrorLabel
        );

        passwordPreviewField.textProperty().bindBidirectional(passwordField.textProperty());
        passwordField.visibleProperty().bind(showPasswordCheckBox.selectedProperty().not());
        passwordField.managedProperty().bind(showPasswordCheckBox.selectedProperty().not());
        passwordPreviewField.visibleProperty().bind(showPasswordCheckBox.selectedProperty());
        passwordPreviewField.managedProperty().bind(showPasswordCheckBox.selectedProperty());

        registerActionButton.disableProperty().bind(Bindings.not(
                Bindings.and(validNick, validEmail).and(validPassword).and(validBirthDate)
        ));
        nickField.textProperty().addListener((obs, oldValue, newValue) -> validateNick(false));
        emailField.textProperty().addListener((obs, oldValue, newValue) -> validateEmail(false));
        passwordField.textProperty().addListener((obs, oldValue, newValue) -> validatePassword(false));
        birthDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> validateBirthDate(false));
        nickField.focusedProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue) {
                validateNick(true);
            }
        });
        emailField.focusedProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue) {
                validateEmail(true);
            }
        });
        passwordField.focusedProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue) {
                validatePassword(true);
            }
        });
        birthDatePicker.focusedProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue) {
                validateBirthDate(true);
            }
        });
    }

    void setMainController(MainLayoutController mainController, Stage stage) {
        this.mainController = mainController;
        this.stage = stage;
        mainController.setupDatePicker(birthDatePicker);
        stage.setTitle(mainController.tr("dialog.register.title"));
        applyLanguage();
        hideErrors();
        validateAll(false);
    }

    boolean isRegistered() {
        return registered;
    }

    private void applyLanguage() {
        headerLabel.setText(mainController.tr("dialog.register.header"));
        nickLabel.setText(mainController.tr("dialog.login.nick"));
        emailLabel.setText(mainController.tr("dialog.register.email"));
        passwordLabel.setText(mainController.tr("dialog.login.password"));
        showPasswordCheckBox.setText(mainController.tr("dialog.showPassword"));
        passwordRulesLabel.setText(mainController.tr("dialog.register.passwordRules"));
        birthDateLabel.setText(mainController.tr("dialog.register.birthDate"));
        avatarLabel.setText(mainController.tr("dialog.register.avatar"));
        avatarField.setPromptText(mainController.tr("dialog.register.avatarPrompt"));
        browseAvatarButton.setText(mainController.tr("dialog.browse"));
        registerActionButton.setText(mainController.tr("button.register"));
        cancelButton.setText(mainController.tr("button.cancel"));
    }

    private void hideErrors() {
        clearErrorLabels(
                nickErrorLabel,
                emailErrorLabel,
                passwordErrorLabel,
                birthDateErrorLabel,
                formErrorLabel
        );
        mainController.setErrorState(nickField, false);
        mainController.setErrorState(emailField, false);
        mainController.setPasswordErrorState(passwordField, passwordPreviewField, false);
        mainController.setErrorState(birthDatePicker, false);
    }

    // IA: mantiene una reserva visual estable para que los mensajes de error no desplacen botones.
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

    private boolean validateAll(boolean showErrors) {
        boolean nickOk = validateNick(showErrors);
        boolean emailOk = validateEmail(showErrors);
        boolean passwordOk = validatePassword(showErrors);
        boolean birthDateOk = validateBirthDate(showErrors);
        return nickOk && emailOk && passwordOk && birthDateOk;
    }

    private boolean validateNick(boolean showError) {
        String nick = mainController == null ? "" : mainController.safeTrim(nickField.getText());
        boolean validFormat = User.checkNickName(nick);
        boolean exists = false;
        if (validFormat) {
            try {
                exists = mainController.app.nickNameExists(nick);
            } catch (RuntimeException ex) {
                exists = false;
            }
        }
        boolean valid = validFormat && !exists;
        validNick.set(valid);
        String message = exists ? mainController.tr("error.registerNickExists") : mainController.tr("error.registerNick");
        updateFieldError(showError, valid, nickField, nickErrorLabel, message);
        formErrorLabel.setVisible(false);
        return valid;
    }

    private boolean validateEmail(boolean showError) {
        String email = mainController == null ? "" : mainController.safeTrim(emailField.getText());
        boolean valid = User.checkEmail(email);
        validEmail.set(valid);
        updateFieldError(showError, valid, emailField, emailErrorLabel, mainController.tr("error.registerEmail"));
        formErrorLabel.setVisible(false);
        return valid;
    }

    private boolean validatePassword(boolean showError) {
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        boolean valid = User.checkPassword(password);
        validPassword.set(valid);
        if (showError) {
            mainController.showFieldError(valid, passwordField, passwordErrorLabel, mainController.tr("error.registerPassword"));
            mainController.setErrorState(passwordPreviewField, !valid);
        } else if (valid) {
            passwordErrorLabel.setVisible(false);
            mainController.setPasswordErrorState(passwordField, passwordPreviewField, false);
        }
        formErrorLabel.setVisible(false);
        return valid;
    }

    private boolean validateBirthDate(boolean showError) {
        LocalDate birthDate = birthDatePicker.getValue();
        boolean valid = birthDate != null && User.isOlderThan(birthDate, 12);
        validBirthDate.set(valid);
        updateFieldError(showError, valid, birthDatePicker, birthDateErrorLabel, mainController.tr("error.registerBirthDate"));
        formErrorLabel.setVisible(false);
        return valid;
    }

    private void updateFieldError(boolean showError, boolean valid, javafx.scene.Node field, Label errorLabel, String message) {
        if (showError) {
            mainController.showFieldError(valid, field, errorLabel, message);
        } else if (valid) {
            errorLabel.setVisible(false);
            mainController.setErrorState(field, false);
        }
    }

    @FXML
    private void onBrowseAvatar() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(mainController.tr("dialog.avatarTitle"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(mainController.tr("dialog.imageFilter"), "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            avatarField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void onRegister() {
        if (!validateAll(true)) {
            return;
        }

        String avatarPath = mainController.safeTrim(avatarField.getText());
        String avatar = avatarPath.isBlank() ? null : avatarPath;
        boolean okRegister;
        try {
            okRegister = mainController.app.registerUser(
                    mainController.safeTrim(nickField.getText()),
                    mainController.safeTrim(emailField.getText()),
                    passwordField.getText(),
                    birthDatePicker.getValue(),
                    avatar
            );
        } catch (RuntimeException ex) {
            okRegister = false;
        }

        if (!okRegister) {
            formErrorLabel.setText(mainController.tr("error.registerFailed"));
            formErrorLabel.setVisible(true);
            mainController.setErrorState(nickField, true);
            mainController.setErrorState(emailField, true);
            return;
        }

        registered = true;
        stage.close();
    }

    @FXML
    private void onCancel() {
        registered = false;
        stage.close();
    }
}
