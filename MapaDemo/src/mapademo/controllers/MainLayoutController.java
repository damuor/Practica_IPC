package mapademo.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import javafx.util.StringConverter;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.Session;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

/*
 * Nota de autoria: el equipo ha usado IA como apoyo en la estructura general
 * del controlador. Los metodos marcados con "IA:" identifican fragmentos
 * integrados integramente con ayuda de IA por su complejidad tecnica.
 */
public class MainLayoutController implements Initializable {

    static final Locale SPANISH_LOCALE = Locale.forLanguageTag("es-ES");
    static final Locale VALENCIAN_LOCALE = Locale.forLanguageTag("ca-ES-valencia");
    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final PseudoClass ERROR_PSEUDO_CLASS = PseudoClass.getPseudoClass("error");

    final SportActivityApp app = SportActivityApp.getInstance();
    final ObservableList<Activity> userActivities = FXCollections.observableArrayList();
    final ObservableList<Session> userSessions = FXCollections.observableArrayList();
    final BooleanProperty loggedInProperty = new SimpleBooleanProperty(false);

    @FXML
    private BorderPane rootPane;
    @FXML
    private Label viewsTitleLabel;
    @FXML
    private Button navActivitiesButton;
    @FXML
    private Button navProfileButton;
    @FXML
    private Button navSessionsButton;
    @FXML
    private Button navMapsButton;

    private Parent topBarView;
    private TopBarController topBarController;

    private Parent activitiesView;
    private Parent profileView;
    private Parent sessionsView;
    private Parent mapsView;

