package mapademo.controllers;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import upv.ipc.sportlib.MapRegion;

/*
 * Nota de autoria: el equipo ha usado IA como apoyo en la estructura general
 * del controlador. Los metodos marcados con "IA:" identifican fragmentos
 * integrados integramente con ayuda de IA por su complejidad tecnica.
 */
public class MapsController implements Initializable {
    private MainLayoutController mainController;

    @FXML TableView<MapRegion> mapRegionTableView;
    @FXML TableColumn<MapRegion, String> mapNameColumn;
    @FXML TableColumn<MapRegion, String> mapImageColumn;
    @FXML TableColumn<MapRegion, String> mapBoundsColumn;
    @FXML Label mapsTitleLabel;
    @FXML Label addMapTitleLabel;
    @FXML Label mapNameTextLabel;
    @FXML TextField mapNameField;
    @FXML Label mapImageTextLabel;
    @FXML TextField mapImagePathField;
    @FXML Label mapLatMinTextLabel;
    @FXML TextField mapLatMinField;
    @FXML Label mapLatMaxTextLabel;
    @FXML TextField mapLatMaxField;
    @FXML Label mapLonMinTextLabel;
    @FXML TextField mapLonMinField;
    @FXML Label mapLonMaxTextLabel;
    @FXML TextField mapLonMaxField;
    @FXML Button refreshMapsButton;
    @FXML Button addMapRegionButton;
    @FXML Button removeMapRegionButton;
    @FXML Button browseMapImageButton;
    @FXML Label mapNameErrorLabel;
    @FXML Label mapImageErrorLabel;
    @FXML Label mapBoundsErrorLabel;
    @FXML Label mapHintLabel;
    private File selectedMapImageFile;

