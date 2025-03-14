package com.example.ooad_project;

import com.example.ooad_project.API.GardenSimulationAPI;
import com.example.ooad_project.Events.*;
import com.example.ooad_project.Parasite.ParasiteManager;
import com.example.ooad_project.Plant.Children.Flower;
import com.example.ooad_project.Plant.Plant;
import com.example.ooad_project.Plant.Children.Tree;
import com.example.ooad_project.Plant.Children.Vegetable;
import com.example.ooad_project.Plant.PlantManager;
import com.example.ooad_project.ThreadUtils.EventBus;
import javafx.animation.AnimationTimer;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.animation.PauseTransition;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class GardenUIController {

    @FXML
    private Button sidButton;

    @FXML
    private Label currentDay;

//    @FXML
//    private MenuButton parasiteMenuButton;

//    @FXML
//    private Button pestTestButton;

    @FXML
    private Label getPLantButton;

    @FXML
    private Label rainStatusLabel;
    @FXML
    private Label temperatureStatusLabel;
    @FXML
    private Label parasiteStatusLabel;

    @FXML
    private GridPane gridPane;
    @FXML
    private MenuButton vegetableMenuButton;

    @FXML
    private MenuButton flowerMenuButton;
    @FXML
    private MenuButton treeMenuButton;

    @FXML
    private AnchorPane anchorPane;

    @FXML
    private StackPane weatherIconContainer;
    @FXML
    private StackPane temperatureIconContainer;
    @FXML
    private StackPane parasiteIconContainer;

    int flag = 0;
    int logDay = 0;
    DayChangeEvent dayChangeEvent;
    private static class RainDrop {
        double x, y, speed;

        public RainDrop(double x, double y, double speed) {
            this.x = x;
            this.y = y;
            this.speed = speed;
        }
    }

    // Create Canvas for the rain animation
    private Canvas rainCanvas;
    private List<RainDrop> rainDrops;
    private AnimationTimer rainAnimation;



    private final Random random = new Random();
    private GardenGrid gardenGrid;

    //    This is the plant manager that will be used to get the plant data
//    from the JSON file, used to populate the menu buttons
    private PlantManager plantManager = PlantManager.getInstance();

    //    Same as above but for the parasites
    private ParasiteManager parasiteManager = ParasiteManager.getInstance();

    public GardenUIController() {
        gardenGrid = GardenGrid.getInstance();
    }

    //    This is the method that will print the grid
    @FXML
    public void printGrid(){
        gardenGrid.printGrid();
    }

    @FXML
    public void sidButtonPressed() {
        System.out.println("SID Button Pressed");
        plantManager.getVegetables().forEach(flower -> System.out.println(flower.getCurrentImage()));
    }

//    @FXML
//    private TextArea logTextArea;

    private static final Logger logger = LogManager.getLogger("GardenUIControllerLogger");


    @FXML
    public void getPLantButtonPressed() {
        GardenSimulationAPI api = new GardenSimulationAPI();
//        api.getPlants();
        api.getState();
    }


    //    This is the UI Logger for the GardenUIController
//    This is used to log events that happen in the UI
    private Logger log4jLogger = LogManager.getLogger("GardenUIControllerLogger");

    @FXML
    public void initialize() {

        showSunnyWeather();

        showOptimalTemperature();

        showNoParasites();

        // Initialize icon containers with default icons
        initializeIconContainers();

        // Load the background image
        Image backgroundImage = new Image(getClass().getResourceAsStream("/images/backgroundImage1.png"));

        // Create an ImageView
        ImageView imageView = new ImageView(backgroundImage);
        imageView.setPreserveRatio(false);

        // Add the ImageView as the first child of the AnchorPane
        anchorPane.getChildren().add(0, imageView);

        // Bind ImageView's size to the AnchorPane's size
        anchorPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            imageView.setFitWidth(newVal.doubleValue());
        });
        anchorPane.heightProperty().addListener((obs, oldVal, newVal) -> {
            imageView.setFitHeight(newVal.doubleValue());
        });

        // Add ColumnConstraints
        for (int col = 0; col < gardenGrid.getNumCols(); col++) {
            ColumnConstraints colConst = new ColumnConstraints();
            colConst.setPrefWidth(83); // Adjust the width as needed
            gridPane.getColumnConstraints().add(colConst);
        }

        // Add RowConstraints
        for (int row = 0; row < gardenGrid.getNumRows(); row++) {
            RowConstraints rowConst = new RowConstraints();
            rowConst.setPrefHeight(93); // Adjust the height as needed
            gridPane.getRowConstraints().add(rowConst);
        }

        createColoredGrid(gridPane, gardenGrid.getNumRows(),gardenGrid.getNumCols());

        // Initialize the rain canvas and animation - position it over the garden grid only
        rainCanvas = new Canvas(800, 600); // Initial size - will be adjusted dynamically
        rainCanvas.setTranslateX(260); // Match the garden grid's left anchor
        rainCanvas.setTranslateY(20);  // Approximate top position of the grid
        anchorPane.getChildren().add(rainCanvas); // Add the canvas to the AnchorPane
        
        // Make the canvas initially invisible
        rainCanvas.setVisible(false);
        
        rainDrops = new ArrayList<>();

        // Load plants data from JSON file and populate MenuButtons
        vegetableMenuButton.setText("Vegetable"); // Updated Name
        treeMenuButton.setText("Trees");
        flowerMenuButton.setText("Flowers");
        
        // Apply custom styling to menu buttons with gradient backgrounds
        final String baseTreeButtonStyle = "-fx-background-radius: 15px; " +
                                "-fx-border-radius: 15px; " +
                                "-fx-background-color: linear-gradient(to bottom, #8BC34A, #689F38); " +
                                "-fx-border-color: #4CAF50; " +
                                "-fx-border-width: 2px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-font-size: 16px; " +
                                "-fx-text-fill: white; " +
                                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 8, 0, 0, 3); " +
                                "-fx-content-display: CENTER; " +
                                "-fx-cursor: hand;";
                                
        final String baseFlowerButtonStyle = "-fx-background-radius: 15px; " +
                                "-fx-border-radius: 15px; " +
                                "-fx-background-color: linear-gradient(to bottom, #FF9800, #F57C00); " +
                                "-fx-border-color: #FF5722; " +
                                "-fx-border-width: 2px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-font-size: 16px; " +
                                "-fx-text-fill: white; " +
                                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 8, 0, 0, 3); " +
                                "-fx-content-display: CENTER; " +
                                "-fx-cursor: hand;";
                                
        final String baseVegetableButtonStyle = "-fx-background-radius: 15px; " +
                                "-fx-border-radius: 15px; " +
                                "-fx-background-color: linear-gradient(to bottom, #4CAF50, #388E3C); " +
                                "-fx-border-color: #2E7D32; " +
                                "-fx-border-width: 2px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-font-size: 16px; " +
                                "-fx-text-fill: white; " +
                                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 8, 0, 0, 3); " +
                                "-fx-content-display: CENTER; " +
                                "-fx-cursor: hand;";
        
        // Make the entire button area clickable by adding a click handler to the button
        vegetableMenuButton.setOnMouseClicked(event -> vegetableMenuButton.show());
        treeMenuButton.setOnMouseClicked(event -> treeMenuButton.show());
        flowerMenuButton.setOnMouseClicked(event -> flowerMenuButton.show());
        
        // Add hover effects to the buttons with glow effect
        vegetableMenuButton.setOnMouseEntered(e -> vegetableMenuButton.setStyle(baseVegetableButtonStyle + "-fx-background-color: linear-gradient(to bottom, #66BB6A, #43A047); -fx-effect: dropshadow(gaussian, rgba(0,255,0,0.3), 15, 0, 0, 0);"));
        vegetableMenuButton.setOnMouseExited(e -> vegetableMenuButton.setStyle(baseVegetableButtonStyle));
        
        treeMenuButton.setOnMouseEntered(e -> treeMenuButton.setStyle(baseTreeButtonStyle + "-fx-background-color: linear-gradient(to bottom, #9CCC65, #7CB342); -fx-effect: dropshadow(gaussian, rgba(0,255,0,0.3), 15, 0, 0, 0);"));
        treeMenuButton.setOnMouseExited(e -> treeMenuButton.setStyle(baseTreeButtonStyle));
        
        flowerMenuButton.setOnMouseEntered(e -> flowerMenuButton.setStyle(baseFlowerButtonStyle + "-fx-background-color: linear-gradient(to bottom, #FFA726, #FB8C00); -fx-effect: dropshadow(gaussian, rgba(255,153,0,0.3), 15, 0, 0, 0);"));
        flowerMenuButton.setOnMouseExited(e -> flowerMenuButton.setStyle(baseFlowerButtonStyle));
        
        vegetableMenuButton.setStyle(baseVegetableButtonStyle);
        treeMenuButton.setStyle(baseTreeButtonStyle);
        flowerMenuButton.setStyle(baseFlowerButtonStyle);
        
        // Set minimum width for consistent button sizes
        vegetableMenuButton.setMinWidth(220);
        treeMenuButton.setMinWidth(220);
        flowerMenuButton.setMinWidth(220);
        
        loadPlantsData();

        log4jLogger.info("🎮 GardenUIController initialized");



        EventBus.subscribe("RainEvent", event -> changeRainUI((RainEvent) event));
        EventBus.subscribe("DisplayParasiteEvent", event -> handleDisplayParasiteEvent((DisplayParasiteEvent) event));
        EventBus.subscribe("PlantImageUpdateEvent", event -> handlePlantImageUpdateEvent((PlantImageUpdateEvent) event));
        EventBus.subscribe("DayChangeEvent",event -> handleDayChangeEvent((DayChangeEvent) event));
        EventBus.subscribe("TemperatureEvent", event -> changeTemperatureUI((TemperatureEvent) event));
        EventBus.subscribe("ParasiteEvent", event -> changeParasiteUI((ParasiteEvent) event));