    private ActivitiesController activitiesController;
    private ProfileController profileController;
    private SessionsController sessionsController;
    private MapsController mapsController;
    private Image defaultAvatarImage;
    private Locale currentLocale = SPANISH_LOCALE;
    private ResourceBundle texts = loadTexts(currentLocale);

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadApplicationViews();
        rootPane.setTop(topBarView);
        topBarController.setMainController(this);
        showActivitiesView();
        configureControls();
        applyLanguage();
        refreshAllData();
    }

    private void loadApplicationViews() {
        try {
            FXMLLoader topBarLoader = new FXMLLoader(getClass().getResource("../views/TopBar.fxml"));
            topBarView = topBarLoader.load();
            topBarController = topBarLoader.getController();

            FXMLLoader activitiesLoader = new FXMLLoader(getClass().getResource("../views/ActivitiesView.fxml"));
            activitiesView = activitiesLoader.load();
            activitiesController = activitiesLoader.getController();
            activitiesController.setMainController(this);

            FXMLLoader profileLoader = new FXMLLoader(getClass().getResource("../views/ProfileView.fxml"));
            profileView = profileLoader.load();
            profileController = profileLoader.getController();
            profileController.setMainController(this);

            FXMLLoader sessionsLoader = new FXMLLoader(getClass().getResource("../views/SessionsView.fxml"));
            sessionsView = sessionsLoader.load();
            sessionsController = sessionsLoader.getController();
            sessionsController.setMainController(this);

            FXMLLoader mapsLoader = new FXMLLoader(getClass().getResource("../views/MapsView.fxml"));
            mapsView = mapsLoader.load();
            mapsController = mapsLoader.getController();
            mapsController.setMainController(this);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudieron cargar las vistas de la aplicación.", ex);
        }
    }

    @FXML
    private void showActivitiesView() {
        rootPane.setCenter(activitiesView);
        markNavigation(navActivitiesButton);
    }

    @FXML
    private void showProfileView() {
        rootPane.setCenter(profileView);
        markNavigation(navProfileButton);
    }

    @FXML
    private void showSessionsView() {
        rootPane.setCenter(sessionsView);
        markNavigation(navSessionsButton);
    }

    @FXML
    private void showMapsView() {
        rootPane.setCenter(mapsView);
        markNavigation(navMapsButton);
    }

    private void markNavigation(Button selectedButton) {
        for (Button button : List.of(navActivitiesButton, navProfileButton, navSessionsButton, navMapsButton)) {
            button.getStyleClass().remove("primary-button");
            if (!button.getStyleClass().contains("secondary-button")) {
                button.getStyleClass().add("secondary-button");
            }
        }
        selectedButton.getStyleClass().remove("secondary-button");
        if (!selectedButton.getStyleClass().contains("primary-button")) {
            selectedButton.getStyleClass().add("primary-button");
        }
    }

    Window getOwnerWindow() {
        return rootPane == null || rootPane.getScene() == null ? null : rootPane.getScene().getWindow();
    }

    private void configureControls() {
        topBarController.configureLanguageSelector();
        topBarController.configureMenuAccelerators();
        setupStateBindings();
        setupTooltips();
        setupInlineErrorLabels();
        setupDatePicker(profileController.profileBirthDatePicker);
    }

    // IA: centraliza atajos globales para usuarios expertos sin duplicar logica de botones o menus.
    public void installKeyboardShortcuts(Scene scene) {
        if (scene == null) {
            return;
        }

        scene.getAccelerators().put(shortcut(KeyCode.DIGIT1), () -> showViewFromShortcut(navActivitiesButton, this::showActivitiesView));
        scene.getAccelerators().put(shortcut(KeyCode.NUMPAD1), () -> showViewFromShortcut(navActivitiesButton, this::showActivitiesView));
        scene.getAccelerators().put(shortcut(KeyCode.DIGIT2), () -> showViewFromShortcut(navProfileButton, this::showProfileView));
        scene.getAccelerators().put(shortcut(KeyCode.NUMPAD2), () -> showViewFromShortcut(navProfileButton, this::showProfileView));
        scene.getAccelerators().put(shortcut(KeyCode.DIGIT3), () -> showViewFromShortcut(navSessionsButton, this::showSessionsView));
        scene.getAccelerators().put(shortcut(KeyCode.NUMPAD3), () -> showViewFromShortcut(navSessionsButton, this::showSessionsView));
        scene.getAccelerators().put(shortcut(KeyCode.DIGIT4), () -> showViewFromShortcut(navMapsButton, this::showMapsView));
        scene.getAccelerators().put(shortcut(KeyCode.NUMPAD4), () -> showViewFromShortcut(navMapsButton, this::showMapsView));
        scene.getAccelerators().put(shortcut(KeyCode.L), () -> topBarController.requestLogin());
        scene.getAccelerators().put(shortcut(KeyCode.R), () -> topBarController.requestRegister());
        scene.getAccelerators().put(shortcut(KeyCode.S), this::saveProfileFromShortcut);
    }

    private KeyCodeCombination shortcut(KeyCode code) {
        return new KeyCodeCombination(code, KeyCombination.SHORTCUT_DOWN);
    }

    private void showViewFromShortcut(Button navigationButton, Runnable action) {
        if (!navigationButton.isDisabled()) {
            action.run();
        }
    }

    private void saveProfileFromShortcut() {
        if (rootPane.getCenter() == profileView && !profileController.saveProfileButton.isDisabled()) {
            profileController.onSaveProfile();
        }
    }

    private void setupStateBindings() {
        BooleanBinding notLoggedIn = Bindings.not(loggedInProperty);
        BooleanBinding noActivitySelected = activitiesController.activityListView.getSelectionModel().selectedItemProperty().isNull();
        BooleanBinding noAnnotationSelected = activitiesController.annotationListView.getSelectionModel().selectedItemProperty().isNull();
        BooleanBinding noMapSelected = mapsController.mapRegionTableView.getSelectionModel().selectedItemProperty().isNull();

        topBarController.loginButton.disableProperty().bind(loggedInProperty);
        topBarController.registerButton.disableProperty().bind(loggedInProperty);
        topBarController.logoutButton.disableProperty().bind(notLoggedIn);
        topBarController.logoutMenuItem.disableProperty().bind(notLoggedIn);

        navProfileButton.disableProperty().bind(notLoggedIn);
        navSessionsButton.disableProperty().bind(notLoggedIn);

        activitiesController.importActivityButton.disableProperty().bind(notLoggedIn);
        topBarController.importActivityMenuItem.disableProperty().bind(notLoggedIn);
        activitiesController.renameActivityButton.disableProperty().bind(notLoggedIn.or(noActivitySelected));
        topBarController.renameActivityMenuItem.disableProperty().bind(notLoggedIn.or(noActivitySelected));
        activitiesController.removeActivityButton.disableProperty().bind(notLoggedIn.or(noActivitySelected));
        topBarController.removeActivityMenuItem.disableProperty().bind(notLoggedIn.or(noActivitySelected));
        activitiesController.speedOverlayCheckBox.disableProperty().bind(notLoggedIn.or(noActivitySelected));
        activitiesController.zoomSlider.disableProperty().bind(notLoggedIn.or(noActivitySelected));
        activitiesController.zoomInButton.disableProperty().bind(notLoggedIn.or(noActivitySelected));
        activitiesController.zoomOutButton.disableProperty().bind(notLoggedIn.or(noActivitySelected));
        activitiesController.monthComboBox.disableProperty().bind(notLoggedIn);

        activitiesController.changeAnnotationColorButton.disableProperty().bind(notLoggedIn.or(noAnnotationSelected));
        activitiesController.removeAnnotationButton.disableProperty().bind(notLoggedIn.or(noAnnotationSelected));

        profileController.profileEmailField.disableProperty().bind(notLoggedIn);
        profileController.profilePasswordField.disableProperty().bind(notLoggedIn);
        profileController.profileBirthDatePicker.disableProperty().bind(notLoggedIn);
        profileController.profileAvatarField.disableProperty().bind(notLoggedIn);
        profileController.browseAvatarButton.disableProperty().bind(notLoggedIn);
        profileController.saveProfileButton.disableProperty().bind(notLoggedIn);

        mapsController.addMapRegionButton.disableProperty().bind(notLoggedIn);
        mapsController.browseMapImageButton.disableProperty().bind(notLoggedIn);
        mapsController.removeMapRegionButton.disableProperty().bind(notLoggedIn.or(noMapSelected));
    }

    private void setupTooltips() {
        mapsController.addMapRegionButton.setTooltip(new Tooltip(tr("tooltip.addMap")));
        mapsController.removeMapRegionButton.setTooltip(new Tooltip(tr("tooltip.removeMap")));
    }

    List<LanguageOption> getLanguageOptions() {
        return List.of(
                new LanguageOption("es", tr("language.es"), SPANISH_LOCALE),
                new LanguageOption("va", tr("language.va"), VALENCIAN_LOCALE)
        );
    }

    LanguageOption getCurrentLanguageOption() {
        String currentCode = currentLocale.equals(VALENCIAN_LOCALE) ? "va" : "es";
        for (LanguageOption option : getLanguageOptions()) {
            if (option.code.equals(currentCode)) {
                return option;
            }
        }
        return getLanguageOptions().get(0);
    }

    void setLanguage(LanguageOption option) {
        if (option == null || option.locale.equals(currentLocale)) {
            return;
        }

        currentLocale = option.locale;
        texts = loadTexts(currentLocale);
        applyLanguage();
        refreshAllData();
    }

    String tr(String key) {
        return texts.containsKey(key) ? texts.getString(key) : key;
    }

    String trf(String key, Object... args) {
        return String.format(tr(key), args);
    }

    Locale getCurrentLocale() {
        return currentLocale;
    }

    private static ResourceBundle loadTexts(Locale locale) {
        if (VALENCIAN_LOCALE.equals(locale)) {
            return ResourceBundle.getBundle("resources.messages_va", Locale.ROOT);
        }
        return ResourceBundle.getBundle("resources.messages_es", Locale.ROOT);
    }

    private void applyLanguage() {
        viewsTitleLabel.setText(tr("nav.views"));
        navActivitiesButton.setText(tr("nav.activities"));
        navProfileButton.setText(tr("nav.profile"));
        navSessionsButton.setText(tr("nav.sessions"));
        navMapsButton.setText(tr("nav.maps"));

        topBarController.applyLanguage();
        activitiesController.applyLanguage();
        profileController.applyLanguage();
        sessionsController.applyLanguage();
        mapsController.applyLanguage();
        setupTooltips();
        setStatus(null);
    }

    private void setupInlineErrorLabels() {
        hideInlineErrors(
                profileController.profileEmailErrorLabel,
                profileController.profilePasswordErrorLabel,
                profileController.profileBirthDateErrorLabel,
                profileController.profileAvatarErrorLabel,
                mapsController.mapNameErrorLabel,
                mapsController.mapImageErrorLabel,
                mapsController.mapBoundsErrorLabel
        );
    }

    void refreshAllData() {
        User currentUser = app.getCurrentUser();
        boolean loggedIn = currentUser != null;

        loggedInProperty.set(loggedIn);
        topBarController.currentUserLabel.setText(loggedIn ? currentUser.getNickName() : tr("user.unauthenticated"));
        updateAvatarImages(currentUser);
        refreshProfileSection();
        refreshMapRegions();

        if (!loggedIn) {
            showActivitiesView();
            userActivities.clear();
            userSessions.clear();
            activitiesController.clearForLoggedOut();
            sessionsController.updateTotals(List.of());
            hideProfileErrors();
            hideMapErrors();
            setStatus(tr("status.notLogged"));
            return;
        }

        updateActivityListAndSelection(null, false);
        sessionsController.refresh();
        setStatus(trf("status.dataLoaded", currentUser.getNickName()));
    }

    void refreshProfileSection() {
        profileController.refresh();
    }

    void refreshMapRegions() {
        mapsController.refresh();
    }

    void updateActivityListAndSelection(Long activityIdToSelect, boolean countAsView) {
        activitiesController.updateActivityListAndSelection(activityIdToSelect, countAsView);
    }

    public void onImportActivity() {
        activitiesController.onImportActivity();
    }

    public void onRenameActivity() {
        activitiesController.onRenameActivity();
    }

    public void onRemoveActivity() {
        activitiesController.onRemoveActivity();
    }

    public void shutdown() {
        logoutCurrentSession();
    }

    void logoutCurrentSession() {
        if (app.getCurrentUser() == null) {
            return;
        }

        try {
            app.logout();
        } catch (RuntimeException ex) {
            showError(tr("error.saveSession"));
        } finally {
            loggedInProperty.set(false);
        }
    }

    void configureDialogPane(Dialog<?> dialog, double width) {
        dialog.setResizable(false);
        dialog.getDialogPane().setMinWidth(width);
        dialog.getDialogPane().setPrefWidth(width);

        if (getOwnerWindow() != null) {
            dialog.initOwner(getOwnerWindow());
        }

        URL stylesheet = getClass().getResource("/resources/estilos.css");
        if (stylesheet != null && !dialog.getDialogPane().getStylesheets().contains(stylesheet.toExternalForm())) {
            dialog.getDialogPane().getStylesheets().add(stylesheet.toExternalForm());
        }
    }

    void configureDialogGrid(GridPane grid, double labelWidth, double fieldWidth, boolean includeActionColumn) {
        if (!grid.getStyleClass().contains("dialog-form")) {
            grid.getStyleClass().add("dialog-form");
        }

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(labelWidth);
        labelColumn.setPrefWidth(labelWidth);
        labelColumn.setHalignment(HPos.RIGHT);

        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setMinWidth(fieldWidth);
        fieldColumn.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().setAll(labelColumn, fieldColumn);
        if (includeActionColumn) {
            ColumnConstraints actionColumn = new ColumnConstraints();
            actionColumn.setMinWidth(42.0);
            grid.getColumnConstraints().add(actionColumn);
        }
    }

    void growDialogFields(Node... nodes) {
        for (Node node : nodes) {
            GridPane.setHgrow(node, Priority.ALWAYS);
            if (node instanceof TextField) {
                ((TextField) node).setMaxWidth(Double.MAX_VALUE);
            } else if (node instanceof DatePicker) {
                ((DatePicker) node).setMaxWidth(Double.MAX_VALUE);
            } else if (node instanceof StackPane) {
                ((StackPane) node).setMaxWidth(Double.MAX_VALUE);
            }
        }
    }

    void setPasswordErrorState(PasswordField passwordField, TextField previewField, boolean error) {
        setErrorState(passwordField, error);
        setErrorState(previewField, error);
    }

    Label createInlineErrorLabel() {
        Label label = new Label();
        label.getStyleClass().add("field-error");
        label.setWrapText(true);
        label.setVisible(false);
        label.setMinHeight(22.0);
        label.setPrefHeight(44.0);
        label.setMaxHeight(72.0);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    void showFieldError(boolean isValid, Node field, Label errorLabel, String errorMessage) {
        errorLabel.setText(errorMessage);
        errorLabel.setVisible(!isValid);
        setErrorState(field, !isValid);
    }

    void showInlineError(Label label, Node field, String errorMessage) {
        label.setText(errorMessage);
        label.setVisible(true);
        setErrorState(field, true);
    }

    void hideInlineErrors(Label... labels) {
        for (Label label : labels) {
            if (label == null) {
                continue;
            }
            if (!label.managedProperty().isBound()) {
                label.managedProperty().bind(label.visibleProperty());
            }
            label.setText("");
            label.setVisible(false);
        }
    }

    void hideProfileErrors() {
        profileController.hideErrors();
    }

    void hideMapErrors() {
        mapsController.hideErrors();
    }

    void setErrorState(Node field, boolean error) {
        if (field != null) {
            field.pseudoClassStateChanged(ERROR_PSEUDO_CLASS, error);
        }
    }

    void setupDatePicker(DatePicker picker) {
        picker.setPromptText("dd/mm/aaaa");
        picker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : DATE_FORMATTER.format(date);
            }

            @Override
            public LocalDate fromString(String value) {
                String text = safeTrim(value);
                if (text.isEmpty()) {
                    return null;
                }
                try {
                    return LocalDate.parse(text, DATE_FORMATTER);
                } catch (DateTimeParseException ex) {
                    return null;
                }
            }
        });
    }

    boolean requireLogin() {
        if (app.getCurrentUser() != null) {
            return true;
        }

        showError(tr("error.loginRequired"));
        return false;
    }

    void setStatus(String message) {
        String prefix = tr("status.prefix");
        if (message == null || message.isBlank()) {
            topBarController.statusLabel.setText(prefix + " " + tr("status.ready"));
            return;
        }
        topBarController.statusLabel.setText(message.startsWith(prefix) ? message : prefix + " " + message);
    }

    void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(tr("error.operationNotCompleted"));
        alert.setContentText(message);
        alert.showAndWait();
    }

    String formatDateTime(java.time.LocalDateTime value) {
        return value == null ? "-" : DATE_TIME_FORMATTER.format(value);
    }

    String formatDuration(Duration duration) {
        if (duration == null) {
            return "-";
        }

        long totalSeconds = Math.max(0, duration.getSeconds());
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    String formatPace(double minutesPerKm) {
        if (!Double.isFinite(minutesPerKm) || minutesPerKm <= 0) {
            return "-";
        }

        int totalSeconds = (int) Math.round(minutesPerKm * 60.0);
        int min = totalSeconds / 60;
        int sec = totalSeconds % 60;
        return String.format("%d:%02d min/km", min, sec);
    }

    boolean isValidHexColor(String color) {
        return color != null && color.matches("^#[0-9A-Fa-f]{6}$");
    }

    Double parseDoubleValue(String value) {
        String normalized = safeTrim(value).replace(',', '.');
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(normalized);
        } catch (Exception ex) {
            return null;
        }
    }

    boolean isValidLatitude(double value) {
        return value >= -90.0 && value <= 90.0;
    }

    boolean isValidLongitude(double value) {
        return value >= -180.0 && value <= 180.0;
    }

    String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    void updateAvatarImages(User user) {
        Image avatar = getUserAvatarOrDefault(user);
        topBarController.currentAvatarImageView.setImage(avatar);
        profileController.profileAvatarImageView.setImage(avatar);
    }

    void updateAvatarPreviewFromPath(String avatarPath) {
        File avatarFile = new File(safeTrim(avatarPath));
        if (!avatarFile.exists()) {
            profileController.profileAvatarImageView.setImage(getDefaultAvatarImage());
            return;
        }

        Image avatar = new Image(avatarFile.toURI().toString());
        profileController.profileAvatarImageView.setImage(avatar.isError() ? getDefaultAvatarImage() : avatar);
    }

    Image getUserAvatarOrDefault(User user) {
        if (user != null) {
            try {
                Image avatar = user.getAvatar();
                if (avatar != null && !avatar.isError()) {
                    return avatar;
                }
            } catch (RuntimeException ex) {
                // Use the application logo if the stored avatar path cannot be loaded.
            }
        }
        return getDefaultAvatarImage();
    }

    Image getDefaultAvatarImage() {
        if (defaultAvatarImage == null) {
            defaultAvatarImage = new Image(getClass().getResourceAsStream("/resources/logo.png"));
        }
        return defaultAvatarImage;
    }

    File resolveMapFile(String relativePath) {
        File file = new File(relativePath);
        if (file.exists()) {
            return file;
        }

        File parentCandidate = new File("..", relativePath);
        if (parentCandidate.exists()) {
            return parentCandidate;
        }

        return file;
    }

    static final class LanguageOption {
        private final String code;
        private final String name;
        private final Locale locale;

        private LanguageOption(String code, String name, Locale locale) {
            this.code = code;
            this.name = name;
            this.locale = locale;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof LanguageOption)) {
                return false;
            }

            LanguageOption other = (LanguageOption) obj;
            return Objects.equals(code, other.code);
        }

        @Override
        public int hashCode() {
            return Objects.hash(code);
        }
    }
}
