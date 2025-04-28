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
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
    private ControlPoint controlPoint = null;
    private boolean creatingEdge = false;
    private boolean curvedEdgeMode = false;
    private double dragOffsetX, dragOffsetY;

    // New fields for image and scale
    private Image backgroundImage;
    private double scaleRatio = 1.0; // pixels per unit
    private String unitName = "m"; // default unit
    private boolean showDistances = true;
    private boolean isAddNode = true;

    // UI elements for lists
    private ListView<String> nodeListView = new ListView<>();
    private ListView<String> edgeListView = new ListView<>();

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        canvas = new Canvas(1000, 700);
        gc = canvas.getGraphicsContext2D();

        // Wrap Canvas in a Group then in a ScrollPane
        javafx.scene.Group canvasGroup = new javafx.scene.Group(canvas);
        ScrollPane scrollPane = new ScrollPane(canvasGroup);
        scrollPane.setPannable(false);

        // Create toolbar
        ToolBar toolbar = new ToolBar();
        Button addNodeBtn = new Button("Add Node");
        CheckBox addNodeCheck = new CheckBox("Add Node");
        addNodeCheck.setSelected(true);

        Button straightEdgeBtn = new Button("Straight Edge");
        Button curvedEdgeBtn = new Button("Curved Edge");
        Button clearBtn = new Button("Clear All");
        Button saveBtn = new Button("Save");
        Button loadBtn = new Button("Load");
        Button importImageBtn = new Button("Import Image");
        Button setScaleBtn = new Button("Set Scale");
        Button zoomInBtn = new Button("Zoom In (+)");
        Button zoomOutBtn = new Button("Zoom Out (-)");
        Button renameNodeBtn = new Button("Rename Node");
        Button deleteSelectedBtn = new Button("Delete Selected");

        CheckBox showDistanceCheck = new CheckBox("Show Distances");
        showDistanceCheck.setSelected(true);

        toolbar.getItems().addAll(
                addNodeCheck, addNodeBtn, straightEdgeBtn, curvedEdgeBtn,
                new Separator(), clearBtn, saveBtn, loadBtn,
                new Separator(), importImageBtn, setScaleBtn, showDistanceCheck,
                new Separator(), zoomInBtn, zoomOutBtn, renameNodeBtn, deleteSelectedBtn
        );

        // Status bar
        Label statusBar = new Label("Ready");
        HBox statusBox = new HBox(statusBar);
        statusBox.setStyle("-fx-background-color: #eee; -fx-padding: 5px;");

        // Right panel for lists
        VBox rightPanel = new VBox();
        rightPanel.setPrefWidth(200);
        rightPanel.getChildren().addAll(
                new Label("Nodes:"),
                nodeListView,
                new Label("Edges:"),
                edgeListView
        );

        root.setTop(toolbar);
        root.setCenter(scrollPane);
        root.setRight(rightPanel);
        root.setBottom(statusBox);

        // Event handlers
        addNodeBtn.setOnAction(e -> {
            statusBar.setText("Mode: Add Node - Click on canvas to add a node");
            creatingEdge = false;
            curvedEdgeMode = false;
            selectedNode = null;
            controlPoint = null;
        });

        straightEdgeBtn.setOnAction(e -> {
            statusBar.setText("Mode: Straight Edge - Click two nodes to connect them");
            creatingEdge = true;
            curvedEdgeMode = false;
            selectedNode = null;
            controlPoint = null;
        });

        curvedEdgeBtn.setOnAction(e -> {
            statusBar.setText("Mode: Curved Edge - Click two nodes to connect them with a curve");
            creatingEdge = true;
            curvedEdgeMode = true;
            selectedNode = null;
            controlPoint = null;
        });

        clearBtn.setOnAction(e -> {
            nodes.clear();
            edges.clear();
            selectedNode = null;
            controlPoint = null;
            updateLists();
            redrawCanvas();
            statusBar.setText("Cleared all nodes and edges");
        });

        zoomInBtn.setOnAction(e -> zoom(1.1));
        zoomOutBtn.setOnAction(e -> zoom(1 / 1.1));

        saveBtn.setOnAction(e -> saveMap(primaryStage));
        loadBtn.setOnAction(e -> loadMap(primaryStage));
        importImageBtn.setOnAction(e -> importImage(primaryStage));
        setScaleBtn.setOnAction(e -> setScale(primaryStage));

        addNodeCheck.setOnAction(e -> {
            isAddNode = addNodeCheck.isSelected();
        });

        showDistanceCheck.setOnAction(e -> {
            showDistances = showDistanceCheck.isSelected();
            redrawCanvas();
        });

        renameNodeBtn.setOnAction(e -> renameSelectedNode(primaryStage));

        deleteSelectedBtn.setOnAction(e -> {
            deleteSelected();
            updateLists();
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
                        if (controlPoint == null) {
                            // Create a temporary control point at the midpoint
                            double midX = (selectedNode.x + hoveredNode.x) / 2;
                            double midY = (selectedNode.y + hoveredNode.y) / 2;
                            drawCurvedEdge(selectedNode, hoveredNode, new ControlPoint(midX, midY), Color.GRAY);
                        } else {
                            drawCurvedEdge(selectedNode, hoveredNode, controlPoint, Color.GRAY);
                        }
                    } else {
                        gc.strokeLine(selectedNode.x, selectedNode.y, hoveredNode.x, hoveredNode.y);
                    }
                    drawDistanceLabel(selectedNode, hoveredNode, Color.GRAY);
                } else {
                    if (curvedEdgeMode) {
                        if (controlPoint == null) {
                            // Create a temporary control point at the midpoint
                            double midX = (selectedNode.x + e.getX()) / 2;
                            double midY = (selectedNode.y + e.getY()) / 2;
                            drawCurvedEdge(selectedNode, new Node(e.getX(), e.getY(), 0, ""),
                                    new ControlPoint(midX, midY), Color.GRAY);
                        } else {
                            drawCurvedEdge(selectedNode, new Node(e.getX(), e.getY(), 0, ""),
                                    controlPoint, Color.GRAY);
                        }
                    } else {
                        gc.strokeLine(selectedNode.x, selectedNode.y, e.getX(), e.getY());
                    }
                }
                gc.setLineDashes(0);
            }
        });

        // List view selection handlers
        nodeListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                int index = nodeListView.getSelectionModel().getSelectedIndex();
                if (index >= 0 && index < nodes.size()) {
                    selectedNode = nodes.get(index);
                    edgeListView.getSelectionModel().clearSelection();
                    controlPoint = null;
                    redrawCanvas();
                }
            }
        });

        edgeListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                int index = edgeListView.getSelectionModel().getSelectedIndex();
                if (index >= 0 && index < edges.size()) {
                    Edge edge = edges.get(index);
                    selectedNode = null;
                    controlPoint = edge.controlPoint;
                    redrawCanvas();
                    // Highlight the selected edge
                    gc.setStroke(Color.RED);
                    gc.setLineWidth(2);
                    if (edge.curved) {
                        drawCurvedEdge(edge.node1, edge.node2, edge.controlPoint, Color.RED);
                    } else {
                        gc.strokeLine(edge.node1.x, edge.node1.y, edge.node2.x, edge.node2.y);
                    }
                    gc.setLineWidth(1);
                }
            }
        });

        redrawCanvas();
        Scene scene = new Scene(root);
        primaryStage.setTitle("MiniMap Builder");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void renameSelectedNode(Stage primaryStage) {
        if (selectedNode == null) {
            // Check if a node is selected in the list view
            int selectedIndex = nodeListView.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < nodes.size()) {
                selectedNode = nodes.get(selectedIndex);
            } else {
                return; // No node selected
            }
        }

        TextInputDialog dialog = new TextInputDialog(selectedNode.label);
        dialog.setTitle("Rename Node");
        dialog.setHeaderText("Enter new name for the node");
        dialog.setContentText("Node name:");

        dialog.showAndWait().ifPresent(newName -> {
            selectedNode.label = newName;
            updateLists();
            redrawCanvas();
        });
    }

    private void updateLists() {
        nodeListView.getItems().clear();
        for (Node node : nodes) {
            nodeListView.getItems().add(String.format("%s (%.1f, %.1f)", node.label, node.x, node.y));
        }

        edgeListView.getItems().clear();
        for (Edge edge : edges) {
            String edgeType = edge.curved ? "Curved" : "Straight";
            double distance;
            if (edge.curved) {
                distance = calculateCurveLength(edge.node1, edge.controlPoint, edge.node2);
            } else {
                distance = calculateDistance(edge.node1, edge.node2);
            }
            edgeListView.getItems().add(String.format("%s -> %s (%s, %.1f %s)",
                    edge.node1.label, edge.node2.label, edgeType, distance, unitName));
        }
    }

    private double calculateDistance(Node node1, Node node2) {
        double dx = node2.x - node1.x;
        double dy = node2.y - node1.y;
        return Math.sqrt(dx * dx + dy * dy) * scaleRatio;
    }

    private double calculateCurveLength(Node p0, ControlPoint p1, Node p2) {
        // Approximate the curve length by dividing it into small segments
        int steps = 20; // More steps = more accurate but slower
        double length = 0;
        double prevX = p0.x;
        double prevY = p0.y;

        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            // Calculate point on curve at t
            double x = Math.pow(1 - t, 2) * p0.x + 2 * (1 - t) * t * p1.x + Math.pow(t, 2) * p2.x;
            double y = Math.pow(1 - t, 2) * p0.y + 2 * (1 - t) * t * p1.y + Math.pow(t, 2) * p2.y;

            // Add distance from previous point
            double dx = x - prevX;
            double dy = y - prevY;
            length += Math.sqrt(dx * dx + dy * dy);

            prevX = x;
            prevY = y;
        }

        return length * scaleRatio; // Convert to real-world units
    }

    private void deleteSelected() {
        // Delete selected node and connected edges
        if (selectedNode != null) {
            // Remove edges connected to this node
            edges.removeIf(edge -> edge.node1 == selectedNode || edge.node2 == selectedNode);
            nodes.remove(selectedNode);
            selectedNode = null;
            controlPoint = null;
        }

        // Delete selected edge
        int selectedEdgeIndex = edgeListView.getSelectionModel().getSelectedIndex();
        if (selectedEdgeIndex >= 0 && selectedEdgeIndex < edges.size()) {
            edges.remove(selectedEdgeIndex);
            edgeListView.getSelectionModel().clearSelection();
            controlPoint = null;
        }
    }

    private void handlePrimaryClick(MouseEvent e) {
        if (!creatingEdge && isAddNode) {
            Node newNode = new Node(e.getX(), e.getY(), 5, String.valueOf(nodes.size() + 1));
            nodes.add(newNode);
            updateLists();
            redrawCanvas();
        } else {
            Node clickedNode = getNodeAt(e.getX(), e.getY());
            if (clickedNode != null) {
                if (selectedNode == null) {
                    selectedNode = clickedNode;
                    if (curvedEdgeMode) {
                        // Create a control point at the midpoint between selected node and mouse position
                        double midX = (selectedNode.x + e.getX()) / 2;
                        double midY = (selectedNode.y + e.getY()) / 2;
                        controlPoint = new ControlPoint(midX, midY);
                    }
                } else if (selectedNode != clickedNode) {
                    if (curvedEdgeMode) {
                        // If no control point was set (user didn't drag it), create one at midpoint
                        if (controlPoint == null) {
                            double midX = (selectedNode.x + clickedNode.x) / 2;
                            double midY = (selectedNode.y + clickedNode.y) / 2;
                            controlPoint = new ControlPoint(midX, midY);
                        }
                        Edge edge = new Edge(selectedNode, clickedNode, true, controlPoint);
                        edges.add(edge);
                    } else {
                        Edge edge = new Edge(selectedNode, clickedNode, false, null);
                        edges.add(edge);
                    }
                    selectedNode = null;
                    controlPoint = null;
                    updateLists();
                    redrawCanvas();
                }
            } else if (curvedEdgeMode && selectedNode != null && controlPoint != null) {
                // Check if clicking on control point to move it
                if (Math.abs(e.getX() - controlPoint.x) < 10 && Math.abs(e.getY() - controlPoint.y) < 10) {
                    // Control point is already being handled in mousePressed
                }
            }
        }
    }

    private void handleMousePressed(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            if (!creatingEdge) {
                Node clickedNode = getNodeAt(e.getX(), e.getY());
                if (clickedNode != null) {
                    draggedNode = clickedNode;
                    dragOffsetX = e.getX() - clickedNode.x;
                    dragOffsetY = e.getY() - clickedNode.y;
                }
            }

            // Check if clicking on control point to move it
            if (curvedEdgeMode && controlPoint != null) {
                if (Math.abs(e.getX() - controlPoint.x) < 10 && Math.abs(e.getY() - controlPoint.y) < 10) {
                    dragOffsetX = e.getX() - controlPoint.x;
                    dragOffsetY = e.getY() - controlPoint.y;
                }
            }
        }
    }

    private void handleMouseDragged(MouseEvent e) {
        if (draggedNode != null) {
            draggedNode.x = e.getX() - dragOffsetX;
            draggedNode.y = e.getY() - dragOffsetY;
            updateLists();
            redrawCanvas();
        } else if (controlPoint != null && curvedEdgeMode) {
            // Moving control point
            controlPoint.x = e.getX() - dragOffsetX;
            controlPoint.y = e.getY() - dragOffsetY;
            updateLists();
            redrawCanvas();
        }
    }

    private void redrawCanvas() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (backgroundImage != null) {
            gc.drawImage(backgroundImage, 0, 0);
        }

        // Draw edges
        // In redrawCanvas():
        for (Edge edge : edges) {
            if (edge.curved) {
                drawCurvedEdge(edge.node1, edge.node2, edge.controlPoint, Color.BLACK);
                if (showDistances) {
                    drawDistanceLabel(edge.node1, edge.node2, Color.BLUE);
                }
            } else {
                gc.setStroke(Color.BLACK);
                gc.strokeLine(edge.node1.x, edge.node1.y, edge.node2.x, edge.node2.y);
                if (showDistances) {
                    drawDistanceLabel(edge.node1, edge.node2, Color.BLUE);
                }
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
            gc.fillText(node.label, node.x - 10, node.y + node.radius + 5);
        }

        // Draw control point if in curved edge mode with a node selected
        if (curvedEdgeMode && selectedNode != null && controlPoint != null) {
            gc.setFill(Color.ORANGE);
            gc.fillOval(controlPoint.x - 5, controlPoint.y - 5, 10, 10);
            gc.setStroke(Color.DARKORANGE);
            gc.strokeOval(controlPoint.x - 5, controlPoint.y - 5, 10, 10);
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

    private void drawCurvedEdge(Node node1, Node node2, ControlPoint control, Color color) {
        gc.setStroke(color);
        gc.beginPath();
        gc.moveTo(node1.x, node1.y);
        gc.quadraticCurveTo(control.x, control.y, node2.x, node2.y);
        gc.stroke();

        // Draw control lines (optional)
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineDashes(2);
        gc.strokeLine(node1.x, node1.y, control.x, control.y);
        gc.strokeLine(node2.x, node2.y, control.x, control.y);
        gc.setLineDashes(0);
    }

    private void drawDistanceLabel(Node node1, Node node2, Color color) {
        double distance;
        if (curvedEdgeMode && controlPoint != null) {
            // For curves with control points (either being drawn or existing ones)
            distance = calculateCurveLength(node1, controlPoint, node2);
        } else {
            // For straight edges
            distance = calculateDistance(node1, node2);
        }

        String distanceText = String.format("%.2f %s", distance, unitName);
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

                JSONArray edgesArray = new JSONArray();
                for (Edge edge : edges) {
                    JSONObject edgeObj = new JSONObject();
                    edgeObj.put("node1Index", nodes.indexOf(edge.node1));
                    edgeObj.put("node2Index", nodes.indexOf(edge.node2));
                    edgeObj.put("curved", edge.curved);
                    if (edge.curved && edge.controlPoint != null) {
                        JSONObject controlObj = new JSONObject();
                        controlObj.put("x", edge.controlPoint.x);
                        controlObj.put("y", edge.controlPoint.y);
                        edgeObj.put("controlPoint", controlObj);
                    }
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

                nodes.clear();
                edges.clear();
                selectedNode = null;
                controlPoint = null;

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

                JSONArray edgesArray = mapData.getJSONArray("edges");
                for (int i = 0; i < edgesArray.length(); i++) {
                    JSONObject edgeObj = edgesArray.getJSONObject(i);
                    int node1Index = edgeObj.getInt("node1Index");
                    int node2Index = edgeObj.getInt("node2Index");
                    boolean curved = edgeObj.getBoolean("curved");
                    ControlPoint cp = null;

                    if (curved && edgeObj.has("controlPoint")) {
                        JSONObject cpObj = edgeObj.getJSONObject("controlPoint");
                        cp = new ControlPoint(cpObj.getDouble("x"), cpObj.getDouble("y"));
                    }

                    if (node1Index >= 0 && node1Index < nodes.size()
                            && node2Index >= 0 && node2Index < nodes.size()) {
                        Edge edge = new Edge(
                                nodes.get(node1Index),
                                nodes.get(node2Index),
                                curved,
                                cp
                        );
                        edges.add(edge);
                    }
                }

                updateLists();
                redrawCanvas();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void zoom(double zoomFactor) {
        canvas.setScaleX(canvas.getScaleX() * zoomFactor);
        canvas.setScaleY(canvas.getScaleY() * zoomFactor);
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
                    TextInputDialog unitDialog = new TextInputDialog(unitName);
                    unitDialog.setTitle("Set Units");
                    unitDialog.setHeaderText("Enter the unit of measurement");
                    unitDialog.setContentText("Unit (e.g., m, km, ft):");

                    unitDialog.showAndWait().ifPresent(unit -> {
                        unitName = unit;
                        updateLists();
                        redrawCanvas();
                    });
                }
            } catch (NumberFormatException e) {
                // Invalid input
            }
        });
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
        ControlPoint controlPoint;

        Edge(Node node1, Node node2, boolean curved, ControlPoint controlPoint) {
            this.node1 = node1;
            this.node2 = node2;
            this.curved = curved;
            this.controlPoint = controlPoint;
        }

        double getLength() {
            if (curved) {
                return calculateCurveLength(node1, controlPoint, node2);
            } else {
                return calculateDistance(node1, node2);
            }
        }
    }

    class ControlPoint {

        double x, y;

        ControlPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
