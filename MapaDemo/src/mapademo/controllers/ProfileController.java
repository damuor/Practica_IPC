package mapademo.controllers;

import java.io.File;
import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import upv.ipc.sportlib.User;

/*
 * Nota de autoria: el equipo ha usado IA como apoyo en la estructura general
 * del controlador. Los metodos marcados con "IA:" identifican fragmentos
 * integrados integramente con ayuda de IA por su complejidad tecnica.
 */
public class ProfileController {
    private MainLayoutController mainController;

    @FXML Label profileTitleLabel;
    @FXML Label profileHintLabel;
    @FXML Label profileNickTextLabel;
    @FXML Label profileNickLabel;
    @FXML Label profileEmailTextLabel;
    @FXML TextField profileEmailField;
    @FXML Label profilePasswordTextLabel;
    @FXML PasswordField profilePasswordField;
    @FXML Label profileBirthDateTextLabel;
    @FXML DatePicker profileBirthDatePicker;
    @FXML Label profileAvatarTextLabel;
    @FXML TextField profileAvatarField;
    @FXML ImageView profileAvatarImageView;
    @FXML Button browseAvatarButton;
    @FXML Button saveProfileButton;
    @FXML Label profileEmailErrorLabel;
    @FXML Label profilePasswordErrorLabel;
    @FXML Label profileBirthDateErrorLabel;
    @FXML Label profileAvatarErrorLabel;

    void setMainController(MainLayoutController mainController) {
        this.mainController = mainController;
    }

    void applyLanguage() {
        profileTitleLabel.setText(mainController.tr("profile.title"));
        profileHintLabel.setText(mainController.tr("profile.hint"));
        profileNickTextLabel.setText(mainController.tr("profile.nick"));
        profileEmailTextLabel.setText(mainController.tr("profile.email"));
        profilePasswordTextLabel.setText(mainController.tr("profile.newPassword"));
        profilePasswordField.setPromptText(mainController.tr("profile.passwordPrompt"));
        profileBirthDateTextLabel.setText(mainController.tr("profile.birthDate"));
        profileAvatarTextLabel.setText(mainController.tr("profile.avatar"));
        saveProfileButton.setText(mainController.tr("button.saveProfile"));
    }

    void refresh() {
        User user = mainController.app.getCurrentUser();
        if (user == null) {
            profileNickLabel.setText("-");
            profileEmailField.clear();
            profilePasswordField.clear();
            profileBirthDatePicker.setValue(null);
            profileAvatarField.clear();
            hideErrors();
            return;
        }

        profileNickLabel.setText(user.getNickName());
        profileEmailField.setText(user.getEmail());
        profilePasswordField.clear();
        profileBirthDatePicker.setValue(user.getBirthDate());
        profileAvatarField.setText(user.getAvatarPath() == null ? "" : user.getAvatarPath());
        hideErrors();
    }

    void hideErrors() {
        mainController.hideInlineErrors(profileEmailErrorLabel, profilePasswordErrorLabel, profileBirthDateErrorLabel, profileAvatarErrorLabel);
        mainController.setErrorState(profileEmailField, false);
        mainController.setErrorState(profilePasswordField, false);
        mainController.setErrorState(profileBirthDatePicker, false);
        mainController.setErrorState(profileAvatarField, false);
    }

    @FXML
    void onBrowseAvatar() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(mainController.tr("dialog.avatarTitle"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(mainController.tr("dialog.imageFilter"), "*.png", "*.jpg", "*.jpeg"));

        File selected = chooser.showOpenDialog(mainController.getOwnerWindow());
        if (selected != null) {
            profileAvatarField.setText(selected.getAbsolutePath());
            mainController.updateAvatarPreviewFromPath(selected.getAbsolutePath());
        }
    }

    @FXML
    // IA: agrupa validacion de perfil, avatar y persistencia manteniendo feedback inline al usuario.
    void onSaveProfile() {
        if (!mainController.requireLogin()) {
            return;
        }

        User user = mainController.app.getCurrentUser();
        hideErrors();

        String email = mainController.safeTrim(profileEmailField.getText());
        String rawPassword = profilePasswordField.getText();
        String password = rawPassword == null || rawPassword.isBlank() ? user.getPassword() : rawPassword;
        LocalDate birthDate = profileBirthDatePicker.getValue();

        String avatarPath = profileAvatarField.getText() == null || profileAvatarField.getText().isBlank()
                ? user.getAvatarPath()
                : profileAvatarField.getText().trim();

        boolean valid = true;
        if (!User.checkEmail(email)) {
            mainController.showInlineError(profileEmailErrorLabel, profileEmailField, mainController.tr("error.profileEmail"));
            valid = false;
        }
        if (rawPassword != null && !rawPassword.isBlank() && !User.checkPassword(rawPassword)) {
            mainController.showInlineError(profilePasswordErrorLabel, profilePasswordField, mainController.tr("error.profilePassword"));
            valid = false;
        }
        if (birthDate == null || !User.isOlderThan(birthDate, 12)) {
            mainController.showInlineError(profileBirthDateErrorLabel, profileBirthDatePicker, mainController.tr("error.profileBirthDate"));
            valid = false;
        }
        if (avatarPath != null && !avatarPath.isBlank() && !new File(avatarPath).exists()) {
            mainController.showInlineError(profileAvatarErrorLabel, profileAvatarField, mainController.tr("error.profileAvatar"));
            valid = false;
        }

        if (!valid) {
            mainController.setStatus(mainController.tr("status.profileInvalid"));
            return;
        }

        boolean updated;
        try {
            updated = mainController.app.updateCurrentUser(email, password, birthDate, avatarPath);
        } catch (RuntimeException ex) {
            mainController.showError(mainController.tr("error.profilePersistence"));
            return;
        }
        if (!updated) {
            mainController.showError(mainController.tr("error.profileGeneric"));
            return;
        }

        profilePasswordField.clear();
        mainController.refreshAllData();
        mainController.setStatus(mainController.tr("status.profileUpdated"));
    }
}


