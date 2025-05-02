package javaproject.models;

public class Node {
    public double x, y;
    public String label;
    public boolean isSpecial;

    public Node(double x, double y, String label, Boolean isSpecial) {
        this.x = x;
        this.y = y;
        this.label = label;
        this.isSpecial = isSpecial;
    }
}