    void setMainController(MainLayoutController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mapRegionTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        mapNameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        mapImageColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getImagePath()));
        mapBoundsColumn.setCellValueFactory(data -> {
            MapRegion region = data.getValue();
            String bounds = String.format("lat[%.6f, %.6f] lon[%.6f, %.6f]", region.getLatMin(), region.getLatMax(), region.getLonMin(), region.getLonMax());
            return new ReadOnlyStringWrapper(bounds);
        });
    }

    void applyLanguage() {
        mapsTitleLabel.setText(mainController.tr("maps.title"));
        mapNameColumn.setText(mainController.tr("maps.name"));
        mapImageColumn.setText(mainController.tr("maps.image"));
        mapBoundsColumn.setText(mainController.tr("maps.bounds"));
        refreshMapsButton.setText(mainController.tr("button.refresh"));
        removeMapRegionButton.setText(mainController.tr("button.removeUnusedMap"));
        addMapTitleLabel.setText(mainController.tr("maps.addTitle"));
        mapNameTextLabel.setText(mainController.tr("maps.nameRequired"));
        mapImageTextLabel.setText(mainController.tr("maps.imageRequired"));
        mapLatMinTextLabel.setText(mainController.tr("maps.latMin"));
        mapLatMaxTextLabel.setText(mainController.tr("maps.latMax"));
        mapLonMinTextLabel.setText(mainController.tr("maps.lonMin"));
        mapLonMaxTextLabel.setText(mainController.tr("maps.lonMax"));
        addMapRegionButton.setText(mainController.tr("button.addMap"));
        mapHintLabel.setText(mainController.tr("maps.hint"));
    }

    void refresh() {
        List<MapRegion> regions = mainController.app.getMapRegions();
        mapRegionTableView.getItems().setAll(regions);
    }

    void hideErrors() {
        mainController.hideInlineErrors(mapNameErrorLabel, mapImageErrorLabel, mapBoundsErrorLabel);
        mainController.setErrorState(mapNameField, false);
        mainController.setErrorState(mapImagePathField, false);
        mainController.setErrorState(mapLatMinField, false);
        mainController.setErrorState(mapLatMaxField, false);
        mainController.setErrorState(mapLonMinField, false);
        mainController.setErrorState(mapLonMaxField, false);
    }

    private void markBoundsError(String message) {
        mapBoundsErrorLabel.setText(message);
        mapBoundsErrorLabel.setVisible(true);
        mainController.setErrorState(mapLatMinField, true);
        mainController.setErrorState(mapLatMaxField, true);
        mainController.setErrorState(mapLonMinField, true);
        mainController.setErrorState(mapLonMaxField, true);
        mainController.setStatus(mainController.tr("status.mapBoundsInvalid"));
    }

    @FXML
    void onRefreshMaps() {
        refresh();
        mainController.setStatus(mainController.tr("status.mapsRefreshed"));
    }

    @FXML
    void onBrowseMapImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(mainController.tr("dialog.mapImageTitle"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(mainController.tr("dialog.jpgFilter"), "*.jpg", "*.jpeg"));

        File selected = chooser.showOpenDialog(mainController.getOwnerWindow());
        if (selected != null) {
            selectedMapImageFile = selected;
            mapImagePathField.setText(selected.getAbsolutePath());
            mainController.hideInlineErrors(mapImageErrorLabel);
            mainController.setErrorState(mapImagePathField, false);
        }
    }

    @FXML
    // IA: valida coordenadas e imagen antes de registrar una nueva region cartografica.
    void onAddMapRegion() {
        if (!mainController.requireLogin()) {
            return;
        }

        hideErrors();

        String name = mainController.safeTrim(mapNameField.getText());
        if (name.isEmpty()) {
            mainController.showInlineError(mapNameErrorLabel, mapNameField, mainController.tr("error.mapNameRequired"));
            return;
        }

        String imagePath = mainController.safeTrim(mapImagePathField.getText());
        File imageFile = imagePath.isEmpty() ? selectedMapImageFile : new File(imagePath);
        if (imageFile == null && selectedMapImageFile != null) {
            imageFile = selectedMapImageFile;
        }

        if (imageFile == null || !imageFile.exists()) {
            mainController.showInlineError(mapImageErrorLabel, mapImagePathField, mainController.tr("error.mapImageRequired"));
            return;
        }

        Double latMin = mainController.parseDoubleValue(mapLatMinField.getText());
        Double latMax = mainController.parseDoubleValue(mapLatMaxField.getText());
        Double lonMin = mainController.parseDoubleValue(mapLonMinField.getText());
        Double lonMax = mainController.parseDoubleValue(mapLonMaxField.getText());

        if (latMin == null || latMax == null || lonMin == null || lonMax == null) {
            markBoundsError(mainController.tr("error.mapBoundsNumbers"));
            return;
        }

        if (!mainController.isValidLatitude(latMin) || !mainController.isValidLatitude(latMax)
                || !mainController.isValidLongitude(lonMin) || !mainController.isValidLongitude(lonMax)) {
            markBoundsError(mainController.tr("error.mapBoundsRange"));
            return;
        }

        if (latMin >= latMax || lonMin >= lonMax) {
            markBoundsError(mainController.tr("error.mapBoundsOrder"));
            return;
        }

        MapRegion region;
        try {
            region = mainController.app.addMapRegion(name, imageFile, latMin, latMax, lonMin, lonMax);
        } catch (RuntimeException ex) {
            mainController.showError(mainController.tr("error.mapPersistence"));
            return;
        }
        if (region == null) {
            mainController.showError(mainController.tr("error.mapGeneric"));
            return;
        }

        mapNameField.clear();
        mapImagePathField.clear();
        mapLatMinField.clear();
        mapLatMaxField.clear();
        mapLonMinField.clear();
        mapLonMaxField.clear();
        selectedMapImageFile = null;

        refresh();
        mainController.setStatus(mainController.trf("status.mapAdded", region.getName()));
    }

    @FXML
    void onRemoveMapRegion() {
        if (!mainController.requireLogin()) {
            return;
        }

        MapRegion selected = mapRegionTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            mainController.showError(mainController.tr("error.selectMap"));
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(mainController.tr("dialog.removeMap.title"));
        confirm.setHeaderText(mainController.tr("dialog.removeMap.header"));
        confirm.setContentText(mainController.trf("dialog.removeMap.content", selected.getName()));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        boolean removed;
        try {
            removed = mainController.app.removeMapRegion(selected);
        } catch (RuntimeException ex) {
            mainController.showError(mainController.tr("error.removeMapPersistence"));
            return;
        }
        if (!removed) {
            mainController.showError(mainController.tr("error.removeMapGeneric"));
            return;
        }

        refresh();
        mainController.setStatus(mainController.trf("status.mapRemoved", selected.getName()));
    }
}


