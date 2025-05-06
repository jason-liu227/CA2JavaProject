package com.example.ca2realrealsproject;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class MainController implements Initializable {

    @FXML private ComboBox<String> startBox;
    @FXML private ComboBox<String> endBox;
    @FXML private ListView<String> avoidList;
    @FXML private CheckBox usePenalty;
    @FXML private Slider penaltySlider;
    @FXML private Button anyRouteBtn;
    @FXML private Button allRoutesBtn;
    @FXML private Button dijkstraBtn;
    @FXML private Button waypointBtn;
    @FXML private Canvas canvas;

    private Map<String, Point2D> pixelMap = new HashMap<>();

    private Graph graph;
    private RouteFinder finder;
    private Map<String, Station> stationMap;
    private Image mapImage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // 1) Load graph and helper
            graph = CSVLoader.load("/com/example/ca2realrealsproject/vienna_subway.csv");
            finder = new RouteFinder(graph);
            stationMap = graph.getStations().stream()
                    .collect(Collectors.toMap(Station::getName, s -> s));

            // 2) Populate UI lists
            startBox.getItems().setAll(stationMap.keySet());
            endBox.getItems().setAll(stationMap.keySet());
            avoidList.getItems().setAll(stationMap.keySet());
            avoidList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            // 3) Load base map image
            mapImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/example/ca2realrealsproject/Ubhan.png")));
            loadPixelCoordinates("/com/example/ca2realrealsproject/StationCoordinates.csv");
            // 4) Paint initial map
            paintBaseMap();

            // 5) Wire up buttons
            anyRouteBtn.setOnAction(e -> handleAnyRoute());
            allRoutesBtn.setOnAction(e -> handleAllRoutes());
            dijkstraBtn.setOnAction(e -> handleDijkstra());
            waypointBtn.setOnAction(e -> handleWaypoint());

        } catch (Exception ex) {
            ex.printStackTrace();
            // In real app, show an alert here
        }
    }

    private void handleAnyRoute() {
        Station s = stationMap.get(startBox.getValue());
        Station t = stationMap.get(endBox.getValue());
        Set<Station> avoid = getAvoidSet();
        List<Station> path = finder.findAnyRoute(s, t, avoid);
        paintRoute(path);
    }

    private void handleAllRoutes() {
        Station s = stationMap.get(startBox.getValue());
        Station t = stationMap.get(endBox.getValue());
        Set<Station> avoid = getAvoidSet();
        // e.g. maxDepth = 20
        List<List<Station>> all = finder.findAllRoutesDFS(s, t, avoid, 20);
        if (!all.isEmpty()) {
            // Just show the first one, or extend to let user pick
            paintRoute(all.get(0));
        }
    }

    private void handleDijkstra() {
        Station s = stationMap.get(startBox.getValue());
        Station t = stationMap.get(endBox.getValue());
        Set<Station> avoid = getAvoidSet();
        double pen = usePenalty.isSelected() ? penaltySlider.getValue() : 0.0;
        List<Station> path = usePenalty.isSelected()
                ? finder.dijkstraWithPenalty(s, t, avoid, pen)
                : finder.dijkstra(s, t, avoid);
        paintRoute(path);
    }

    private void handleWaypoint() {
        // Example: prompt user for one waypoint via dialog
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Enter waypoint station name:");
        Optional<String> wp = dialog.showAndWait();
        if (wp.isPresent() && stationMap.containsKey(wp.get())) {
            List<Station> pts = Arrays.asList(
                    stationMap.get(startBox.getValue()),
                    stationMap.get(wp.get()),
                    stationMap.get(endBox.getValue())
            );
            Set<Station> avoid = getAvoidSet();
            double pen = usePenalty.isSelected() ? penaltySlider.getValue() : 0.0;
            List<Station> path = finder.routeWithWaypoints(pts, true, pen, avoid);
            paintRoute(path);
        }
    }

    private Set<Station> getAvoidSet() {
        return avoidList.getSelectionModel()
                .getSelectedItems()
                .stream()
                .map(stationMap::get)
                .collect(Collectors.toSet());
    }

    private void paintBaseMap() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.drawImage(mapImage, 0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void paintRoute(List<Station> path) {
        paintBaseMap();
        if (path == null || path.isEmpty()) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(4);
        for (int i = 0; i < path.size() - 1; i++) {
            Station a = path.get(i), b = path.get(i + 1);
            Point2D pa = pixelMap.get(a.getName());
            Point2D pb = pixelMap.get(b.getName());
            if (pa == null || pb == null) continue;  // skip if coordinates missing
            gc.strokeLine(pa.getX(), pa.getY(), pb.getX(), pb.getY());
            gc.fillOval(pa.getX() - 5, pa.getY() - 5, 10, 10);
//            String line = a.getLines().stream()
//                    .filter(b.getLines()::contains)
//                    .findFirst()
//                    .orElse("");
//            gc.setStroke(Color.web(lineColorHex(line)));
//            gc.strokeLine(pa[0], pa[1], pb[0], pb[1]);
//            gc.fillOval(pa[0] - 5, pa[1] - 5, 10, 10);
        }
        // Last station marker
        Station last = path.get(path.size() - 1);
//        double[] pl = geoToPixel(last);
//        gc.fillOval(pl[0] - 5, pl[1] - 5, 10, 10);
    }

    private void loadPixelCoordinates(String resourcePath) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream(resourcePath)))) {
            String line = br.readLine();  // skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                String name = parts[0].trim();
                double x = Double.parseDouble(parts[1].trim());
                double y = Double.parseDouble(parts[2].trim());
                pixelMap.put(name, new Point2D(x, y));
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Optionally show an alert to the user
        }
    }

    /** Stub: replace with real lat/long → pixel mapping */
//    private double[] geoToPixel(Station s) {
//        return new double[]{
//                Math.random() * canvas.getWidth(),
//                Math.random() * canvas.getHeight()
//        };
//    }

    /** Hex colors matching U-Bahn lines */
    private static String lineColorHex(String lineName) {
        switch (lineName) {
            case "1": return "#FF0000"; // U1 red
            case "2": return "#800080"; // U2 purple
            case "3": return "#0000FF"; // U3 blue
            case "4": return "#FFFF00"; // U4 yellow
            case "6": return "#00FF00"; // U6 green
            default:  return "#000000";
        }
    }
}
