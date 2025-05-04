package javaproject.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import javaproject.models.Edge;
import javaproject.models.Node;

public class AStarPathfinder {
    
    public static class PathResult {
        public final List<Node> path;
        public final double totalDistance;
        
        public PathResult(List<Node> path, double totalDistance) {
            this.path = path;
            this.totalDistance = totalDistance;
        }
    }

    public static PathResult findPath(List<Node> nodes, List<Edge> edges, Node start, Node goal) {
        // Implementation of A* algorithm
        Map<Node, Double> gScore = new HashMap<>(); // Cost from start to current node
        Map<Node, Double> fScore = new HashMap<>(); // Estimated total cost
        Map<Node, Node> cameFrom = new HashMap<>(); // For path reconstruction
        
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> fScore.getOrDefault(n, Double.POSITIVE_INFINITY)));
        
        // Initialize scores
        for (Node node : nodes) {
            gScore.put(node, Double.POSITIVE_INFINITY);
            fScore.put(node, Double.POSITIVE_INFINITY);
        }
        
        gScore.put(start, 0.0);
        fScore.put(start, heuristic(start, goal));
        openSet.add(start);
        
        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            
            if (current.equals(goal)) {
                return new PathResult(reconstructPath(cameFrom, current), gScore.get(current));
            }
            
            for (Edge edge : getNeighbors(current, edges)) {
                Node neighbor = edge.node1.equals(current) ? edge.node2 : edge.node1;
                double tentativeGScore = gScore.get(current) + edge.getLength(1.0); // scaleRatio=1 for consistent heuristic
                
                if (tentativeGScore < gScore.get(neighbor)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeGScore);
                    fScore.put(neighbor, tentativeGScore + heuristic(neighbor, goal));
                    
                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }
        
        return new PathResult(Collections.emptyList(), 0); // No path found
    }
    
    private static List<Node> reconstructPath(Map<Node, Node> cameFrom, Node current) {
        List<Node> path = new ArrayList<>();
        path.add(current);
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(0, current);
        }
        return path;
    }
    
    private static List<Edge> getNeighbors(Node node, List<Edge> edges) {
        List<Edge> neighbors = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.node1.equals(node) || edge.node2.equals(node)) {
                neighbors.add(edge);
            }
        }
        return neighbors;
    }
    
    private static double heuristic(Node a, Node b) {
        // Euclidean distance heuristic
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}