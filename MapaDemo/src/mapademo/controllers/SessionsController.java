package mapademo.controllers;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import upv.ipc.sportlib.Session;
import upv.ipc.sportlib.User;

/*
 * Nota de autoria: el equipo ha usado IA como apoyo en la estructura general
 * del controlador. Los metodos marcados con "IA:" identifican fragmentos
 * integrados integramente con ayuda de IA por su complejidad tecnica.
 */
public class SessionsController implements Initializable {
    private MainLayoutController mainController;

    @FXML TableView<Session> sessionTableView;
    @FXML TableColumn<Session, String> sessionStartColumn;
    @FXML TableColumn<Session, String> sessionEndColumn;
    @FXML TableColumn<Session, String> sessionDurationColumn;
    @FXML TableColumn<Session, String> sessionImportedColumn;
    @FXML TableColumn<Session, String> sessionViewedColumn;
    @FXML TableColumn<Session, String> sessionAnnotationsColumn;
    @FXML Label sessionTotalCountLabel;
    @FXML Label sessionTotalDurationLabel;
    @FXML Label sessionTotalImportedLabel;
    @FXML Label sessionTotalViewedLabel;
    @FXML Label sessionTotalAnnotationsLabel;

    void setMainController(MainLayoutController mainController) {
        this.mainController = mainController;
        sessionTableView.setItems(mainController.userSessions);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        sessionStartColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(mainController.formatDateTime(data.getValue().getStartTime())));
        sessionEndColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(mainController.formatDateTime(data.getValue().getEndTime())));
        sessionDurationColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(mainController.formatDuration(data.getValue().getDuration())));
        sessionImportedColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getImportedActivities())));
        sessionViewedColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getViewedActivities())));
        sessionAnnotationsColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getAnnotationsCreated())));
    }

    void applyLanguage() {
        sessionStartColumn.setText(mainController.tr("sessions.start"));
        sessionEndColumn.setText(mainController.tr("sessions.end"));
        sessionDurationColumn.setText(mainController.tr("sessions.duration"));
        sessionImportedColumn.setText(mainController.tr("sessions.imported"));
        sessionViewedColumn.setText(mainController.tr("sessions.viewed"));
        sessionAnnotationsColumn.setText(mainController.tr("sessions.annotations"));
        updateTotals(mainController.userSessions);
    }

    void refresh() {
        User currentUser = mainController.app.getCurrentUser();
        if (currentUser == null) {
            mainController.userSessions.clear();
            updateTotals(List.of());
            return;
        }

        List<Session> sessions = mainController.app.getSessionsByUser(currentUser);
        mainController.userSessions.setAll(sessions);
        updateTotals(sessions);
    }

    // IA: resume la actividad de sesiones acumulando duracion, importaciones, vistas y anotaciones.
    void updateTotals(List<Session> sessions) {
        Duration totalDuration = Duration.ZERO;
        int imported = 0;
        int viewed = 0;
        int annotations = 0;

        for (Session session : sessions) {
            totalDuration = totalDuration.plus(session.getDuration());
            imported += session.getImportedActivities();
            viewed += session.getViewedActivities();
            annotations += session.getAnnotationsCreated();
        }

        sessionTotalCountLabel.setText(mainController.trf("sessions.totalCount", sessions.size()));
        sessionTotalDurationLabel.setText(mainController.trf("sessions.totalDuration", mainController.formatDuration(totalDuration)));
        sessionTotalImportedLabel.setText(mainController.trf("sessions.totalImported", imported));
        sessionTotalViewedLabel.setText(mainController.trf("sessions.totalViewed", viewed));
        sessionTotalAnnotationsLabel.setText(mainController.trf("sessions.totalAnnotations", annotations));
    }
}