//      Gives you row, col and waterneeded
        EventBus.subscribe("SprinklerEvent", event -> handleSprinklerEvent((SprinklerEvent) event));


//        When plant is cooled by x
        EventBus.subscribe("TemperatureCoolEvent", event -> handleTemperatureCoolEvent((TemperatureCoolEvent) event));


//      When plant is heated by x
        EventBus.subscribe("TemperatureHeatEvent", event -> handleTemperatureHeatEvent((TemperatureHeatEvent) event));


//        When plant is damaged by x
//        Includes -> row, col, damage
        EventBus.subscribe("ParasiteDamageEvent", event -> handleParasiteDamageEvent((ParasiteDamageEvent) event));

        EventBus.subscribe("InitializeGarden", event -> handleInitializeGarden());

//        Event whenever there is change to plants health
        EventBus.subscribe("PlantHealthUpdateEvent", event -> handlePlantHealthUpdateEvent((PlantHealthUpdateEvent) event));

        EventBus.subscribe("PlantDeathUIChangeEvent", event -> handlePlantDeathUIChangeEvent((Plant) event));

    }

    // Start rain animation
    private void startRainAnimation() {
        // Make the canvas visible when animation starts
        rainCanvas.setVisible(true);
        
        // Calculate the grid's actual dimensions
        double gridWidth = gridPane.getWidth();
        double gridHeight = gridPane.getHeight();
        
        // If grid dimensions are not yet available, use the number of rows/columns and cell size
        if (gridWidth <= 0 || gridHeight <= 0) {
            gridWidth = gardenGrid.getNumCols() * 83; // Using column constraint width
            gridHeight = gardenGrid.getNumRows() * 93; // Using row constraint height
        }
        
        // Ensure minimum dimensions and add some extra height to reach the bottom
        gridWidth = Math.max(gridWidth, 500);
        gridHeight = Math.max(gridHeight, 550);
        
        // Set canvas size to match the grid area with full height
        rainCanvas.setWidth(gridWidth);
        rainCanvas.setHeight(gridHeight);
        
        GraphicsContext gc = rainCanvas.getGraphicsContext2D();

        // Create initial raindrops
        rainDrops.clear(); // Clear any existing raindrops
        for (int i = 0; i < 150; i++) { // Increased number of raindrops for better coverage
            rainDrops.add(new RainDrop(
                random.nextDouble() * rainCanvas.getWidth(), 
                random.nextDouble() * rainCanvas.getHeight(), 
                2 + random.nextDouble() * 4
            ));
        }

        // Animation timer to update and draw raindrops
        if (rainAnimation != null) {
            rainAnimation.stop();
        }
        
        rainAnimation = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateRainDrops();
                drawRain(gc);
            }
        };

        rainAnimation.start();
    }

    // Update raindrop positions
    private void updateRainDrops() {
        for (RainDrop drop : rainDrops) {
            drop.y += drop.speed;
            if (drop.y > rainCanvas.getHeight()) {
                // Reset raindrop to top with random horizontal position
                drop.y = -10 - random.nextDouble() * 20; // Start slightly above the canvas with variation
                drop.x = random.nextDouble() * rainCanvas.getWidth();
            }
        }
    }

    // Draw raindrops on the canvas
    private void drawRain(GraphicsContext gc) {
        gc.clearRect(0, 0, rainCanvas.getWidth(), rainCanvas.getHeight());
        gc.setFill(Color.CYAN);

        for (RainDrop drop : rainDrops) {
            // Draw slightly larger raindrops for better visibility
            gc.fillOval(drop.x, drop.y, 2, 15); // Raindrop shape (x, y, width, height)
        }
    }

    // Stop rain animation
    private void stopRainAfterFiveSeconds() {
        PauseTransition pauseRain = new PauseTransition(Duration.seconds(1));
        pauseRain.setOnFinished(event -> {
            // Clear the canvas and stop the animation
            if (rainAnimation != null) {
                rainAnimation.stop();
            }
            if (rainCanvas != null) {
                rainCanvas.getGraphicsContext2D().clearRect(0, 0, rainCanvas.getWidth(), rainCanvas.getHeight());
                rainCanvas.setVisible(false); // Hide the canvas when not in use
            }
        });
        pauseRain.play();
    }



    public void createColoredGrid(GridPane gridPane, int numRows, int numCols) {
        double cellWidth = 80;  // Width of each cell
        double cellHeight = 80; // Height of each cell

        // Loop through rows and columns to create cells
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                // Create a StackPane for each cell
                StackPane cell = new StackPane();

                // Set preferred size of the cell
                cell.setPrefSize(cellWidth, cellHeight);

                // Set a unique border color for each cell
                Color borderColor = Color.TRANSPARENT; // Function to generate random colors
                cell.setBorder(new Border(new BorderStroke(
                        borderColor,
                        BorderStrokeStyle.SOLID,
                        CornerRadii.EMPTY,
                        new BorderWidths(2) // Border thickness
                )));

                // Add the cell to the GridPane
                gridPane.add(cell, col, row);
            }
        }
    }


    private void handlePlantDeathUIChangeEvent(Plant plant){

    }

    private void handlePlantHealthUpdateEvent(PlantHealthUpdateEvent event){
        logger.info("🌱 Day: " + logDay + " Plant health updated at row " + event.getRow() + " and column " + event.getCol() + " from " + event.getOldHealth() + " to " + event.getNewHealth());
    }

    private void handleInitializeGarden() {
        // Hard-coded positions for plants as specified in the layout
        Object[][] gardenLayout = {
                {"Oak", 0, 1}, {"Maple", 0, 5}, {"Pine", 0, 6},
                {"Tomato", 1, 6}, {"Carrot", 2, 2}, {"Lettuce", 1, 0},
                {"Sunflower", 3, 1}, {"Rose", 4, 4}, {"Jasmine", 4, 6},
                {"Oak", 5, 6}, {"Tomato", 3, 0}, {"Sunflower", 5, 3}
        };

        Platform.runLater(() -> {
            for (Object[] plantInfo : gardenLayout) {
                String plantType = (String) plantInfo[0];
                int row = (Integer) plantInfo[1];
                int col = (Integer) plantInfo[2];

                Plant plant = plantManager.getPlantByName(plantType);
                if (plant != null) {
                    plant.setRow(row);
                    plant.setCol(col);
                    try {
                        gardenGrid.addPlant(plant, row, col);  // Add plant to the logical grid
                        addPlantToGridUI(plant, row, col);    // Add plant to the UI
                    } catch (Exception e) {
                        logger.error("❌ Failed to place plant: " + plant.getName() + " at (" + row + ", " + col + "): " + e.getMessage());
                    }
                }
            }
        });
    }

    private void addPlantToGridUI(Plant plant, int row, int col) {

        logger.info("🌿 Day: " + logDay + " Adding plant to grid: " + plant.getName() + " at row " + row + " and column " + col);

        String imageFile = plant.getCurrentImage();
        Image image = new Image(getClass().getResourceAsStream("/images/" + imageFile));
        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(65); // Fit the cell size
        imageView.setFitWidth(65);

        StackPane pane = new StackPane(imageView);
        pane.setStyle("-fx-alignment: center;"); // Center the image in the pane
        gridPane.add(pane, col, row); // Add the pane to the grid
        GridPane.setHalignment(pane, HPos.LEFT); // Center align in grid cell
        GridPane.setValignment(pane, VPos.TOP);
    }

    //    Function that is called when the parasite damage event is published
    private void handleParasiteDamageEvent(ParasiteDamageEvent event) {
        logger.info("💥 Day: " + logDay + " Displayed plant damaged at row " + event.getRow() + " and column " + event.getCol() + " by " + event.getDamage());

        Platform.runLater(() -> {
            int row = event.getRow();
            int col = event.getCol();
            int damage = event.getDamage();

            // Create a label with the damage value prefixed by a minus sign
            Label damageLabel = new Label("-" + damage);
            damageLabel.setTextFill(Color.RED);
            damageLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            // Create a white circular background
            Circle backgroundCircle = new Circle(15, Color.WHITE);
            backgroundCircle.setStroke(Color.BLACK);

            // Wrap label and background in a StackPane for alignment
            StackPane damagePane = new StackPane(backgroundCircle, damageLabel);
            damagePane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

            // Add damagePane to the grid
            gridPane.add(damagePane, col, row);
            GridPane.setHalignment(damagePane, HPos.RIGHT);  // Align to lower right corner
            GridPane.setValignment(damagePane, VPos.BOTTOM);

            // Remove the label after a pause
            PauseTransition pause = new PauseTransition(Duration.seconds(5)); // Set duration to 5 seconds
            pause.setOnFinished(_ -> gridPane.getChildren().remove(damagePane));
            pause.play();
        });
    }
    private void handleTemperatureHeatEvent(TemperatureHeatEvent event) {

        logger.info("🔥 Day: " + logDay + " Displayed plant heated at row " + event.getRow() + " and column " + event.getCol() + " by " + event.getTempDiff());

        Platform.runLater(() -> {
            int row = event.getRow();
            int col = event.getCol();

            String imageName = "heated.png"; // Update this to your heat image name
            Image heatImage = new Image(getClass().getResourceAsStream("/images/" + imageName));
            ImageView heatImageView = new ImageView(heatImage);
            heatImageView.setFitHeight(40);  // Match the cell size in the grid
            heatImageView.setFitWidth(40);

            GridPane.setRowIndex(heatImageView, row);
            GridPane.setColumnIndex(heatImageView, col);
            GridPane.setHalignment(heatImageView, HPos.LEFT);  // Align to left
            GridPane.setValignment(heatImageView, VPos.TOP); // Align to top
            gridPane.getChildren().add(heatImageView);

            PauseTransition pause = new PauseTransition(Duration.seconds(5)); // Set duration to 10 seconds
            pause.setOnFinished(_ -> gridPane.getChildren().remove(heatImageView));
            pause.play();
        });
    }


