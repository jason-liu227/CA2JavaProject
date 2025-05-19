package com.example.ca2realrealsproject;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class CSVLoader {
    // Expects a CSV with columns: Start,Stop,Line,Color
   //  weight is defaulted to 1.0 (you can augment with real distances later
    public static Graph load(String resourceCsv) throws Exception {
        Graph g = new Graph();
        Map<String,Station> stations = new HashMap<>();
        InputStream is = CSVLoader.class.getResourceAsStream(resourceCsv);
        if (is == null) throw new IllegalArgumentException("CSV not found: " + resourceCsv);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String header = br.readLine(); // skip
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                String a = parts[0].trim();
                String b = parts[1].trim();
                String lineName = parts[2].trim();// color ignored here (you can map lineName -> color in GUI)
                Station sa = stations.computeIfAbsent(a, Station::new);
                Station sb = stations.computeIfAbsent(b, Station::new);
                g.addEdge(sa, sb, 1.0, lineName);}}
        return g;}
}
