package mapademo.controllers;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.Annotation;
import upv.ipc.sportlib.AnnotationType;
import upv.ipc.sportlib.GeoPoint;
import upv.ipc.sportlib.MapProjection;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.TrackPoint;

/*
 * Nota de autoria: el equipo ha usado IA como apoyo en la estructura general
 * del controlador. Los metodos marcados con "IA:" identifican fragmentos
 * integrados integramente con ayuda de IA por su complejidad tecnica.
 */
public class ActivitiesController implements Initializable {
    private static final double ZOOM_STEP = 0.1;

    private MainLayoutController mainController;
    private final ContextMenu mapContextMenu = new ContextMenu();

    private Group zoomGroup;
    private Group speedLegendGroup;
    private Pane mapPane;
    private Activity currentActivity;
    private MapRegion currentMapRegion;
    private MapProjection currentProjection;
    private Circle elevationHoverMarker;
    private Circle pendingAnnotationMarker;
    private Text pendingAnnotationHint;

    private Point2D contextClickPoint;
    private AnnotationType pendingAnnotationType;
    private GeoPoint pendingFirstPoint;
    private MenuItem pointAnnotationMenuItem;
    private MenuItem textAnnotationMenuItem;
    private MenuItem lineAnnotationMenuItem;
    private MenuItem circleAnnotationMenuItem;

    private boolean suppressActivitySelectionListener = false;

    private List<TrackPoint> chartTrackPoints = List.of();
    private List<Point2D> projectedTrackPoints = List.of();
    private List<Double> chartDistanceKm = List.of();

    @FXML ListView<Activity> activityListView;
    @FXML Label activitiesTitleLabel;
    @FXML Button importActivityButton;
    @FXML Button renameActivityButton;
    @FXML Button removeActivityButton;
    @FXML Label monthlyTitleLabel;
    @FXML ComboBox<YearMonth> monthComboBox;
    @FXML Label monthDistanceTextLabel;
    @FXML Label monthDistanceLabel;
    @FXML Label monthDurationTextLabel;
    @FXML Label monthDurationLabel;
    @FXML Label monthGainTextLabel;
    @FXML Label monthGainLabel;
    @FXML Label monthLossTextLabel;
    @FXML Label monthLossLabel;
    @FXML CheckBox speedOverlayCheckBox;
    @FXML Label annotationsHintLabel;
    @FXML Label zoomTextLabel;
    @FXML Slider zoomSlider;
    @FXML Button zoomInButton;
    @FXML Button zoomOutButton;
    @FXML ScrollPane mapScrollPane;
    @FXML LineChart<Number, Number> elevationChart;
    @FXML NumberAxis distanceAxis;
    @FXML NumberAxis elevationAxis;
    @FXML Label activityStatsTitleLabel;
    @FXML Label statNameTextLabel;
    @FXML Label statNameLabel;
    @FXML Label statDistanceTextLabel;
    @FXML Label statDistanceLabel;
    @FXML Label statDurationTextLabel;
    @FXML Label statDurationLabel;
    @FXML Label statSpeedTextLabel;
    @FXML Label statSpeedLabel;
    @FXML Label statPaceTextLabel;
    @FXML Label statPaceLabel;
    @FXML Label statGainTextLabel;
    @FXML Label statGainLabel;
    @FXML Label statLossTextLabel;
    @FXML Label statLossLabel;
    @FXML Label statMinElevTextLabel;
    @FXML Label statMinElevLabel;
    @FXML Label statMaxElevTextLabel;
    @FXML Label statMaxElevLabel;
    @FXML Label annotationsTitleLabel;
    @FXML ListView<Annotation> annotationListView;
    @FXML Button changeAnnotationColorButton;
    @FXML Button removeAnnotationButton;

    void setMainController(MainLayoutController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configureActivityList();
        activityListView.getSelectionModel().selectedItemProperty().addListener((obs, oldActivity, newActivity) -> {
            if (!suppressActivitySelectionListener) {
                renderActivity(newActivity, true);
            }
        });
        zoomSlider.valueProperty().addListener((obs, oldV, newV) -> applyZoom(newV.doubleValue()));
        setupMonthComboBox();
        setupChartInteraction();
        setupMapContextMenu();
        setupMapCanvas();
    }