//    Function that is called when the temperature cool event is published

    private void handleTemperatureCoolEvent(TemperatureCoolEvent event) {


        logger.info("❄️ Day: " + currentDay + " Displayed plant cooled at row " + event.getRow() + " and column " + event.getCol() + " by " + event.getTempDiff());

        Platform.runLater(() -> {
            int row = event.getRow();
            int col = event.getCol();

            String imageName = "cooled.png"; // Update this to your cool image name
            Image coolImage = new Image(getClass().getResourceAsStream("/images/" + imageName));
            ImageView coolImageView = new ImageView(coolImage);
            coolImageView.setFitHeight(20);  // Match the cell size in the grid
            coolImageView.setFitWidth(20);

            GridPane.setRowIndex(coolImageView, row);
            GridPane.setColumnIndex(coolImageView, col);
            GridPane.setHalignment(coolImageView, HPos.LEFT);  // Align to left
            GridPane.setValignment(coolImageView, VPos.TOP); // Align to top
            gridPane.getChildren().add(coolImageView);

            PauseTransition pause = new PauseTransition(Duration.seconds(5)); // Set duration to 10 seconds
            pause.setOnFinished(_ -> gridPane.getChildren().remove(coolImageView));
            pause.play();
        });
    }
    //  Function that is called when the sprinkler event is published
    private void handleSprinklerEvent(SprinklerEvent event) {

        logger.info("💧 Day: " + currentDay + " Displayed Sprinkler activated at row " + event.getRow() + " and column " + event.getCol() + " with water amount " + event.getWaterNeeded());

        Platform.runLater(() -> {
            int row = event.getRow();
            int col = event.getCol();

            // Create a group to hold animated droplets
            Group sprinklerAnimationGroup = new Group();

            // Add multiple lines or droplets to simulate water spray
            int numDroplets = 10; // Number of water droplets
            double tileWidth = 40; // Width of the grid cell
            double tileHeight = 70; // Height of the grid cell

            for (int i = 0; i < numDroplets; i++) {
                // Calculate evenly spaced positions within the tile
                double positionX = (i % Math.sqrt(numDroplets)) * (tileWidth / Math.sqrt(numDroplets));
                double positionY = (i / Math.sqrt(numDroplets)) * (tileHeight / Math.sqrt(numDroplets));

                Circle droplet = new Circle();
                droplet.setRadius(3); // Radius of the droplet
                droplet.setFill(Color.BLUE); // Color of the droplet

                // Set starting position for the droplet
                droplet.setCenterX(positionX);
                droplet.setCenterY(positionY);

                // Create a transition for each droplet
                TranslateTransition transition = new TranslateTransition();
                transition.setNode(droplet);
                transition.setDuration(Duration.seconds(0.9)); // Droplet animation duration
                transition.setByX(Math.random() * 20 - 2.5); // Small random spread on X-axis
                transition.setByY(Math.random() * 20);      // Small downward spread on Y-axis
                transition.setCycleCount(1);
                // Add to group and start animation
                sprinklerAnimationGroup.getChildren().add(droplet);
                transition.play();
            }

            // Add animation group to the grid cell
            GridPane.setRowIndex(sprinklerAnimationGroup, row);
            GridPane.setColumnIndex(sprinklerAnimationGroup, col);
            gridPane.getChildren().add(sprinklerAnimationGroup);

            // Remove animation after it completes
            PauseTransition pause = new PauseTransition(Duration.seconds(3)); // Total duration for animation to persist
            pause.setOnFinished(_ -> gridPane.getChildren().remove(sprinklerAnimationGroup));
            pause.play();

            String imageNameCool = "cooled.png"; // Update this to your cool image name
            Image coolImage = new Image(getClass().getResourceAsStream("/images/" + imageNameCool));
            ImageView coolImageView = new ImageView(coolImage);
            coolImageView.setFitHeight(20);  // Match the cell size in the grid
            coolImageView.setFitWidth(20);

            GridPane.setRowIndex(coolImageView, row);
            GridPane.setColumnIndex(coolImageView, col);
            GridPane.setHalignment(coolImageView, HPos.LEFT);  // Align to left
            GridPane.setValignment(coolImageView, VPos.BOTTOM); // Align to top
            gridPane.getChildren().add(coolImageView);

            PauseTransition pauseCool = new PauseTransition(Duration.seconds(5)); // Set duration to 10 seconds
            pauseCool.setOnFinished(_ -> gridPane.getChildren().remove(coolImageView));
            pauseCool.play();
        });


    }

    private void initializeIconContainers() {
        // Initialize weather icon container
        Image sunImage = new Image(getClass().getResourceAsStream("/images/sun.png"));
        ImageView sunImageView = new ImageView(sunImage);
        sunImageView.setFitHeight(50);
        sunImageView.setFitWidth(50);
        weatherIconContainer.getChildren().clear();
        weatherIconContainer.getChildren().add(sunImageView);
        
        // Initialize temperature icon container
        Image tempImage = new Image(getClass().getResourceAsStream("/images/Temperature/normalTemperature.png"));
        ImageView tempImageView = new ImageView(tempImage);
        tempImageView.setFitHeight(90);
        tempImageView.setFitWidth(45);
        temperatureIconContainer.getChildren().clear();
        temperatureIconContainer.getChildren().add(tempImageView);
        
        // Initialize parasite icon container
        Image parasiteImage = new Image(getClass().getResourceAsStream("/images/Parasites/noParasite.png"));
        ImageView parasiteImageView = new ImageView(parasiteImage);
        parasiteImageView.setFitHeight(50);
        parasiteImageView.setFitWidth(50);
        parasiteIconContainer.getChildren().clear();
        parasiteIconContainer.getChildren().add(parasiteImageView);
    }

    public void handleDayChangeEvent(DayChangeEvent event) {

        logger.info("📅 Day: " + logDay + " Day changed to: " + event.getDay());
        dayChangeEvent = event;
        Platform.runLater(() -> {
            logDay = event.getDay();
            currentDay.setText(String.valueOf(event.getDay()));
            
            // Add a visual effect for day change
            currentDay.setStyle("-fx-text-fill: #ffd700; -fx-font-size: 24px; -fx-font-weight: bold;");
            
            // Create a scale transition for the day number
            javafx.animation.ScaleTransition scaleTransition = new javafx.animation.ScaleTransition(Duration.millis(500), currentDay);
            scaleTransition.setFromX(1.0);
            scaleTransition.setFromY(1.0);
            scaleTransition.setToX(1.5);
            scaleTransition.setToY(1.5);
            scaleTransition.setCycleCount(2);
            scaleTransition.setAutoReverse(true);
            scaleTransition.play();
        });
    }

    private void handlePlantImageUpdateEvent(PlantImageUpdateEvent event) {
        logger.info("🖼️ Day: " + logDay + " Plant image updated at row " + event.getPlant().getRow() + " and column " + event.getPlant().getCol() + " to " + event.getPlant().getCurrentImage());

        Platform.runLater(() -> {
            Plant plant = event.getPlant();
            int row = plant.getRow();
            int col = plant.getCol();

            // Remove previous image from the grid
            gridPane.getChildren().removeIf(node -> {
                Integer nodeRow = GridPane.getRowIndex(node);
                Integer nodeCol = GridPane.getColumnIndex(node);
                return nodeRow != null && nodeCol != null && nodeRow == row && nodeCol == col && node instanceof StackPane;
            });

            // Load the new image for the plant
            String imageName = plant.getCurrentImage();
            Image newImage = new Image(getClass().getResourceAsStream("/images/" + imageName));
            ImageView newImageView = new ImageView(newImage);
            newImageView.setFitHeight(40);  // Match the cell size in the grid
            newImageView.setFitWidth(40);

            // Create a pane to center the image
            StackPane pane = new StackPane(newImageView);
            gridPane.add(pane, col, row);
            GridPane.setColumnIndex(pane, col);
            GridPane.setRowIndex(pane, row);
        });
    }

    private void handleDisplayParasiteEvent(DisplayParasiteEvent event) {
        logger.info("🐛 Day: " + logDay + " Parasite displayed at row " + event.getRow() +
                " and column " + event.getColumn() + " with name " + event.getParasite().getName());

        int row = event.getRow();
        int col = event.getColumn();

        // Load and configure the parasite image
        Image parasiteImage = new Image(getClass().getResourceAsStream("/images/" + event.getParasite().getImageName()));
        ImageView parasiteImageView = new ImageView(parasiteImage);
        parasiteImageView.setFitHeight(50);
        parasiteImageView.setFitWidth(50);

        // Create a StackPane to hold the parasite image
        StackPane parasitePane = new StackPane(parasiteImageView);
        gridPane.add(parasitePane, col, row);
        GridPane.setHalignment(parasitePane, HPos.LEFT);
        GridPane.setValignment(parasitePane, VPos.BOTTOM);

        // Remove parasite after 3 seconds with pest control effect
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(_ -> {
            triggerPestControlEffect(row, col);
            gridPane.getChildren().remove(parasitePane);
        });
        pause.play();
    }

    private void triggerPestControlEffect(int row, int col) {
        Image pestControlImage = new Image(getClass().getResourceAsStream("/images/pControl.png"));
        ImageView pestControlImageView = new ImageView(pestControlImage);
        pestControlImageView.setFitHeight(70);
        pestControlImageView.setFitWidth(70);

        StackPane pestControlPane = new StackPane(pestControlImageView);
        gridPane.add(pestControlPane, col, row);
        GridPane.setHalignment(pestControlPane, HPos.LEFT);
        GridPane.setValignment(pestControlPane, VPos.BOTTOM);

        // Remove pest control effect after 2 seconds
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(_ -> gridPane.getChildren().remove(pestControlPane));
        pause.play();
    }



    private void changeRainUI(RainEvent event) {

        startRainAnimation();

        // Stop rain after 5 seconds
        //stopRainAfterFiveSeconds();

        logger.info("🌧️ Day: " + logDay + " Displayed rain event with amount: " + event.getAmount() + "mm");

        Platform.runLater(() -> {
            // Update UI to reflect it's raining

            // Create an ImageView for the rain icon
            Image rainImage = new Image(getClass().getResourceAsStream("/images/rain.png"));
            ImageView rainImageView = new ImageView(rainImage);
            rainImageView.setFitHeight(50);
            rainImageView.setFitWidth(50);

            // Set the icon in the container
            weatherIconContainer.getChildren().clear();
            weatherIconContainer.getChildren().add(rainImageView);
            
            // Set the text with the rain amount
            rainStatusLabel.setText(event.getAmount() + "mm");
            
            // Change the background color to indicate rain
            rainStatusLabel.getParent().getParent().setStyle("-fx-background-color: rgba(0,102,204,0.8); -fx-background-radius: 10px; -fx-padding: 10px;");

            // Create a pause transition of 5 seconds
            PauseTransition pause = new PauseTransition(Duration.seconds(5));
            pause.setOnFinished(e -> {
                // Update UI to reflect no rain after the event ends
                showSunnyWeather();
            });
            pause.play();
        });
    }

    private void showSunnyWeather() {
        if(flag == 1) {
            stopRainAfterFiveSeconds();
        }
        flag = 1;

        logger.info("☀️ Day: " + logDay + " Displayed sunny weather");

        Platform.runLater(() -> {
            // Create an ImageView for the sun icon
            Image sunImage = new Image(getClass().getResourceAsStream("/images/sun.png"));
            ImageView sunImageView = new ImageView(sunImage);
            sunImageView.setFitHeight(50);
            sunImageView.setFitWidth(50);

            // Set the icon in the container
            weatherIconContainer.getChildren().clear();
            weatherIconContainer.getChildren().add(sunImageView);
            
            // Set the text with the sun status
            rainStatusLabel.setText("Sunny");
            
            // Reset the background color
            rainStatusLabel.getParent().getParent().setStyle("-fx-background-color: rgba(0,102,204,0.6); -fx-background-radius: 10px; -fx-padding: 10px;");
        });
    }


    private void changeTemperatureUI(TemperatureEvent event) {

        logger.info("🌡️ Day: " + logDay + " Temperature changed to: " + event.getAmount() + "°F");

        Platform.runLater(() -> {
            // Update UI to reflect the temperature change

            // Create an ImageView for the temperature icon
            String image = "normalTemperature.png";
            int fitHeight = 90;
            int fitWidth = 45;
            String bgColor = "rgba(204,51,0,0.6)";
            
            if (event.getAmount() <= 50) {
                image = "coldTemperature.png";
                bgColor = "rgba(0,102,204,0.8)"; // Cooler blue for cold
            } else if(event.getAmount() >= 60) {
                image = "hotTemperature.png";
                bgColor = "rgba(255,51,0,0.8)"; // Hotter red for hot
            }
            
            Image tempImage = new Image(getClass().getResourceAsStream("/images/Temperature/" + image));
            ImageView tempImageView = new ImageView(tempImage);
            tempImageView.setFitHeight(fitHeight);
            tempImageView.setFitWidth(fitWidth);
            
            // Set the icon in the container
            temperatureIconContainer.getChildren().clear();
            temperatureIconContainer.getChildren().add(tempImageView);
            
            // Set the text with the temperature amount
            temperatureStatusLabel.setText(event.getAmount() + "°F");
            
            // Change the background color based on temperature
            temperatureStatusLabel.getParent().getParent().setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10px; -fx-padding: 15px; -fx-min-height: 120px;");

            // Create a pause transition of 5 seconds
            PauseTransition pause = new PauseTransition(Duration.seconds(5));
            pause.setOnFinished(e -> {
                // Update UI to reflect optimal temperature after the event ends
                showOptimalTemperature();
            });
            pause.play();
        });
    }

    private void showOptimalTemperature() {

        logger.info("✨ Day: " + logDay +" Displayed optimal temperature");

        Platform.runLater(() -> {
            // Create an ImageView for the optimal temperature icon
            Image optimalImage = new Image(getClass().getResourceAsStream("/images/Temperature/normalTemperature.png"));
            ImageView optimalImageView = new ImageView(optimalImage);
            optimalImageView.setFitHeight(90);
            optimalImageView.setFitWidth(45);
            
            // Set the icon in the container
            temperatureIconContainer.getChildren().clear();
            temperatureIconContainer.getChildren().add(optimalImageView);
            
            // Set the text with the optimal status
            temperatureStatusLabel.setText("Optimal");
            
            // Reset the background color
            temperatureStatusLabel.getParent().getParent().setStyle("-fx-background-color: rgba(204,51,0,0.6); -fx-background-radius: 10px; -fx-padding: 15px; -fx-min-height: 120px;");
        });
    }

    private void changeParasiteUI(ParasiteEvent event) {

        logger.info("🐛 Day: " + logDay + " Parasite event triggered: " + event.getParasite().getName());

        Platform.runLater(() -> {
            // Update UI to reflect parasite event
            
            // Create an ImageView for the parasite icon
            Image parasiteImage = new Image(getClass().getResourceAsStream("/images/Parasites/noParasite.png"));
            String bgColor = "rgba(153,51,51,0.7)"; // Less bright red background for parasite alert

            if (Objects.equals(event.getParasite().getName(), "Slugs")) {
                parasiteImage = new Image(getClass().getResourceAsStream("/images/Parasites/slugDetected.png"));
            } else if (Objects.equals(event.getParasite().getName(), "Crow")) {
                parasiteImage = new Image(getClass().getResourceAsStream("/images/Parasites/crowDetected.png"));
            } else if (Objects.equals(event.getParasite().getName(), "Locust")) {
                parasiteImage = new Image(getClass().getResourceAsStream("/images/Parasites/locustDetected.png"));
            } else if (Objects.equals(event.getParasite().getName(), "Aphids")) {
                parasiteImage = new Image(getClass().getResourceAsStream("/images/Parasites/aphidsDetected.png"));
            } else if (Objects.equals(event.getParasite().getName(), "Rat")) {
                parasiteImage = new Image(getClass().getResourceAsStream("/images/Parasites/ratDetected.png"));
            } else if (Objects.equals(event.getParasite().getName(), "Parasite")) {
                parasiteImage = new Image(getClass().getResourceAsStream("/images/Parasites/parasiteDetected.png"));
            }

            ImageView parasiteImageView = new ImageView(parasiteImage);
            parasiteImageView.setFitHeight(50);
            parasiteImageView.setFitWidth(50);
            
            // Set the icon in the container
            parasiteIconContainer.getChildren().clear();
            parasiteIconContainer.getChildren().add(parasiteImageView);
            
            // Set the text with the parasite name
            parasiteStatusLabel.setText(event.getParasite().getName() + " detected");
            
            // Change the background color to indicate parasite alert
            parasiteStatusLabel.getParent().getParent().setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10px; -fx-padding: 10px;");

            // Create a pause transition of 5 seconds
            PauseTransition pause = new PauseTransition(Duration.seconds(5));
            pause.setOnFinished(e -> {
                // Update UI to reflect no parasites after the event ends
                showNoParasites();
            });
            pause.play();
        });
    }

    private void showNoParasites() {

        logger.info("✅ Day: " + logDay + " Displayed no parasites status");

        Platform.runLater(() -> {
            // Create an ImageView for the happy icon
            Image happyImage = new Image(getClass().getResourceAsStream("/images/Parasites/noParasite.png"));
            ImageView happyImageView = new ImageView(happyImage);
            happyImageView.setFitHeight(50);
            happyImageView.setFitWidth(50);

            // Set the icon in the container
            parasiteIconContainer.getChildren().clear();
            parasiteIconContainer.getChildren().add(happyImageView);
            
            // Set the text with the no parasites status
            parasiteStatusLabel.setText("No Parasites");
            
            // Reset the background color
            parasiteStatusLabel.getParent().getParent().setStyle("-fx-background-color: rgba(102,153,0,0.6); -fx-background-radius: 10px; -fx-padding: 10px;");
        });
    }

    //    This is the method that will populate the menu buttons with the plant data
    private void loadPlantsData() {
        logger.info("📚 Day: " + currentDay + " Loading plant data from JSON file");

        // Apply CSS styling to ensure the popup menu matches the button width
        // This is done through the custom menu items which we've already set to 200px width

        for (Flower flower : plantManager.getFlowers()) {
            CustomMenuItem menuItem = createImageMenuItem(flower.getName(), flower.getCurrentImage());
            menuItem.setOnAction(e -> addPlantToGrid(flower.getName(), flower.getCurrentImage()));
            flowerMenuButton.getItems().add(menuItem);
        }

        logger.info("🌳 Day: " + currentDay + " Loading Tree");

        for (Tree tree : plantManager.getTrees()) {
            CustomMenuItem menuItem = createImageMenuItem(tree.getName(), tree.getCurrentImage());
            menuItem.setOnAction(e -> addPlantToGrid(tree.getName(), tree.getCurrentImage()));
            treeMenuButton.getItems().add(menuItem);
        }

        logger.info("🥬 Day: " + currentDay + " Loading Vegetable");

        for (Vegetable vegetable : plantManager.getVegetables()) {
            logger.info("1");
            CustomMenuItem menuItem = createImageMenuItem(vegetable.getName(), vegetable.getCurrentImage());
            logger.info("2");
            menuItem.setOnAction(e -> addPlantToGrid(vegetable.getName(), vegetable.getCurrentImage()));
            vegetableMenuButton.getItems().add(menuItem);
        }
    }

    private CustomMenuItem createImageMenuItem(String name, String imagePath) {
        // Create an HBox to hold the image and text
        HBox hBox = new HBox(15); // Increased spacing
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setPadding(new javafx.geometry.Insets(8, 12, 8, 12)); // Increased padding
        hBox.getStyleClass().add("menu-item-container");
        hBox.setPrefWidth(220); // Match the width of menu buttons

        // Load the image
        ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream("/images/" + imagePath)));
        imageView.setFitWidth(65); // Slightly larger width
        imageView.setFitHeight(45); // Slightly larger height
        imageView.setPreserveRatio(true);
        
        // Add a border and rounded corners to the image with glow effect
        StackPane imageContainer = new StackPane(imageView);
        imageContainer.getStyleClass().add("menu-item-image-container");
        imageContainer.setPadding(new javafx.geometry.Insets(4));

        // Create a label for the text with custom font
        Label label = new Label(name);
        label.getStyleClass().add("menu-item-label");
        
        // Use HBox.setHgrow to allow the label to fill available space
        HBox.setHgrow(label, javafx.scene.layout.Priority.ALWAYS);

        // Add the image container and text to the HBox
        hBox.getChildren().addAll(imageContainer, label);

        // Wrap the HBox in a CustomMenuItem
        CustomMenuItem customMenuItem = new CustomMenuItem(hBox);
        customMenuItem.setHideOnClick(true); // Automatically hide the dropdown when clicked
        
        // Make the entire menu item take the full width
        customMenuItem.setStyle("-fx-pref-width: 220px;");
        
        // Add hover effect with animation
        hBox.setOnMouseEntered(e -> {
            imageContainer.getStyleClass().add("menu-item-image-container-hover");
        });
        
        hBox.setOnMouseExited(e -> {
            imageContainer.getStyleClass().remove("menu-item-image-container-hover");
        });

        return customMenuItem;
    }

    private void addPlantToGrid(String name, String imageFile) {
        logger.info("🌺 Day: " + logDay + " Adding plant to grid: " + name + " with image: " + imageFile);

        Plant plant = plantManager.getPlantByName(name); // Assume this method retrieves the correct plant
        if (plant != null) {
            boolean placed = false;
            int attempts = 0;
            while (!placed && attempts < 100) { // Limit attempts to avoid potential infinite loop
                int row = random.nextInt(gardenGrid.getNumRows());
                int col = random.nextInt(gardenGrid.getNumCols());
                if (!gardenGrid.isSpotOccupied(row, col)) {

                    ImageView farmerView = new ImageView(new Image(getClass().getResourceAsStream("/images/farmer.png")));
                    farmerView.setFitHeight(80);
                    farmerView.setFitWidth(80);

                    // Create a pane to center the image
                    StackPane farmerPane = new StackPane();
                    farmerPane.getChildren().add(farmerView);
                    gridPane.add(farmerPane, col, row);

                    PauseTransition pause = new PauseTransition(Duration.seconds(3));

                    pause.setOnFinished(_ -> {
                        gridPane.getChildren().remove(farmerPane);  // Remove the rat image from the grid
                        //gridPane.getChildren().remove(pestControlImageView);
                    });
                    pause.play();

                    PauseTransition farmerPause = new PauseTransition(Duration.seconds(3));

                    farmerPause.setOnFinished(event -> {
                        // Code to execute after the 5-second pause
                        plant.setRow(row);
                        plant.setCol(col);
                        gardenGrid.addPlant(plant, row, col);
                        ImageView plantView = new ImageView(new Image(getClass().getResourceAsStream("/images/" + imageFile)));
                        plantView.setFitHeight(40);
                        plantView.setFitWidth(40);

                        // Create a pane to center the image
                        StackPane pane = new StackPane();
                        pane.getChildren().add(plantView);
                        gridPane.add(pane, col, row);

                        // Optionally update UI here
                        Platform.runLater(() -> {
                            // Update your UI components if necessary
                        });
                    });

                    // Start the pause
                    farmerPause.play();
                    placed = true;
                }
                attempts++;
            }
            if (!placed) {
                System.err.println("Failed to place the plant after 100 attempts, grid might be full.");
            }
        } else {
            System.err.println("Plant not found: " + name);
        }
    }

    private void initializeLogger() {
        // LoggerAppender.setController(this);
    }
}