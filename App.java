package javaproject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class App extends Application {

    private Canvas canvas;
    private GraphicsContext gc;
    private List<Node> nodes = new ArrayList<>();
    private List<Edge> edges = new ArrayList<>();
    private Node selectedNode = null;
    private Node draggedNode = null;
    private boolean creatingEdge = false;
    private boolean curvedEdgeMode = false;
    private double dragOffsetX, dragOffsetY;
    
    // New fields for image and scale
    private Image backgroundImage;
    private double scaleRatio = 1.0; // pixels per unit
    private String unitName = "m"; // default unit
    private boolean showDistances = true;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        canvas = new Canvas(1000, 700);
        gc = canvas.getGraphicsContext2D();
        
        // Create toolbar
        ToolBar toolbar = new ToolBar();
        Button addNodeBtn = new Button("Add Node");
        Button straightEdgeBtn = new Button("Straight Edge");
        Button curvedEdgeBtn = new Button("Curved Edge");
        Button clearBtn = new Button("Clear All");
        Button saveBtn = new Button("Save");
        Button loadBtn = new Button("Load");
        Button importImageBtn = new Button("Import Image");
        Button setScaleBtn = new Button("Set Scale");
        CheckBox showDistanceCheck = new CheckBox("Show Distances");
        showDistanceCheck.setSelected(true);
        
        toolbar.getItems().addAll(
            addNodeBtn, straightEdgeBtn, curvedEdgeBtn, 
            new Separator(), clearBtn, saveBtn, loadBtn,
            new Separator(), importImageBtn, setScaleBtn, showDistanceCheck
        );
        
        // Status bar
        Label statusBar = new Label("Ready");
        HBox statusBox = new HBox(statusBar);
        statusBox.setStyle("-fx-background-color: #eee; -fx-padding: 5px;");
        
        root.setTop(toolbar);
        root.setCenter(canvas);
        root.setBottom(statusBox);
        
        // Event handlers
        addNodeBtn.setOnAction(e -> {
            statusBar.setText("Mode: Add Node - Click on canvas to add a node");
            creatingEdge = false;
            curvedEdgeMode = false;
        });
        
        
        straightEdgeBtn.setOnAction(e -> {
            statusBar.setText("Mode: Straight Edge - Click two nodes to connect them");
            creatingEdge = true;
            curvedEdgeMode = false;
            selectedNode = null;
        });
        
        curvedEdgeBtn.setOnAction(e -> {
            statusBar.setText("Mode: Curved Edge - Click two nodes to connect them with a curve");
            creatingEdge = true;
            curvedEdgeMode = true;
            selectedNode = null;
        });
        
        clearBtn.setOnAction(e -> {
            nodes.clear();
            edges.clear();
            selectedNode = null;
            redrawCanvas();
            statusBar.setText("Cleared all nodes and edges");
        });
        
        saveBtn.setOnAction(e -> saveMap(primaryStage));
        loadBtn.setOnAction(e -> loadMap(primaryStage));
        importImageBtn.setOnAction(e -> importImage(primaryStage));
        setScaleBtn.setOnAction(e -> setScale(primaryStage));
        
        showDistanceCheck.setOnAction(e -> {
            showDistances = showDistanceCheck.isSelected();
            redrawCanvas();
        });
        
        // Mouse event handlers
        canvas.setOnMouseClicked(this::handlePrimaryClick);
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(e -> {
            draggedNode = null;
            redrawCanvas();
        });
        
        canvas.setOnMouseMoved(e -> {
            if (creatingEdge && selectedNode != null) {
                redrawCanvas();
                gc.setStroke(Color.GRAY);
                gc.setLineDashes(5);
                
                Node hoveredNode = getNodeAt(e.getX(), e.getY());
                if (hoveredNode != null && hoveredNode != selectedNode) {
                    if (curvedEdgeMode) {
                        drawCurvedEdge(selectedNode, hoveredNode, Color.GRAY);
                    } else {
                        gc.strokeLine(selectedNode.x, selectedNode.y, hoveredNode.x, hoveredNode.y);
                    }
                    drawDistanceLabel(selectedNode, hoveredNode, Color.GRAY);
                } else {
                    if (curvedEdgeMode) {
                        drawCurvedEdge(selectedNode, new Node(e.getX(), e.getY(), 0, ""), Color.GRAY);
                    } else {
                        gc.strokeLine(selectedNode.x, selectedNode.y, e.getX(), e.getY());
                    }
                }
                gc.setLineDashes(0);
            }
        });
        
        redrawCanvas();
        Scene scene = new Scene(root);
        primaryStage.setTitle("MiniMap Builder");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private void importImage(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Map Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(primaryStage);
        
        if (file != null) {
            try {
                backgroundImage = new Image(file.toURI().toString());
                // Resize canvas to fit image
                canvas.setWidth(backgroundImage.getWidth());
                
                canvas.setHeight(backgroundImage.getHeight());
                redrawCanvas();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void setScale(Stage primaryStage) {
        TextInputDialog dialog = new TextInputDialog("1.0");
        dialog.setTitle("Set Map Scale");
        dialog.setHeaderText("Enter the real-world width of the map image");
        dialog.setContentText("Real width (" + unitName + "):");
        
        dialog.showAndWait().ifPresent(input -> {
            try {
                double realWidth = Double.parseDouble(input);
                if (backgroundImage != null && realWidth > 0) {
                    scaleRatio = realWidth / backgroundImage.getWidth();
                    // Ask for unit name
                    TextInputDialog unitDialog = new TextInputDialog(unitName);
                    unitDialog.setTitle("Set Units");
                    unitDialog.setHeaderText("Enter the unit of measurement");
                    unitDialog.setContentText("Unit (e.g., m, km, ft):");
                    
                    unitDialog.showAndWait().ifPresent(unit -> {
                        unitName = unit;
                        redrawCanvas();
                    });
                }
            } catch (NumberFormatException e) {
                // Invalid input
            }
        });
    }
    
    private void handlePrimaryClick(MouseEvent e) {
        if (!creatingEdge) {
            // Node Size
            Node newNode = new Node(e.getX(), e.getY(), 5, "Node " + (nodes.size() + 1));
            nodes.add(newNode);
            redrawCanvas();
        } else {
            Node clickedNode = getNodeAt(e.getX(), e.getY());
            if (clickedNode != null) {
                if (selectedNode == null) {
                    selectedNode = clickedNode;
                } else if (selectedNode != clickedNode) {
                    Edge edge = new Edge(selectedNode, clickedNode, curvedEdgeMode);
                    edges.add(edge);
                    selectedNode = null;
                    redrawCanvas();
                }
            }
            
        }
    }
    
    private void handleMousePressed(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY && !creatingEdge) {
            Node clickedNode = getNodeAt(e.getX(), e.getY());
            if (clickedNode != null) {
                draggedNode = clickedNode;
                dragOffsetX = e.getX() - clickedNode.x;
                dragOffsetY = e.getY() - clickedNode.y;
            }
        }
    }
    
    private void handleMouseDragged(MouseEvent e) {
        if (draggedNode != null) {
            draggedNode.x = e.getX() - dragOffsetX;
            draggedNode.y = e.getY() - dragOffsetY;
            redrawCanvas();
        }
    }
    
    private void redrawCanvas() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // Draw background image if exists
        if (backgroundImage != null) {
            gc.drawImage(backgroundImage, 0, 0);
        }
        
        // Draw edges
        for (Edge edge : edges) {
            if (edge.curved) {
                drawCurvedEdge(edge.node1, edge.node2, Color.BLACK);
            } else {
                gc.setStroke(Color.BLACK);
                gc.strokeLine(edge.node1.x, edge.node1.y, edge.node2.x, edge.node2.y);
            }
            
            if (showDistances) {
                drawDistanceLabel(edge.node1, edge.node2, Color.BLUE);
            }
        }
        
        // Draw nodes
        for (Node node : nodes) {
            gc.setFill(Color.LIGHTBLUE);
            gc.fillOval(node.x - node.radius, node.y - node.radius, node.radius * 2, node.radius * 2);
            gc.setStroke(Color.DARKBLUE);
            gc.strokeOval(node.x - node.radius, node.y - node.radius, node.radius * 2, node.radius * 2);
            
            // Draw node label
            gc.setFill(Color.BLACK);
            // Node Size
            gc.fillText(node.label, node.x - 10, node.y + node.radius + 5);
        }
        
        // Highlight selected node
        if (selectedNode != null) {
            gc.setStroke(Color.RED);
            gc.setLineWidth(2);
            gc.strokeOval(selectedNode.x - selectedNode.radius - 2, 
                          selectedNode.y - selectedNode.radius - 2, 
                          selectedNode.radius * 2 + 4, 
                          selectedNode.radius * 2 + 4);
            gc.setLineWidth(1);
        }
    }
    
    private void drawCurvedEdge(Node node1, Node node2, Color color) {
        gc.setStroke(color);
        
        double controlX = (node1.x + node2.x) / 2;
        double controlY = (node1.y + node2.y) / 2 + 50;
        
        gc.beginPath();
        gc.moveTo(node1.x, node1.y);
        gc.quadraticCurveTo(controlX, controlY, node2.x, node2.y);
        gc.stroke();
    }
    
    private void drawDistanceLabel(Node node1, Node node2, Color color) {
        double dx = node2.x - node1.x;
        double dy = node2.y - node1.y;
        double distancePx = Math.sqrt(dx * dx + dy * dy);
        double distanceReal = distancePx * scaleRatio;
        
        String distanceText = String.format("%.2f %s", distanceReal, unitName);
        
        double midX = (node1.x + node2.x) / 2;
        double midY = (node1.y + node2.y) / 2;
        
        gc.setFill(Color.WHITE);
        gc.fillRect(midX - 30, midY - 10, 60, 20);
        gc.setStroke(color);
        gc.strokeRect(midX - 30, midY - 10, 60, 20);
        gc.setFill(color);
        gc.fillText(distanceText, midX - 25, midY + 5);
    }
    
    private Node getNodeAt(double x, double y) {
        for (Node node : nodes) {
            double dx = x - node.x;
            double dy = y - node.y;
            if (dx * dx + dy * dy <= node.radius * node.radius) {
                return node;
            }
        }
        return null;
    }
    
    private void saveMap(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Map");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showSaveDialog(primaryStage);
        
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                JSONObject mapData = new JSONObject();
                
                // Save nodes
                JSONArray nodesArray = new JSONArray();
                for (Node node : nodes) {
                    JSONObject nodeObj = new JSONObject();
                    nodeObj.put("x", node.x);
                    nodeObj.put("y", node.y);
                    nodeObj.put("radius", node.radius);
                    nodeObj.put("label", node.label);
                    nodesArray.put(nodeObj);
                }
                mapData.put("nodes", nodesArray);
                
                // Save edges
                JSONArray edgesArray = new JSONArray();
                for (Edge edge : edges) {
                    JSONObject edgeObj = new JSONObject();
                    edgeObj.put("node1Index", nodes.indexOf(edge.node1));
                    edgeObj.put("node2Index", nodes.indexOf(edge.node2));
                    edgeObj.put("curved", edge.curved);
                    edgesArray.put(edgeObj);
                }
                mapData.put("edges", edgesArray);
                
                writer.write(mapData.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void loadMap(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Map");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showOpenDialog(primaryStage);
        
        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                
                JSONObject mapData = new JSONObject(content.toString());
                
                // Clear current map
                nodes.clear();
                edges.clear();
                selectedNode = null;
                
                // Load nodes
                JSONArray nodesArray = mapData.getJSONArray("nodes");
                for (int i = 0; i < nodesArray.length(); i++) {
                    JSONObject nodeObj = nodesArray.getJSONObject(i);
                    Node node = new Node(
                        nodeObj.getDouble("x"),
                        nodeObj.getDouble("y"),
                        nodeObj.getDouble("radius"),
                        nodeObj.getString("label")
                    );
                    nodes.add(node);
                }
                
                // Load edges
                JSONArray edgesArray = mapData.getJSONArray("edges");
                for (int i = 0; i < edgesArray.length(); i++) {
                    JSONObject edgeObj = edgesArray.getJSONObject(i);
                    int node1Index = edgeObj.getInt("node1Index");
                    int node2Index = edgeObj.getInt("node2Index");
                    boolean curved = edgeObj.getBoolean("curved");
                    
                    if (node1Index >= 0 && node1Index < nodes.size() &&
                        node2Index >= 0 && node2Index < nodes.size()) {
                        Edge edge = new Edge(
                            nodes.get(node1Index),
                            nodes.get(node2Index),
                            curved
                        );
                        edges.add(edge);
                    }
                }
                
                redrawCanvas();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
    
    class Node {
        double x, y;
        double radius;
        String label;
        
        Node(double x, double y, double radius, String label) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.label = label;
        }
    }
    
    class Edge {
        Node node1, node2;
        boolean curved;
        
        Edge(Node node1, Node node2, boolean curved) {
            this.node1 = node1;
            this.node2 = node2;
            this.curved = curved;
        }
    }
}