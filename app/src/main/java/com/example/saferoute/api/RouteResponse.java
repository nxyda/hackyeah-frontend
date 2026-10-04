package com.example.saferoute.api;

import java.util.List;

public class RouteResponse {

    public RouteOption fastest;
    public RouteOption safest;
    public List<RouteOption> alternatives;

    public static class RouteOption {
        public Geometry geometry;
        public double duration_s;
        public double distance_m;
        public double safety_score;
        public String explanation;
    }

    public static class Geometry {
        public List<List<Double>> coordinates;
    }
}
