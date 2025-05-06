package com.example.ca2realrealsproject;

import java.util.*;

public class Graph {
    private final Map<Station, List<Edge>> adj = new HashMap<>();

    public void addStation(Station s) {
        adj.putIfAbsent(s, new ArrayList<>());
    }

    public void addEdge(Station a, Station b, double weight, String line) {
        addStation(a);
        addStation(b);
        Edge ab = new Edge(a, b, weight, line);
        Edge ba = new Edge(b, a, weight, line);
        adj.get(a).add(ab);
        adj.get(b).add(ba);
        a.addLine(line);
        b.addLine(line);
    }

    public List<Edge> getNeighbors(Station s) {
        return adj.getOrDefault(s, Collections.emptyList());
    }

    public Set<Station> getStations() {
        return adj.keySet();
    }

    public void removeStations(Set<Station> toRemove) {
        for (Station s : toRemove) {
            adj.remove(s);
        }
        for (List<Edge> edges : adj.values()) {
            edges.removeIf(e -> toRemove.contains(e.getTo()));
        }
    }
}
