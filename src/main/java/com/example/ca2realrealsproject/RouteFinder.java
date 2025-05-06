package com.example.ca2realrealsproject;

import java.util.*;
import java.util.stream.Collectors;

public class RouteFinder {
    private final Graph graph;

    public RouteFinder(Graph graph) {
        this.graph = graph;
    }

    /** 1. Any single route via DFS */
    public List<Station> findAnyRoute(Station start, Station end, Set<Station> avoid) {
        Set<Station> visited = new HashSet<>(avoid);
        List<Station> path = new ArrayList<>();
        return dfsAny(start, end, visited, path) ? path : Collections.emptyList();
    }

    private boolean dfsAny(Station cur, Station end, Set<Station> visited, List<Station> path) {
        if (visited.contains(cur)) return false;
        visited.add(cur);
        path.add(cur);
        if (cur.equals(end)) return true;
        for (Edge e : graph.getNeighbors(cur)) {
            if (dfsAny(e.getTo(), end, visited, path)) return true;
        }
        path.remove(path.size()-1);
        return false;
    }

    /** 2. All routes (up to maxDepth) via DFS permutations */
    public List<List<Station>> findAllRoutesDFS(Station start, Station end,
                                                Set<Station> avoid, int maxDepth) {
        List<List<Station>> results = new ArrayList<>();
        Deque<Station> path = new ArrayDeque<>();
        dfsAll(start, end, avoid, new HashSet<>(avoid), path, results, maxDepth);
        return results;
    }

    private void dfsAll(Station cur, Station end, Set<Station> avoid,
                        Set<Station> visited, Deque<Station> path,
                        List<List<Station>> results, int maxDepth) {
        if (visited.contains(cur) || path.size() > maxDepth) return;
        visited.add(cur);
        path.addLast(cur);
        if (cur.equals(end)) {
            results.add(new ArrayList<>(path));
        } else {
            for (Edge e : graph.getNeighbors(cur)) {
                dfsAll(e.getTo(), end, avoid, visited, path, results, maxDepth);
            }
        }
        path.removeLast();
        visited.remove(cur);
    }

    /** 3. Dijkstra without penalty */
    public List<Station> dijkstra(Station start, Station end, Set<Station> avoid) {
        return dijkstraInternal(start, end, avoid, 0.0);
    }

    /** 4. Dijkstra with line-change penalty */
    public List<Station> dijkstraWithPenalty(Station start, Station end,
                                             Set<Station> avoid, double penalty) {
        return dijkstraInternal(start, end, avoid, penalty);
    }

    private List<Station> dijkstraInternal(Station start, Station end,
                                           Set<Station> avoid, double penalty) {
        // state: Station + lastLine
        class State implements Comparable<State> {
            Station station;
            String lastLine;
            double dist;
            State(Station s, String l, double d) { station = s; lastLine = l; dist = d; }
            @Override public int compareTo(State o) { return Double.compare(dist, o.dist); }
        }

        Map<Station, Double> best = new HashMap<>();
        Map<Station, Station> prev = new HashMap<>();
        Map<Station, String> prevLine = new HashMap<>();

        PriorityQueue<State> pq = new PriorityQueue<>();
        pq.add(new State(start, null, 0.0));
        best.put(start, 0.0);

        while (!pq.isEmpty()) {
            State cur = pq.poll();
            if (cur.dist > best.getOrDefault(cur.station, Double.MAX_VALUE)) continue;
            if (cur.station.equals(end)) break;

            for (Edge e : graph.getNeighbors(cur.station)) {
                Station nb = e.getTo();
                if (avoid.contains(nb)) continue;
                double cost = cur.dist + e.getWeight();
                if (cur.lastLine != null && !cur.lastLine.equals(e.getLine())) {
                    cost += penalty;
                }
                if (cost < best.getOrDefault(nb, Double.MAX_VALUE)) {
                    best.put(nb, cost);
                    prev.put(nb, cur.station);
                    prevLine.put(nb, e.getLine());
                    pq.add(new State(nb, e.getLine(), cost));
                }
            }
        }

        // reconstruct
        List<Station> path = new ArrayList<>();
        if (!best.containsKey(end)) return path;
        for (Station at = end; at != null; at = prev.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
        return path;
    }

    /** 5. Route with waypoints */
    public List<Station> routeWithWaypoints(List<Station> waypoints,
                                            boolean useDijkstra,
                                            double penalty,
                                            Set<Station> avoid) {
        List<Station> full = new ArrayList<>();
        for (int i = 0; i < waypoints.size() - 1; i++) {
            Station a = waypoints.get(i), b = waypoints.get(i+1);
            List<Station> segment = useDijkstra
                    ? dijkstraInternal(a, b, avoid, penalty)
                    : findAnyRoute(a, b, avoid);
            if (segment.isEmpty()) return Collections.emptyList();
            // avoid duplicating junction
            if (i > 0) segment.remove(0);
            full.addAll(segment);
        }
        return full;
    }
}