    void applyLanguage() {
        activitiesTitleLabel.setText(mainController.tr("activities.title"));
        importActivityButton.setText(mainController.tr("button.importGpx"));
        renameActivityButton.setText(mainController.tr("button.rename"));
        removeActivityButton.setText(mainController.tr("button.delete"));
        monthlyTitleLabel.setText(mainController.tr("activities.monthly"));
        monthDistanceTextLabel.setText(mainController.tr("activities.distance"));
        monthDurationTextLabel.setText(mainController.tr("activities.time"));
        monthGainTextLabel.setText(mainController.tr("activities.gain"));
        monthLossTextLabel.setText(mainController.tr("activities.loss"));
        speedOverlayCheckBox.setText(mainController.tr("activities.speedOverlay"));
        annotationsHintLabel.setText(mainController.tr("activities.annotationsHint"));
        zoomTextLabel.setText(mainController.tr("activities.zoom"));
        elevationChart.setTitle(mainController.tr("activities.chartTitle"));
        distanceAxis.setLabel(mainController.tr("activities.distanceAxis"));
        elevationAxis.setLabel(mainController.tr("activities.elevationAxis"));
        activityStatsTitleLabel.setText(mainController.tr("activities.stats"));
        statNameTextLabel.setText(mainController.tr("activities.name"));
        statDistanceTextLabel.setText(mainController.tr("activities.distance"));
        statDurationTextLabel.setText(mainController.tr("activities.duration"));
        statSpeedTextLabel.setText(mainController.tr("activities.averageSpeed"));
        statPaceTextLabel.setText(mainController.tr("activities.averagePace"));
        statGainTextLabel.setText(mainController.tr("activities.elevationGain"));
        statLossTextLabel.setText(mainController.tr("activities.elevationLoss"));
        statMinElevTextLabel.setText(mainController.tr("activities.minElevation"));
        statMaxElevTextLabel.setText(mainController.tr("activities.maxElevation"));
        annotationsTitleLabel.setText(mainController.tr("activities.annotations"));
        changeAnnotationColorButton.setText(mainController.tr("button.changeColor"));
        removeAnnotationButton.setText(mainController.tr("button.remove"));

        if (pointAnnotationMenuItem != null) {
            pointAnnotationMenuItem.setText(mainController.tr("annotation.point"));
            textAnnotationMenuItem.setText(mainController.tr("annotation.text"));
            lineAnnotationMenuItem.setText(mainController.tr("annotation.line"));
            circleAnnotationMenuItem.setText(mainController.tr("annotation.circle"));
        }

        monthComboBox.setButtonCell(monthComboBox.getCellFactory().call(null));
        activityListView.refresh();
        annotationListView.refresh();
        setupTooltips();
    }

    void configureActivityList() {
        activityListView.setCellFactory(list -> new ListCell<Activity>() {
            @Override
            protected void updateItem(Activity item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String start = item.getStartTime() == null
                            ? mainController.tr("activities.noDate")
                            : MainLayoutController.DATE_TIME_FORMATTER.format(item.getStartTime());
                    setText(item.getName() + " - " + start);
                }
            }
        });

