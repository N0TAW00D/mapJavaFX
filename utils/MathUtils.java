package javaproject.utils;

import javaproject.models.ControlPoint;
import javaproject.models.Node;

public class MathUtils {
    
    public static double calculateDistance(Node n1, Node n2, double scaleRatio) {
        double dx = n2.x - n1.x;
        double dy = n2.y - n1.y;
        return Math.sqrt(dx * dx + dy * dy) * scaleRatio;
    }

    public static double calculateCurveLength(Node p0, ControlPoint cp, Node p2, double scaleRatio) {
        int steps = 20;
        double length = 0;
        double prevX = p0.x;
        double prevY = p0.y;

        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            double x = Math.pow(1 - t, 2) * p0.x + 2 * (1 - t) * t * cp.x + Math.pow(t, 2) * p2.x;
            double y = Math.pow(1 - t, 2) * p0.y + 2 * (1 - t) * t * cp.y + Math.pow(t, 2) * p2.y;

            double dx = x - prevX;
            double dy = y - prevY;
            length += Math.sqrt(dx * dx + dy * dy);

            prevX = x;
            prevY = y;
        }

        return length * scaleRatio;
    }
}
