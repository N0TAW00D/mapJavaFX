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
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javaproject.models.ControlPoint;
import javaproject.models.Edge;
import javaproject.models.Node;
import javaproject.utils.AStarPathfinder;
import javaproject.utils.ImageImporter;
import javaproject.utils.MathUtils;

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

    private int logCount = 0;

    private double radius = 5;

    // UI elements for lists
    private ListView<String> nodeListView = new ListView<>();
    private ListView<String> edgeListView = new ListView<>();

    private ComboBox<String> startNodeCombo = new ComboBox<>();
    private ComboBox<String> destinationNodeCombo = new ComboBox<>();
    private Button solvePathBtn = new Button("Solve Path");
    private TextArea solutionText = new TextArea();
    private CheckBox showPathCheck = new CheckBox("Show Path");
    private List<Edge> solutionPath = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        canvas = new Canvas(1000, 700);
        gc = canvas.getGraphicsContext2D();

        Scene scene = new Scene(root);

        // Wrap Canvas in a Group then in a ScrollPane
        javafx.scene.Group canvasGroup = new javafx.scene.Group(canvas);
        ScrollPane scrollPane = new ScrollPane(canvasGroup);
        scrollPane.setPannable(false);

        // Create toolbar
        ToolBar toolbar = new ToolBar();
        Button addNodeBtn = new Button("Add Node");
        CheckBox addNodeCheck = new CheckBox("");
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
        VBox leftPanel = new VBox();
        leftPanel.setPrefWidth(200);
        leftPanel.getChildren().addAll(
                new Label("Nodes:"),
                nodeListView,
                new Label("Edges:"),
                edgeListView
        );

        VBox rightPanel = new VBox(10);
        rightPanel.setPrefWidth(250);
        rightPanel.setPadding(new Insets(10));
        rightPanel.setStyle("-fx-background-color: #f5f5f5;");

        // Title
        Label titleLabel = new Label("Path Solver");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        // Start Node selection
        Label startLabel = new Label("Start Node:");
        startNodeCombo.setPrefWidth(Double.MAX_VALUE);

        // Destination Node selection
        Label destLabel = new Label("Destination Node:");
        destinationNodeCombo.setPrefWidth(Double.MAX_VALUE);

        // Solve button
        solvePathBtn.setPrefWidth(Double.MAX_VALUE);
        solvePathBtn.setStyle("-fx-base: #4CAF50;");

        // Solution display
        solutionText.setEditable(false);
        solutionText.setWrapText(true);
        solutionText.setPrefHeight(200);

        // Show path checkbox
        showPathCheck.setSelected(true);

        rightPanel.getChildren().addAll(
                titleLabel,
                new Separator(),
                startLabel,
                startNodeCombo,
                destLabel,
                destinationNodeCombo,
                solvePathBtn,
                new Separator(),
                showPathCheck,
                new Label("Solution:"),
                solutionText
        );

        nodeListView.getItems().addListener((ListChangeListener<String>) c -> updateNodeCombos());

        root.setTop(toolbar);
        root.setCenter(scrollPane);
        root.setLeft(leftPanel);
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

        importImageBtn.setOnAction(e -> {
            ImageImporter.importImage(
                    primaryStage,
                    canvas,
                    image -> this.backgroundImage = image,
                    this::redrawCanvas
            );
        });

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

        solvePathBtn.setOnAction(e -> solvePath());
        showPathCheck.setOnAction(e -> redrawCanvas());

        // Mouse event handlers
        canvas.setOnMouseClicked(this::handlePrimaryClick);
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);

        // scene.setOnKeyPressed(this::handleKeyPress);
        scene.setOnKeyPressed(event -> handleKeyPress(event, primaryStage));

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
                    // drawDistanceLabel(selectedNode, hoveredNode, Color.GRAY);
                    drawDistanceLabel(selectedNode, hoveredNode, Color.GRAY, false, null);
                } else {
                    if (curvedEdgeMode) {
                        if (controlPoint == null) {
                            // Create a temporary control point at the midpoint
                            double midX = (selectedNode.x + e.getX()) / 2;
                            double midY = (selectedNode.y + e.getY()) / 2;
                            drawCurvedEdge(selectedNode, new Node(e.getX(), e.getY(), "", false),
                                    new ControlPoint(midX, midY), Color.GRAY);
                        } else {
                            drawCurvedEdge(selectedNode, new Node(e.getX(), e.getY(), "", false),
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
                    gc.setLineWidth(3);
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
        // Scene scene = new Scene(root);
        primaryStage.setTitle("MiniMap Builder");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    private void solvePath() {
        // Get the selected node labels from combo boxes
        String startLabel = startNodeCombo.getSelectionModel().getSelectedItem();
        String destLabel = destinationNodeCombo.getSelectionModel().getSelectedItem();

        if (startLabel == null || destLabel == null || startLabel.equals(destLabel)) {
            solutionText.setText("Please select valid start and destination nodes");
            return;
        }

        // Find the actual Node objects by label
        Node start = null;
        Node dest = null;
        for (Node node : nodes) {
            if (node.label.equals(startLabel)) {
                start = node;
            }
            if (node.label.equals(destLabel)) {
                dest = node;
            }
            if (start != null && dest != null) {
                break; // Found both

            }
        }

        if (start == null || dest == null) {
            solutionText.setText("Selected nodes not found in graph");
            return;
        }

        // Clear previous solution
        solutionPath.clear();

        // Use A* algorithm
        AStarPathfinder.PathResult result = AStarPathfinder.findPath(nodes, edges, start, dest);

        // System.err.println("nodes: " + nodes.toString());
        // System.err.println();
        // System.err.println("edges: " + edges.toString());
        // System.err.println();
        // System.err.println("start: " + start.label);
        // System.err.println();
        // System.err.println("dest: " + dest.label);
        // System.err.println();

        if (result.path.isEmpty()) {
            solutionText.setText("No path found between " + start.label + " and " + dest.label);
            return;
        }

        // Convert path to edges for visualization
        solutionPath.clear();
        for (int i = 0; i < result.path.size() - 1; i++) {
            Node node1 = result.path.get(i);
            Node node2 = result.path.get(i + 1);

            // Find the edge between these nodes
            for (Edge edge : edges) {
                if ((edge.node1.equals(node1) && edge.node2.equals(node2))
                        || (edge.node1.equals(node2) && edge.node2.equals(node1))) {
                    solutionPath.add(edge);
                    break;
                }
            }
        }

        // Build solution text
        StringBuilder sb = new StringBuilder();
        sb.append("Path from ").append(start.label).append(" to ").append(dest.label).append(":\n");

        double totalDistance = 0;
        String activist = new String();

        activist = start.label;

        for (Edge edge : solutionPath) {
            double distance = edge.getLength(scaleRatio);
            totalDistance += distance;

            if (activist == edge.node1.label) {
                sb.append("- ").append(edge.node1.label).append(" → ").append(edge.node2.label)
                        .append(" (").append(String.format("%.2f", distance)).append(" ").append(unitName).append(")\n");
                activist = edge.node2.label;
            } else {
                sb.append("- ").append(edge.node2.label).append(" → ").append(edge.node1.label)
                        .append(" (").append(String.format("%.2f", distance)).append(" ").append(unitName).append(")\n");
                activist = edge.node1.label;
            }
        }

        sb.append("\nTotal distance: ").append(String.format("%.2f", totalDistance)).append(" ").append(unitName);
        solutionText.setText(sb.toString());

        redrawCanvas();
    }

    private void updateNodeCombos() {
        ObservableList<String> items = FXCollections.observableArrayList();
        for (Node node : nodes) {
            if (node.isSpecial == true) {
                items.add(node.label);
            }
        }

        String currentStart = startNodeCombo.getSelectionModel().getSelectedItem();
        String currentDest = destinationNodeCombo.getSelectionModel().getSelectedItem();

        startNodeCombo.setItems(items);
        destinationNodeCombo.setItems(items);

        if (currentStart != null) {
            startNodeCombo.getSelectionModel().select(currentStart);
        }
        if (currentDest != null) {
            destinationNodeCombo.getSelectionModel().select(currentDest);
        }
    }

    private void logChecker() {

        System.out.println();
        System.out.println("Log Checker -----------");
        System.out.println();

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

        // Create the checkbox
        CheckBox isSpecialCheck = new CheckBox("Is it Special Node?");
        isSpecialCheck.setSelected(selectedNode.isSpecial);

        // Create a container for the dialog content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Add components to the grid
        grid.add(new Label("Node name:"), 0, 0);
        grid.add(dialog.getEditor(), 1, 0);
        grid.add(isSpecialCheck, 0, 1, 2, 1);

        // Set the grid as the dialog's content
        dialog.getDialogPane().setContent(grid);

        // Focus on the text field by default
        Platform.runLater(() -> dialog.getEditor().requestFocus());

        dialog.showAndWait().ifPresent(newName -> {
            selectedNode.label = newName;
            selectedNode.isSpecial = isSpecialCheck.isSelected(); // Update the special status
            updateLists();
            redrawCanvas();
        });
    }

    private void updateLists() {
        nodeListView.getItems().clear();
        for (Node node : nodes) {
            nodeListView.getItems().add(String.format("%s (%.1f, %.1f, %b)", node.label, node.x, node.y, node.isSpecial));
        }

        edgeListView.getItems().clear();
        for (Edge edge : edges) {
            String edgeType = edge.curved ? "Curved" : "Straight";
            double distance;
            if (edge.curved) {
                distance = MathUtils.calculateCurveLength(edge.node1, edge.controlPoint, edge.node2, scaleRatio);
            } else {
                distance = MathUtils.calculateDistance(edge.node1, edge.node2, scaleRatio);
            }
            edgeListView.getItems().add(String.format("%s -> %s (%s, %.1f %s)",
                    edge.node1.label, edge.node2.label, edgeType, distance, unitName));
        }
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
            Node newNode = new Node(e.getX(), e.getY(), String.valueOf(nodes.size() + 1), false);
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

    private void clearFN(){
        selectedNode = null;
        controlPoint = null;
        creatingEdge = false;
    }

    private void handleMouseDragged(MouseEvent e) {
        if (draggedNode != null) {
            draggedNode.x = e.getX() - dragOffsetX;
            draggedNode.y = e.getY() - dragOffsetY;
            updateLists();
            redrawCanvas();
            clearFN();
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
        for (Edge edge : edges) {
            // Highlight solution path edges if showing
            boolean isSolutionEdge = showPathCheck.isSelected() && solutionPath.contains(edge);
            Color edgeColor = isSolutionEdge ? Color.MAGENTA : Color.BLACK;
            Color labelColor = isSolutionEdge ? Color.DARKGREEN : Color.BLUE;

            
            if(isSolutionEdge){
                gc.setLineWidth(3);
            } else {
                gc.setLineWidth(1);
            }

            if (edge.curved) {
                drawCurvedEdge(edge.node1, edge.node2, edge.controlPoint, edgeColor);

                if (showDistances) {
                    drawDistanceLabel(edge.node1, edge.node2, labelColor, true, edge.controlPoint);
                }
                
            } else {
                gc.setStroke(edgeColor);
                gc.strokeLine(edge.node1.x, edge.node1.y, edge.node2.x, edge.node2.y);
                if (showDistances) {
                    drawDistanceLabel(edge.node1, edge.node2, labelColor, false, null);
                }
            }
        }

        // Draw nodes
        for (Node node : nodes) {
            gc.setFill(Color.LIGHTBLUE);
            gc.fillOval(node.x - radius, node.y - radius, radius * 2, radius * 2);
            gc.setStroke(Color.DARKBLUE);
            gc.strokeOval(node.x - radius, node.y - radius, radius * 2, radius * 2);

            // Draw node label
            gc.setFill(Color.BLACK);
            gc.fillText(node.label, node.x - 10, node.y + radius + 5);
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
            gc.strokeOval(selectedNode.x - radius - 2,
                    selectedNode.y - radius - 2,
                    radius * 2 + 4,
                    radius * 2 + 4);
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

    private void drawDistanceLabel(Node node1, Node node2, Color color, boolean isCurved, ControlPoint control) {
        double distance;
        if (isCurved && control != null) {
            // For curved edges with control points
            distance = MathUtils.calculateCurveLength(node1, control, node2, scaleRatio);
        } else {
            // For straight edges
            distance = MathUtils.calculateDistance(node1, node2, scaleRatio);
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
            if (dx * dx + dy * dy <= radius * radius) {
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
                    nodeObj.put("label", node.label);
                    nodeObj.put("isSpecial", node.isSpecial);
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
                            nodeObj.getString("label"),
                            nodeObj.getBoolean("isSpecial")
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
                    
                    updateLists();
                    redrawCanvas();
                }
            } catch (NumberFormatException e) {
                // Invalid input
            }
        });
    }

    private void handleKeyPress(KeyEvent event, Stage primaryStage) {
        // System.out.println(event.getCode());
        switch (event.getCode()) {
            case A:
                // Add Node mode
                isAddNode = true;
                creatingEdge = false;
                curvedEdgeMode = false;
                selectedNode = null;
                controlPoint = null;
                break;

            case S:
                // Straight Edge mode
                creatingEdge = true;
                curvedEdgeMode = false;
                selectedNode = null;
                controlPoint = null;
                break;

            case C:
                // Curved Edge mode
                creatingEdge = true;
                curvedEdgeMode = true;
                selectedNode = null;
                controlPoint = null;
                break;

            case BACK_SPACE:
            case DELETE:
                // Delete selected
                deleteSelected();
                updateLists();
                redrawCanvas();
                break;

            case R:
                // Rename node
                renameSelectedNode(primaryStage);
                selectedNode = null;
                controlPoint = null;
                creatingEdge = false;
                break;

            case PLUS:
            case EQUALS:
                // Zoom in
                zoom(1.1);
                break;

            case MINUS:
            case SUBTRACT:
                // Zoom out
                zoom(1 / 1.1);
                break;

            case D:
                // Toggle distance display
                showDistances = !showDistances;
                redrawCanvas();
                break;

            case N:
                // Select next node in list
                if (!nodes.isEmpty()) {
                    int currentIndex = nodeListView.getSelectionModel().getSelectedIndex();
                    int newIndex = (currentIndex + 1) % nodes.size();
                    nodeListView.getSelectionModel().select(newIndex);
                }
                break;

            case P:
                // Select previous node in list
                if (!nodes.isEmpty()) {
                    int currentIndex = nodeListView.getSelectionModel().getSelectedIndex();
                    int newIndex = (currentIndex - 1 + nodes.size()) % nodes.size();
                    nodeListView.getSelectionModel().select(newIndex);
                }
                break;

            case E:
                // Select next edge in list
                if (!edges.isEmpty()) {
                    int currentIndex = edgeListView.getSelectionModel().getSelectedIndex();
                    int newIndex = (currentIndex + 1) % edges.size();
                    edgeListView.getSelectionModel().select(newIndex);
                }
                break;

            case W:
                // Select previous edge in list
                if (!edges.isEmpty()) {
                    int currentIndex = edgeListView.getSelectionModel().getSelectedIndex();
                    int newIndex = (currentIndex - 1 + edges.size()) % edges.size();
                    edgeListView.getSelectionModel().select(newIndex);
                }
                break;

            case ESCAPE:
                // Cancel current operation
                solutionPath.clear();
                selectedNode = null;
                controlPoint = null;
                creatingEdge = false;
                redrawCanvas();
                // clearFN();
                break;

            case SLASH:
                logChecker();
                break;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