        annotationListView.setCellFactory(list -> new ListCell<Annotation>() {
            @Override
            protected void updateItem(Annotation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String text = item.getText() == null || item.getText().isBlank() ? mainController.tr("activities.noText") : item.getText();
                    setText(annotationTypeText(item.getType()) + " - " + text + " - " + item.getColor());
                }
            }
        });
    }

    private void setupMonthComboBox() {
        monthComboBox.setCellFactory(list -> new ListCell<YearMonth>() {
            @Override
            protected void updateItem(YearMonth item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String month = item.getMonth().getDisplayName(java.time.format.TextStyle.FULL, mainController.getCurrentLocale());
                    setText(Character.toUpperCase(month.charAt(0)) + month.substring(1) + " " + item.getYear());
                }
            }
        });
        monthComboBox.setButtonCell(monthComboBox.getCellFactory().call(null));
    }

    private void setupChartInteraction() {
        elevationChart.setOnMouseMoved(this::handleChartMouseMoved);
        elevationChart.setOnMouseExited(e -> {
            if (elevationHoverMarker != null) {
                elevationHoverMarker.setVisible(false);
            }
        });
    }

    private void setupMapContextMenu() {
        pointAnnotationMenuItem = new MenuItem("Anotación punto");
        textAnnotationMenuItem = new MenuItem("Anotación texto");
        lineAnnotationMenuItem = new MenuItem("Anotación línea");
        circleAnnotationMenuItem = new MenuItem("Anotación círculo");

        pointAnnotationMenuItem.setOnAction(e -> startAnnotationFromContext(AnnotationType.POINT));
        textAnnotationMenuItem.setOnAction(e -> startAnnotationFromContext(AnnotationType.TEXT));
        lineAnnotationMenuItem.setOnAction(e -> startAnnotationFromContext(AnnotationType.LINE));
        circleAnnotationMenuItem.setOnAction(e -> startAnnotationFromContext(AnnotationType.CIRCLE));

        mapContextMenu.getItems().setAll(pointAnnotationMenuItem, textAnnotationMenuItem, lineAnnotationMenuItem, circleAnnotationMenuItem);
    }

    private void setupMapCanvas() {
        mapPane = new Pane();
        mapPane.setPrefSize(1200, 800);

        zoomGroup = new Group(mapPane);
        Group contentGroup = new Group(zoomGroup);
        mapScrollPane.setContent(contentGroup);
        mapScrollPane.addEventFilter(ScrollEvent.SCROLL, this::handleMapScrollZoom);
        mapScrollPane.hvalueProperty().addListener((obs, oldValue, newValue) -> positionSpeedLegend());
        mapScrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> positionSpeedLegend());
        mapScrollPane.viewportBoundsProperty().addListener((obs, oldValue, newValue) -> positionSpeedLegend());

        mapPane.setOnMouseClicked(this::handleMapMouseClick);
        mapScrollPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleZoomShortcut);
                newScene.addEventFilter(MouseEvent.MOUSE_PRESSED, this::hideMapContextMenuOnPrimaryClick);
            }
        });

        elevationHoverMarker = new Circle(6, Color.TRANSPARENT);
        elevationHoverMarker.setStroke(Color.DARKBLUE);
        elevationHoverMarker.setStrokeWidth(2);
        elevationHoverMarker.setVisible(false);
        mapPane.getChildren().add(elevationHoverMarker);

        showMapPlaceholder("Selecciona una actividad para visualizar su trazado.");
    }

    private void setupTooltips() {
        importActivityButton.setTooltip(new Tooltip(mainController.tr("tooltip.importActivity")));
        renameActivityButton.setTooltip(new Tooltip(mainController.tr("tooltip.renameActivity")));
        removeActivityButton.setTooltip(new Tooltip(mainController.tr("tooltip.removeActivity")));
        speedOverlayCheckBox.setTooltip(new Tooltip(mainController.tr("tooltip.speedOverlay")));
        changeAnnotationColorButton.setTooltip(new Tooltip(mainController.tr("tooltip.changeAnnotationColor")));
        removeAnnotationButton.setTooltip(new Tooltip(mainController.tr("tooltip.removeAnnotation")));
    }

    void updateMonthChoices() {
        List<YearMonth> months = new ArrayList<>();
        for (Activity activity : mainController.userActivities) {
            if (activity.getStartTime() != null) {
                YearMonth month = YearMonth.from(activity.getStartTime());
                if (!months.contains(month)) {
                    months.add(month);
                }
            }
        }
        months.sort(Comparator.reverseOrder());

        YearMonth currentSelection = monthComboBox.getValue();
        monthComboBox.getItems().setAll(months);

        if (currentSelection != null && months.contains(currentSelection)) {
            monthComboBox.setValue(currentSelection);
        } else if (!months.isEmpty()) {
            monthComboBox.setValue(months.get(0));
        } else {
            monthComboBox.setValue(null);
        }
    }

    void updateMonthlyTotals() {
        YearMonth selectedMonth = monthComboBox.getValue();
        if (selectedMonth == null) {
            clearMonthlyTotals();
            return;
        }

        double totalDistance = 0.0;
        Duration totalDuration = Duration.ZERO;
        double totalGain = 0.0;
        double totalLoss = 0.0;

        for (Activity activity : mainController.userActivities) {
            if (activity.getStartTime() != null && YearMonth.from(activity.getStartTime()).equals(selectedMonth)) {
                totalDistance += activity.getTotalDistance();
                totalDuration = totalDuration.plus(activity.getDuration());
                totalGain += activity.getElevationGain();
                totalLoss += activity.getElevationLoss();
            }
        }

        monthDistanceLabel.setText(String.format("%.2f km", totalDistance / 1000.0));
        monthDurationLabel.setText(mainController.formatDuration(totalDuration));
        monthGainLabel.setText(String.format("%.0f m", totalGain));
        monthLossLabel.setText(String.format("%.0f m", totalLoss));
    }

    void clearMonthlyTotals() {
        monthDistanceLabel.setText("0 km");
        monthDurationLabel.setText("00:00:00");
        monthGainLabel.setText("0 m");
        monthLossLabel.setText("0 m");
    }

    void clearForLoggedOut() {
        activityListView.getItems().clear();
        annotationListView.getItems().clear();
        clearActivityDetails();
        clearMonthlyTotals();
        showMapPlaceholder(mainController.tr("activities.mapPlaceholderLogin"));
    }

    void updateActivityListAndSelection(Long activityIdToSelect, boolean countAsView) {
        List<Activity> activities = mainController.app.getUserActivities();
        mainController.userActivities.setAll(activities);

        suppressActivitySelectionListener = true;
        activityListView.getItems().setAll(activities);
        Activity selected = null;

        if (activityIdToSelect != null) {
            selected = findActivityById(activityIdToSelect, activities);
            if (selected != null) {
                activityListView.getSelectionModel().select(selected);
            }
        }

        if (selected == null && !activities.isEmpty()) {
            selected = activities.get(0);
            activityListView.getSelectionModel().selectFirst();
        }

        if (selected == null) {
            activityListView.getSelectionModel().clearSelection();
        }

        suppressActivitySelectionListener = false;

        renderActivity(selected, countAsView);
        updateMonthChoices();
        updateMonthlyTotals();
    }

    private Activity findActivityById(long id, List<Activity> activities) {
        for (Activity activity : activities) {
            if (activity.getId() == id) {
                return activity;
            }
        }
        return null;
    }

    void renderActivity(Activity activity, boolean countAsView) {
        currentActivity = activity;
        clearPendingAnnotationState();

        if (activity == null) {
            clearActivityDetails();
            showMapPlaceholder(mainController.tr("activities.mapPlaceholderNone"));
            annotationListView.getItems().clear();
            return;
        }

        if (countAsView) {
            currentMapRegion = mainController.app.findMapForActivity(activity);
        }
        if (currentMapRegion == null) {
            currentMapRegion = activity.getSuggestedMap();
        }

        drawActivityOnMap(activity, currentMapRegion);
        drawElevationProfile(activity);
        fillActivityStats(activity);
        annotationListView.getItems().setAll(activity.getAnnotations());
        mainController.setStatus(mainController.trf("status.viewingActivity", activity.getName()));
    }

    private void clearActivityDetails() {
        statNameLabel.setText("-");
        statDistanceLabel.setText("-");
        statDurationLabel.setText("-");
        statSpeedLabel.setText("-");
        statPaceLabel.setText("-");
        statGainLabel.setText("-");
        statLossLabel.setText("-");
        statMinElevLabel.setText("-");
        statMaxElevLabel.setText("-");
        elevationChart.getData().clear();
        chartTrackPoints = List.of();
        projectedTrackPoints = List.of();
        chartDistanceKm = List.of();
    }

    private void fillActivityStats(Activity activity) {
        statNameLabel.setText(activity.getName());
        statDistanceLabel.setText(String.format("%.2f km", activity.getTotalDistance() / 1000.0));
        statDurationLabel.setText(mainController.formatDuration(activity.getDuration()));
        statSpeedLabel.setText(String.format("%.2f km/h", activity.getAverageSpeed()));
        statPaceLabel.setText(mainController.formatPace(activity.getAveragePace()));
        statGainLabel.setText(String.format("%.0f m", activity.getElevationGain()));
        statLossLabel.setText(String.format("%.0f m", activity.getElevationLoss()));
        statMinElevLabel.setText(String.format("%.0f m", activity.getMinElevation()));
        statMaxElevLabel.setText(String.format("%.0f m", activity.getMaxElevation()));
    }

    private void drawActivityOnMap(Activity activity, MapRegion region) {
        if (region == null) {
            showMapPlaceholder(mainController.tr("activities.mapNoRegion"));
            return;
        }

        File mapFile = mainController.resolveMapFile(region.getImagePath());
        if (!mapFile.exists()) {
            showMapPlaceholder(mainController.trf("activities.mapImageMissing", region.getImagePath()));
            return;
        }

        Image mapImage = new Image(mapFile.toURI().toString());
        if (mapImage.isError()) {
            showMapPlaceholder(mainController.tr("activities.mapImageLoadError"));
            return;
        }

        mapPane.getChildren().clear();
        speedLegendGroup = null;
        ImageView mapView = new ImageView(mapImage);
        mapView.setFitWidth(mapImage.getWidth());
        mapView.setFitHeight(mapImage.getHeight());
        mapPane.getChildren().add(mapView);

        mapPane.setMinSize(mapImage.getWidth(), mapImage.getHeight());
        mapPane.setPrefSize(mapImage.getWidth(), mapImage.getHeight());
        mapPane.setMaxSize(mapImage.getWidth(), mapImage.getHeight());

        currentProjection = new MapProjection(region, mapImage.getWidth(), mapImage.getHeight());

        List<TrackPoint> points = activity.getTrackPoints();
        drawRoute(points);
        drawStartAndEndMarkers(activity);
        drawAnnotations(activity.getAnnotations());

        elevationHoverMarker.setVisible(false);
        mapPane.getChildren().add(elevationHoverMarker);
        applyZoom(zoomSlider.getValue());
    }

    // IA: proyecta el GPX sobre el mapa y calcula el coloreado por velocidad de cada segmento.
    private void drawRoute(List<TrackPoint> points) {
        if (points == null || points.size() < 2 || currentProjection == null) {
            projectedTrackPoints = List.of();
            return;
        }

        List<Point2D> projected = new ArrayList<>();
        for (TrackPoint tp : points) {
            projected.add(currentProjection.project(tp));
        }
        projectedTrackPoints = projected;

        if (!speedOverlayCheckBox.isSelected()) {
            Polyline route = new Polyline();
            route.setStroke(Color.DODGERBLUE);
            route.setStrokeWidth(3.0);
            for (Point2D p : projected) {
                route.getPoints().addAll(p.getX(), p.getY());
            }
            mapPane.getChildren().add(route);
            return;
        }

        List<Double> speeds = new ArrayList<>();
        for (int i = 1; i < points.size(); i++) {
            double speed = points.get(i - 1).speedTo(points.get(i));
            if (!Double.isFinite(speed) || speed < 0) {
                speed = 0;
            }
            speeds.add(speed);
        }

        double minSpeed = 0.0;
        double maxSpeed = 0.0;
        if (!speeds.isEmpty()) {
            minSpeed = speeds.get(0);
            maxSpeed = speeds.get(0);
            for (Double speedValue : speeds) {
                if (speedValue < minSpeed) {
                    minSpeed = speedValue;
                }
                if (speedValue > maxSpeed) {
                    maxSpeed = speedValue;
                }
            }
        }
        if (Math.abs(maxSpeed - minSpeed) < 0.0001) {
            maxSpeed = minSpeed + 1.0;
        }

        for (int i = 1; i < projected.size(); i++) {
            Point2D p1 = projected.get(i - 1);
            Point2D p2 = projected.get(i);

            Line segment = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
            segment.setStrokeWidth(4.0);
            segment.setStroke(colorForSpeed(speeds.get(i - 1), minSpeed, maxSpeed));
            mapPane.getChildren().add(segment);
        }

        drawSpeedLegend(minSpeed, maxSpeed);
    }

    private void drawSpeedLegend(double minSpeed, double maxSpeed) {
        Rectangle background = new Rectangle(0, 0, 170, 58);
        background.setArcWidth(10);
        background.setArcHeight(10);
        background.setFill(Color.rgb(255, 255, 255, 0.88));
        background.setStroke(Color.rgb(45, 65, 85, 0.35));

        Line slowLine = new Line(14, 24, 52, 24);
        slowLine.setStroke(colorForSpeed(minSpeed, minSpeed, maxSpeed));
        slowLine.setStrokeWidth(5);

        Line fastLine = new Line(14, 44, 52, 44);
        fastLine.setStroke(colorForSpeed(maxSpeed, minSpeed, maxSpeed));
        fastLine.setStrokeWidth(5);

        Text slowText = new Text(62, 28, mainController.trf("activities.legendSlow", minSpeed));
        Text fastText = new Text(62, 48, mainController.trf("activities.legendFast", maxSpeed));
        slowText.setFill(Color.rgb(35, 50, 65));
        fastText.setFill(Color.rgb(35, 50, 65));

        speedLegendGroup = new Group(background, slowLine, fastLine, slowText, fastText);
        speedLegendGroup.setMouseTransparent(true);
        mapPane.getChildren().add(speedLegendGroup);
        positionSpeedLegend();
    }

    private Color colorForSpeed(double speed, double minSpeed, double maxSpeed) {
        double t = (speed - minSpeed) / (maxSpeed - minSpeed);
        t = Math.max(0.0, Math.min(1.0, t));
        double hue = 230.0 - (230.0 * t);
        return Color.hsb(hue, 0.95, 0.95);
    }

    private void drawStartAndEndMarkers(Activity activity) {
        if (activity.getStartPoint() == null || activity.getEndPoint() == null || currentProjection == null) {
            return;
        }

        Point2D start = currentProjection.project(activity.getStartPoint());
        Point2D end = currentProjection.project(activity.getEndPoint());

        Circle startCircle = new Circle(start.getX(), start.getY(), 7, Color.LIMEGREEN);
        startCircle.setStroke(Color.BLACK);
        startCircle.setStrokeWidth(1.5);

        Rectangle endSquare = new Rectangle(end.getX() - 7, end.getY() - 7, 14, 14);
        endSquare.setFill(Color.RED);
        endSquare.setStroke(Color.BLACK);
        endSquare.setStrokeWidth(1.5);

        Text startLabel = createMapLabel(mainController.tr("activities.start"), start.getX() + 10, start.getY() - 10, Color.DARKGREEN);
        Text endLabel = createMapLabel(mainController.tr("activities.end"), end.getX() + 10, end.getY() - 10, Color.DARKRED);

        mapPane.getChildren().addAll(startCircle, endSquare, startLabel, endLabel);
    }

    private Text createMapLabel(String value, double x, double y, Color color) {
        Text label = new Text(x, y, value);
        label.setFill(color);
        label.setStroke(Color.WHITE);
        label.setStrokeWidth(0.6);
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    private void drawAnnotations(List<Annotation> annotations) {
        if (annotations == null || annotations.isEmpty() || currentProjection == null) {
            return;
        }

        for (Annotation annotation : annotations) {
            Color color = parseColor(annotation.getColor(), Color.DARKORANGE);
            List<GeoPoint> points = annotation.getGeoPoints();

            switch (annotation.getType()) {
                case POINT:
                    if (points.size() >= 1) {
                        Point2D p = currentProjection.project(points.get(0));
                        Circle marker = new Circle(p.getX(), p.getY(), Math.max(4.0, annotation.getStrokeWidth() * 2), color);
                        marker.setStroke(Color.BLACK);
                        mapPane.getChildren().add(marker);
                        if (annotation.getText() != null && !annotation.getText().isBlank()) {
                            mapPane.getChildren().add(createMapLabel(annotation.getText(), p.getX() + 8, p.getY() - 8, color));
                        }
                    }
                    break;
                case TEXT:
                    if (points.size() >= 1) {
                        Point2D p = currentProjection.project(points.get(0));
                        String textValue = annotation.getText() == null ? "" : annotation.getText();
                        mapPane.getChildren().add(createMapLabel(textValue, p.getX() + 4, p.getY() - 4, color));
                    }
                    break;
                case LINE:
                    if (points.size() >= 2) {
                        Point2D a = currentProjection.project(points.get(0));
                        Point2D b = currentProjection.project(points.get(1));
                        Line line = new Line(a.getX(), a.getY(), b.getX(), b.getY());
                        line.setStroke(color);
                        line.setStrokeWidth(Math.max(1.0, annotation.getStrokeWidth()));
                        mapPane.getChildren().add(line);
                    }
                    break;
                case CIRCLE:
                    if (points.size() >= 2) {
                        Point2D center = currentProjection.project(points.get(0));
                        Point2D edge = currentProjection.project(points.get(1));
                        double radius = center.distance(edge);
                        Circle circle = new Circle(center.getX(), center.getY(), radius);
                        circle.setFill(Color.TRANSPARENT);
                        circle.setStroke(color);
                        circle.setStrokeWidth(Math.max(1.0, annotation.getStrokeWidth()));
                        mapPane.getChildren().add(circle);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    // IA: transforma los puntos GPX en distancia acumulada para sincronizar grafica y mapa.
    private void drawElevationProfile(Activity activity) {
        elevationChart.getData().clear();
        chartTrackPoints = activity.getTrackPoints();

        if (chartTrackPoints == null || chartTrackPoints.isEmpty()) {
            chartDistanceKm = List.of();
            return;
        }

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        List<Double> cumulative = new ArrayList<>();

        double km = 0.0;
        cumulative.add(0.0);
        TrackPoint first = chartTrackPoints.get(0);
        series.getData().add(new XYChart.Data<>(0.0, first.getElevation()));

        for (int i = 1; i < chartTrackPoints.size(); i++) {
            TrackPoint previous = chartTrackPoints.get(i - 1);
            TrackPoint current = chartTrackPoints.get(i);
            km += previous.distanceTo(current) / 1000.0;
            cumulative.add(km);
            series.getData().add(new XYChart.Data<>(km, current.getElevation()));
        }

        chartDistanceKm = cumulative;
        elevationChart.getData().add(series);
    }

    private void handleMapMouseClick(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && mapContextMenu.isShowing()) {
            mapContextMenu.hide();
            return;
        }

        if (currentProjection == null || currentActivity == null) {
            return;
        }

        if (event.getButton() == MouseButton.SECONDARY) {
            contextClickPoint = new Point2D(event.getX(), event.getY());
            mapContextMenu.show(mapPane, event.getScreenX(), event.getScreenY());
            return;
        }

        if (event.getButton() == MouseButton.PRIMARY && pendingAnnotationType != null && pendingFirstPoint != null) {
            GeoPoint secondPoint = currentProjection.unproject(event.getX(), event.getY());
            AnnotationType type = pendingAnnotationType;
            GeoPoint firstPoint = pendingFirstPoint;
            clearPendingAnnotationState();

            if (type == AnnotationType.LINE || type == AnnotationType.CIRCLE) {
                createAnnotationAndPersist(type, List.of(firstPoint, secondPoint));
            }
        }
    }

    // IA: coordina anotaciones de uno o dos puntos conservando el estado entre clic derecho e izquierdo.
    private void startAnnotationFromContext(AnnotationType type) {
        if (currentProjection == null || currentActivity == null || contextClickPoint == null) {
            mainController.setStatus(mainController.tr("status.selectActivityAndPoint"));
            return;
        }

        GeoPoint firstPoint = currentProjection.unproject(contextClickPoint.getX(), contextClickPoint.getY());

        if (type == AnnotationType.LINE || type == AnnotationType.CIRCLE) {
            pendingAnnotationType = type;
            pendingFirstPoint = firstPoint;
            showPendingAnnotationGuide(type, contextClickPoint);
            if (type == AnnotationType.LINE) {
                mainController.setStatus(mainController.tr("status.lineStarted"));
            } else {
                mainController.setStatus(mainController.tr("status.circleStarted"));
            }
            return;
        }

        createAnnotationAndPersist(type, List.of(firstPoint));
    }

    private void clearPendingAnnotationState() {
        pendingAnnotationType = null;
        pendingFirstPoint = null;
        clearPendingAnnotationGuide();
    }

    private void showPendingAnnotationGuide(AnnotationType type, Point2D firstPoint) {
        clearPendingAnnotationGuide();

        pendingAnnotationMarker = new Circle(firstPoint.getX(), firstPoint.getY(), 10, Color.rgb(255, 193, 7, 0.35));
        pendingAnnotationMarker.setStroke(Color.rgb(184, 112, 0));
        pendingAnnotationMarker.setStrokeWidth(3);

        String message = type == AnnotationType.LINE
                ? mainController.tr("activities.pendingLine")
                : mainController.tr("activities.pendingCircle");
        pendingAnnotationHint = createMapLabel(message, firstPoint.getX() + 14, firstPoint.getY() - 14, Color.rgb(140, 82, 0));
        pendingAnnotationHint.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        mapPane.getChildren().addAll(pendingAnnotationMarker, pendingAnnotationHint);
    }

    private void clearPendingAnnotationGuide() {
        if (mapPane != null) {
            if (pendingAnnotationMarker != null) {
                mapPane.getChildren().remove(pendingAnnotationMarker);
            }
            if (pendingAnnotationHint != null) {
                mapPane.getChildren().remove(pendingAnnotationHint);
            }
        }
        pendingAnnotationMarker = null;
        pendingAnnotationHint = null;
    }

    private void createAnnotationAndPersist(AnnotationType type, List<GeoPoint> points) {
        Optional<AnnotationInput> inputResult = showAnnotationDialog(type);
        if (inputResult.isEmpty()) {
            mainController.setStatus(mainController.tr("status.annotationCancelled"));
            return;
        }

        AnnotationInput input = inputResult.get();
        Annotation annotation = new Annotation(type, input.text, input.color, input.strokeWidth, points);
        Annotation saved;
        try {
            saved = mainController.app.addAnnotation(currentActivity, annotation);
        } catch (RuntimeException ex) {
            mainController.showError(mainController.tr("error.annotationSave"));
            return;
        }

        if (saved == null) {
            mainController.showError(mainController.tr("error.annotationSaveGeneric"));
            return;
        }

        mainController.setStatus(mainController.tr("status.annotationCreated"));
        updateActivityListAndSelection(currentActivity.getId(), false);
    }

    private Optional<AnnotationInput> showAnnotationDialog(AnnotationType type) {
        Dialog<AnnotationInput> dialog = new Dialog<>();
        dialog.setTitle(mainController.trf("dialog.annotation.title", annotationTypeText(type)));
        dialog.setHeaderText(mainController.tr("dialog.annotation.header"));

        ButtonType accept = new ButtonType(mainController.tr("button.save"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(accept, ButtonType.CANCEL);

        TextField textField = new TextField();
        TextField colorField = new TextField("#E74C3C");
        TextField widthField = new TextField("2.0");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(new Label(mainController.tr("dialog.annotation.text")), 0, 0);
        grid.add(textField, 1, 0);
        grid.add(new Label(mainController.tr("dialog.annotation.color")), 0, 1);
        grid.add(colorField, 1, 1);
        grid.add(new Label(mainController.tr("dialog.annotation.width")), 0, 2);
        grid.add(widthField, 1, 2);
        dialog.getDialogPane().setContent(grid);

        Node okButton = dialog.getDialogPane().lookupButton(accept);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!mainController.isValidHexColor(colorField.getText())) {
                event.consume();
                mainController.showError(mainController.tr("error.invalidColorFull"));
                return;
            }
            try {
                double w = Double.parseDouble(widthField.getText());
                if (w <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                event.consume();
                mainController.showError(mainController.tr("error.invalidWidth"));
            }
        });

        dialog.setResultConverter(button -> {
            if (button == accept) {
                return new AnnotationInput(
                        textField.getText() == null ? "" : textField.getText().trim(),
                        colorField.getText().trim(),
                        Double.parseDouble(widthField.getText().trim())
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private void handleChartMouseMoved(MouseEvent event) {
        if (chartDistanceKm.isEmpty() || projectedTrackPoints.isEmpty()) {
            return;
        }

        double distanceAtMouse = valueAtAxis(distanceAxis, event.getSceneX());
        if (!Double.isFinite(distanceAtMouse)) {
            return;
        }

        int index = closestDistanceIndex(distanceAtMouse, chartDistanceKm);
        if (index < 0 || index >= projectedTrackPoints.size()) {
            return;
        }

        Point2D markerPoint = projectedTrackPoints.get(index);
        elevationHoverMarker.setCenterX(markerPoint.getX());
        elevationHoverMarker.setCenterY(markerPoint.getY());
        elevationHoverMarker.setVisible(true);
    }

    private double valueAtAxis(NumberAxis axis, double sceneX) {
        Point2D local = axis.sceneToLocal(sceneX, 0);
        Number value = axis.getValueForDisplay(local.getX());
        return value == null ? Double.NaN : value.doubleValue();
    }

    private int closestDistanceIndex(double target, List<Double> distances) {
        int bestIndex = -1;
        double bestDelta = Double.MAX_VALUE;

        for (int i = 0; i < distances.size(); i++) {
            double delta = Math.abs(distances.get(i) - target);
            if (delta < bestDelta) {
                bestDelta = delta;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private void showMapPlaceholder(String message) {
        mapPane.getChildren().clear();
        speedLegendGroup = null;
        Label label = new Label(message);
        label.setStyle("-fx-text-fill: #555;");
        label.setLayoutX(20);
        label.setLayoutY(20);
        mapPane.getChildren().add(label);
        mapPane.setPrefSize(900, 500);

        if (elevationHoverMarker != null) {
            elevationHoverMarker.setVisible(false);
            mapPane.getChildren().add(elevationHoverMarker);
        }

        currentProjection = null;
        projectedTrackPoints = List.of();
        chartTrackPoints = List.of();
        chartDistanceKm = List.of();
    }

    private void applyZoom(double scaleValue) {
        if (zoomGroup == null) {
            return;
        }

        double h = mapScrollPane.getHvalue();
        double v = mapScrollPane.getVvalue();

        zoomGroup.setScaleX(scaleValue);
        zoomGroup.setScaleY(scaleValue);

        mapScrollPane.setHvalue(h);
        mapScrollPane.setVvalue(v);
        positionSpeedLegend();
    }

    private void positionSpeedLegend() {
        if (speedLegendGroup == null || mapPane == null || mapScrollPane == null || zoomSlider == null) {
            return;
        }

        double scale = Math.max(zoomSlider.getValue(), 0.0001);
        Bounds viewport = mapScrollPane.getViewportBounds();
        double mapWidth = mapPane.getWidth() > 0 ? mapPane.getWidth() : mapPane.getPrefWidth();
        double mapHeight = mapPane.getHeight() > 0 ? mapPane.getHeight() : mapPane.getPrefHeight();
        double scaledWidth = mapWidth * scale;
        double scaledHeight = mapHeight * scale;
        double left = Math.max(0.0, scaledWidth - viewport.getWidth()) * mapScrollPane.getHvalue();
        double top = Math.max(0.0, scaledHeight - viewport.getHeight()) * mapScrollPane.getVvalue();

        speedLegendGroup.setLayoutX((left + 12.0) / scale);
        speedLegendGroup.setLayoutY((top + 12.0) / scale);
        speedLegendGroup.getTransforms().setAll(new Scale(1.0 / scale, 1.0 / scale, 0, 0));
        speedLegendGroup.toFront();
    }

    private void handleMapScrollZoom(ScrollEvent event) {
        if (currentProjection == null || event.getDeltaY() == 0.0) {
            return;
        }

        changeZoom(event.getDeltaY() > 0.0 ? ZOOM_STEP : -ZOOM_STEP);
        event.consume();
    }

    // IA: adapta los atajos estandar de zoom del sistema al slider del mapa.
    private void handleZoomShortcut(KeyEvent event) {
        if (!event.isShortcutDown()) {
            return;
        }

        KeyCode code = event.getCode();
        if (code == KeyCode.PLUS || code == KeyCode.ADD || code == KeyCode.EQUALS) {
            changeZoom(ZOOM_STEP);
            event.consume();
        } else if (code == KeyCode.MINUS || code == KeyCode.SUBTRACT) {
            changeZoom(-ZOOM_STEP);
            event.consume();
        } else if (code == KeyCode.DIGIT0 || code == KeyCode.NUMPAD0) {
            zoomSlider.setValue(1.0);
            mainController.setStatus(mainController.tr("status.zoomReset"));
            event.consume();
        }
    }

    private void hideMapContextMenuOnPrimaryClick(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && mapContextMenu.isShowing()) {
            mapContextMenu.hide();
        }
    }

    private void changeZoom(double delta) {
        double nextValue = Math.max(zoomSlider.getMin(), Math.min(zoomSlider.getMax(), zoomSlider.getValue() + delta));
        zoomSlider.setValue(nextValue);
        mainController.setStatus(mainController.trf("status.zoom", nextValue * 100.0));
    }

    private Color parseColor(String value, Color fallback) {
        try {
            return Color.web(value);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String annotationTypeText(AnnotationType type) {
        if (type == null) {
            return "";
        }

        switch (type) {
            case POINT:
                return mainController.tr("annotation.point");
            case TEXT:
                return mainController.tr("annotation.text");
            case LINE:
                return mainController.tr("annotation.line");
            case CIRCLE:
                return mainController.tr("annotation.circle");
            default:
                return type.name();
        }
    }

    @FXML
    void onImportActivity() {
        if (!mainController.requireLogin()) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle(mainController.tr("dialog.gpxTitle"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(mainController.tr("dialog.gpxFilter"), "*.gpx"));
        File gpxDir = mainController.resolveMapFile("gpx");
        if (gpxDir.exists() && gpxDir.isDirectory()) {
            chooser.setInitialDirectory(gpxDir);
        }

        File selected = chooser.showOpenDialog(mainController.getOwnerWindow());
        if (selected == null) {
            return;
        }

        Activity activity;
        try {
            activity = mainController.app.importActivity(selected);
        } catch (RuntimeException ex) {
            mainController.showError(mainController.tr("error.importGpx"));
            return;
        }
        if (activity == null) {
            mainController.showError(mainController.tr("error.importGpxGeneric"));
            return;
        }

        updateActivityListAndSelection(activity.getId(), true);
        mainController.setStatus(mainController.trf("status.activityImported", activity.getName()));
    }

    @FXML
    void onRenameActivity() {
        if (!mainController.requireLogin()) {
            return;
        }

        Activity selected = activityListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            mainController.showError(mainController.tr("error.selectActivityRename"));
            return;
        }

        TextField nameField = new TextField(selected.getName());
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(mainController.tr("dialog.rename.title"));
        dialog.setHeaderText(mainController.tr("dialog.rename.header"));

        ButtonType ok = new ButtonType(mainController.tr("button.save"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(new VBox(8, new Label(mainController.tr("dialog.rename.name")), nameField));
        dialog.setResultConverter(bt -> bt == ok ? nameField.getText().trim() : null);

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().isBlank()) {
            return;
        }

        boolean updated;
        try {
            updated = mainController.app.renameActivity(selected, result.get());
        } catch (RuntimeException ex) {
            mainController.showError(mainController.tr("error.renamePersistence"));
            return;
        }
        if (!updated) {
            mainController.showError(mainController.tr("error.renameGeneric"));
            return;
        }

        updateActivityListAndSelection(selected.getId(), false);
        mainController.setStatus(mainController.tr("status.activityRenamed"));
    }

    @FXML
    void onRemoveActivity() {
        if (!mainController.requireLogin()) {
            return;
        }

        Activity selected = activityListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            mainController.showError(mainController.tr("error.selectActivityDelete"));
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(mainController.tr("dialog.deleteActivity.title"));
        confirm.setHeaderText(mainController.tr("dialog.deleteActivity.header"));
        confirm.setContentText(mainController.trf("dialog.deleteActivity.content", selected.getName()));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        boolean removed;
        try {
            removed = mainController.app.removeActivity(selected);
        } catch (RuntimeException ex) {
            mainController.showError(mainController.tr("error.deleteActivityPersistence"));
            return;
        }
        if (!removed) {
            mainController.showError(mainController.tr("error.deleteActivityGeneric"));
            return;
        }

        updateActivityListAndSelection(null, false);
        mainController.setStatus(mainController.tr("status.activityRemoved"));
    }

    @FXML
    private void onZoomIn() {
        changeZoom(ZOOM_STEP);
    }

    @FXML
    private void onZoomOut() {
        changeZoom(-ZOOM_STEP);
    }

    @FXML
    private void onSpeedOverlayChanged() {
        if (currentActivity != null) {
            renderActivity(currentActivity, false);
        }
    }

    @FXML
    private void onMonthChanged() {
        updateMonthlyTotals();
    }

    @FXML
    private void onChangeAnnotationColor() {
        if (!mainController.requireLogin()) {
            return;
        }

        Annotation selected = annotationListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            mainController.showError(mainController.tr("error.selectAnnotation"));
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(mainController.tr("dialog.color.title"));
        dialog.setHeaderText(mainController.tr("dialog.color.header"));

        ButtonType ok = new ButtonType(mainController.tr("button.save"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        TextField colorField = new TextField(selected.getColor());
        dialog.getDialogPane().setContent(new VBox(8, new Label(mainController.tr("dialog.annotation.color")), colorField));

        Node okButton = dialog.getDialogPane().lookupButton(ok);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!mainController.isValidHexColor(colorField.getText())) {
                event.consume();
                mainController.showError(mainController.tr("error.invalidColorShort"));
            }
        });

        dialog.setResultConverter(bt -> bt == ok ? colorField.getText().trim() : null);

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        selected.setColor(result.get());
        boolean okUpdate;
        try {
            okUpdate = mainController.app.updateAnnotationColor(selected);
        } catch (RuntimeException ex) {
            mainController.showError(mainController.tr("error.colorPersistence"));
            return;
        }
        if (!okUpdate) {
            mainController.showError(mainController.tr("error.colorGeneric"));
            return;
        }

        updateActivityListAndSelection(currentActivity.getId(), false);
        mainController.setStatus(mainController.tr("status.annotationColorUpdated"));
    }

    @FXML
    private void onRemoveAnnotation() {
        if (!mainController.requireLogin()) {
            return;
        }

        Annotation selected = annotationListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            mainController.showError(mainController.tr("error.selectAnnotationRemove"));
            return;
        }

        boolean removed;
        try {
            removed = mainController.app.removeAnnotation(selected);
        } catch (RuntimeException ex) {
            mainController.showError(mainController.tr("error.annotationRemovePersistence"));
            return;
        }
        if (!removed) {
            mainController.showError(mainController.tr("error.annotationRemoveGeneric"));
            return;
        }

        updateActivityListAndSelection(currentActivity.getId(), false);
        mainController.setStatus(mainController.tr("status.annotationRemoved"));
    }

    private static final class AnnotationInput {
        final String text;
        final String color;
        final double strokeWidth;

        AnnotationInput(String text, String color, double strokeWidth) {
            this.text = text;
            this.color = color;
            this.strokeWidth = strokeWidth;
        }
    }
}


