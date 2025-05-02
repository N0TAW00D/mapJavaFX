package javaproject.models;

import javaproject.utils.MathUtils;

public class Edge {
    public Node node1, node2;
    public boolean curved;
    public ControlPoint controlPoint;

    public Edge(Node node1, Node node2, boolean curved, ControlPoint controlPoint) {
        this.node1 = node1;
        this.node2 = node2;
        this.curved = curved;
        this.controlPoint = controlPoint;
    }

    public double getLength(double scaleRatio) {
        if (curved && controlPoint != null) {
            return MathUtils.calculateCurveLength(node1, controlPoint, node2, scaleRatio);
        } else {
            return MathUtils.calculateDistance(node1, node2, scaleRatio);
        }
    }
}
