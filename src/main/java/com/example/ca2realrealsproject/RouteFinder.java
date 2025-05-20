package com.example.ca2realrealsproject;

import java.util.*;
import java.util.stream.Collectors;

public class RouteFinder {
    private final Graph graph;  // Graph representing the network of stations and connections

    // Constructor: initializes the RouteFinder with a graph
    public RouteFinder(Graph graph) {
        this.graph = graph;
    }

    // Finds any single valid route from start to end using Depth-First Search (DFS)
    // Avoids any stations listed in the 'avoid' set
    public List<Station> findAnyRoute(Station start, Station end, Set<Station> avoid) {
        Set<Station> visited = new HashSet<>(avoid); // Tracks visited stations (starting with avoided ones)
        List<Station> path = new ArrayList<>();      // Stores the current path
        return dfsAny(start, end, visited, path) ? path : Collections.emptyList();
    }

    // Recursive DFS helper method to find any valid path from cur to end
    private boolean dfsAny(Station cur, Station end, Set<Station> visited, List<Station> path) {
        if (visited.contains(cur)) return false;     // Skip already visited or avoided station
        visited.add(cur);                            // Mark current station as visited
        path.add(cur);                               // Add current station to path
        if (cur.equals(end)) return true;            // Reached destination
        for (Edge e : graph.getNeighbors(cur)) {     // Explore neighbors
            if (dfsAny(e.getTo(), end, visited, path)) return true; // Recur
        }
        path.remove(path.size() - 1);                // Backtrack if path not found
        return false;
    }

    // Finds all possible routes from start to end using DFS
    // Stops recursion at maxDepth and avoids stations in the avoid set
    public List<List<Station>> findAllRoutesDFS(Station start, Station end,
                                                Set<Station> avoid, int maxDepth) {
        List<List<Station>> results = new ArrayList<>(); // Stores all valid paths
        Deque<Station> path = new ArrayDeque<>();        // Stack-like structure for current path
        dfsAll(start, end, avoid, new HashSet<>(avoid), path, results, maxDepth);
        return results;
    }

    // Recursive DFS to collect all valid paths up to maxDepth
    private void dfsAll(Station cur, Station end, Set<Station> avoid,
                        Set<Station> visited, Deque<Station> path,
                        List<List<Station>> results, int maxDepth) {
        if (visited.contains(cur) || path.size() > maxDepth) return; // Stop if visited or over depth
        visited.add(cur);
        path.addLast(cur);
        if (cur.equals(end)) {
            results.add(new ArrayList<>(path)); // Found valid path
        } else {
            for (Edge e : graph.getNeighbors(cur)) {
                dfsAll(e.getTo(), end, avoid, visited, path, results, maxDepth);
            }
        }
        path.removeLast(); // Backtrack
        visited.remove(cur);
    }

    // Dijkstra's algorithm without any penalty for line changes
    public List<Station> dijkstra(Station start, Station end, Set<Station> avoid) {
        return dijkstraInternal(start, end, avoid, 0.0);
    }

    // Dijkstra's algorithm with a penalty for switching train lines
    public List<Station> dijkstraWithPenalty(Station start, Station end,
                                             Set<Station> avoid, double penalty) {
        return dijkstraInternal(start, end, avoid, penalty);
    }

    // Internal method that runs Dijkstra's algorithm with optional line-change penalty
    private List<Station> dijkstraInternal(Station start, Station end,
                                           Set<Station> avoid, double penalty) {
        // Class representing a state in the priority queue
        class State implements Comparable<State> {
            Station station;
            String lastLine;
            double dist; // Cost to reach this state
            State(Station s, String l, double d) { station = s; lastLine = l; dist = d; }
            @Override public int compareTo(State o) { return Double.compare(dist, o.dist); }
        }

        Map<Station, Double> best = new HashMap<>();      // Best known cost to each station
        Map<Station, Station> prev = new HashMap<>();     // For path reconstruction
        Map<Station, String> prevLine = new HashMap<>();  // Last line used for each station

        PriorityQueue<State> pq = new PriorityQueue<>();
        pq.add(new State(start, null, 0.0));              // Start state
        best.put(start, 0.0);

        while (!pq.isEmpty()) {
            State cur = pq.poll();                        // Get station with lowest cost
            if (cur.dist > best.getOrDefault(cur.station, Double.MAX_VALUE)) continue;
            if (cur.station.equals(end)) break;           // Stop if reached the destination

            for (Edge e : graph.getNeighbors(cur.station)) {
                Station nb = e.getTo();
                if (avoid.contains(nb)) continue;         // Skip avoided stations
                double cost = cur.dist + e.getWeight();   // Basic edge weight
                if (cur.lastLine != null && !cur.lastLine.equals(e.getLine())) {
                    cost += penalty;                      // Add penalty if line changes
                }
                if (cost < best.getOrDefault(nb, Double.MAX_VALUE)) {
                    best.put(nb, cost);                   // Update best cost
                    prev.put(nb, cur.station);            // Update previous station
                    prevLine.put(nb, e.getLine());        // Track line used
                    pq.add(new State(nb, e.getLine(), cost));
                }
            }
        }

        // Reconstruct shortest path from end to start using 'prev'
        List<Station> path = new ArrayList<>();
        if (!best.containsKey(end)) return path;          // No path found
        for (Station at = end; at != null; at = prev.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);                        // Reverse to get correct order
        return path;
    }

    // Finds a complete route that includes multiple waypoints in order
    // Each leg is found using either Dijkstra (with optional penalty) or DFS
    public List<Station> routeWithWaypoints(List<Station> waypoints,
                                            boolean useDijkstra,
                                            double penalty,
                                            Set<Station> avoid) {
        List<Station> full = new ArrayList<>();
        for (int i = 0; i < waypoints.size() - 1; i++) {
            Station a = waypoints.get(i), b = waypoints.get(i + 1);
            List<Station> segment = useDijkstra
                    ? dijkstraInternal(a, b, avoid, penalty)   // Use Dijkstra
                    : findAnyRoute(a, b, avoid);              // Use DFS
            if (segment.isEmpty()) return Collections.emptyList(); // Abort if any segment fails
            if (i > 0) segment.remove(0);                     // Remove duplicate station (waypoint)
            full.addAll(segment);
        }
        return full;
    }
}