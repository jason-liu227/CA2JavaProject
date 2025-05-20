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

    // Handles "Find Any Route" action
// Uses DFS to find a single valid route from the start station to the end station,
// avoiding any stations selected in the UI, and paints the route on the map.
    private void handleAnyRoute() {
        Station s = stationMap.get(startBox.getValue()); // Get start station from dropdown
        Station t = stationMap.get(endBox.getValue());   // Get end station from dropdown
        Set<Station> avoid = getAvoidSet();              // Get user-selected stations to avoid
        List<Station> path = finder.findAnyRoute(s, t, avoid); // Find one route via DFS
        paintRoute(path);                                // Display the route on the canvas
    }

    // Handles "Find All Routes" action
// Uses DFS to find all valid routes from start to end within a max depth,
// avoiding selected stations. Paints the first route found (could be extended to choose).
    private void handleAllRoutes() {
        Station s = stationMap.get(startBox.getValue());
        Station t = stationMap.get(endBox.getValue());
        Set<Station> avoid = getAvoidSet();
        List<List<Station>> all = finder.findAllRoutesDFS(s, t, avoid, 20); // Max depth = 20
        if (!all.isEmpty()) {
            paintRoute(all.get(0)); // Only paints the first route (could be extended to show others)
        }
    }

    // Handles "Dijkstra" action
// Uses Dijkstra's algorithm to find the shortest path,
// optionally applying a penalty for line changes if the user enabled it.
    private void handleDijkstra() {
        Station s = stationMap.get(startBox.getValue());
        Station t = stationMap.get(endBox.getValue());
        Set<Station> avoid = getAvoidSet();
        double pen = usePenalty.isSelected() ? penaltySlider.getValue() : 0.0; // Penalty value
        List<Station> path = usePenalty.isSelected()
                ? finder.dijkstraWithPenalty(s, t, avoid, pen) // Use Dijkstra with penalty
                : finder.dijkstra(s, t, avoid);                // Use normal Dijkstra
        paintRoute(path); // Paint the resulting shortest route
    }

    // Handles "Route with Waypoint" action
// Prompts user for a waypoint station and finds a route that goes through:
// start → waypoint → end using Dijkstra (with optional penalty).
    private void handleWaypoint() {
        TextInputDialog dialog = new TextInputDialog();             // Dialog to enter waypoint
        dialog.setHeaderText("Enter waypoint station name:");
        Optional<String> wp = dialog.showAndWait();                 // Wait for user input
        if (wp.isPresent() && stationMap.containsKey(wp.get())) {   // Validate input
            List<Station> pts = Arrays.asList(
                    stationMap.get(startBox.getValue()),
                    stationMap.get(wp.get()),
                    stationMap.get(endBox.getValue())
            );
            Set<Station> avoid = getAvoidSet();
            double pen = usePenalty.isSelected() ? penaltySlider.getValue() : 0.0;
            List<Station> path = finder.routeWithWaypoints(pts, true, pen, avoid); // Always using Dijkstra
            paintRoute(path);
        }
    }

    // Helper method: gets the set of stations the user has selected to avoid
    private Set<Station> getAvoidSet() {
        return avoidList.getSelectionModel()              // Get selected items from ListView
                .getSelectedItems()
                .stream()
                .map(stationMap::get)                     // Convert station names to Station objects
                .collect(Collectors.toSet());
    }

    // Draws the base map image onto the canvas (used before painting a route)
    private void paintBaseMap() {
        GraphicsContext gc = canvas.getGraphicsContext2D();         // Get canvas context
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());  // Clear any old drawings
        gc.drawImage(mapImage, 0, 0, canvas.getWidth(), canvas.getHeight()); // Draw background map
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
        }
        // Last station marker
        Station last = path.get(path.size() - 1);
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


    // Hex colors matching U-Bahn lines
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
