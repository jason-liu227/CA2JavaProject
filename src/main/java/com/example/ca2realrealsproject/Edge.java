package com.example.ca2realrealsproject;

public class Edge {
    private final Station from;
    private final Station to;
    private final double weight;
    private final String line;

    public Edge(Station from, Station to, double weight, String line) {
        this.from = from;
        this.to = to;
        this.weight = weight;
        this.line = line;
    }

    public Station getFrom() { return from; }
    public Station getTo() { return to; }
    public double getWeight() { return weight; }
    public String getLine() { return line; }
}
