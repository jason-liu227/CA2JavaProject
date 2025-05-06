package com.example.ca2realrealsproject;

import java.util.HashSet;
import java.util.Set;

public class Station {
    private final String name;
    private final Set<String> lines = new HashSet<>();

    public Station(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Set<String> getLines() {
        return lines;
    }

    public void addLine(String line) {
        lines.add(line);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Station)) return false;
        Station station = (Station) o;
        return name.equals(station.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
