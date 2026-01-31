package com.cortex.ui;

import com.cortex.database.VideoGameDAO;
import com.cortex.database.UserDAO;
import com.cortex.model.User;
import com.cortex.model.VideoGame;
import com.cortex.model.GameRating;
import com.cortex.service.AuthenticationService;
import com.cortex.service.IGDBService;
import com.cortex.service.RecommendationEngine;
import com.cortex.util.ConfigLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Main application view after login.
 */
public class MainView {
    private static final Logger logger = LoggerFactory.getLogger(MainView.class);
    private final Stage stage;
    private final AuthenticationService authService;
    private final User currentUser;
    private final VideoGameDAO gameDAO;
    private final UserDAO userDAO;
    private final RecommendationEngine recommendationEngine;
    private IGDBService igdbService;
    private final BorderPane view;
    private VBox contentArea;

    public MainView(Stage stage, AuthenticationService authService, User currentUser) {
        this.stage = stage;
        this.authService = authService;
        this.currentUser = currentUser;
        this.gameDAO = new VideoGameDAO();
        this.userDAO = new UserDAO();
        this.recommendationEngine = new RecommendationEngine();
        
        // Initialize IGDB service with credentials from config
        String clientId = ConfigLoader.getIgdbClientId();
        String clientSecret = ConfigLoader.getIgdbClientSecret();
        if (clientId != null && !clientId.isEmpty() && !clientId.equals("YOUR_CLIENT_ID_HERE")) {
            this.igdbService = new IGDBService(clientId, clientSecret);
        }
        
        // Train ML model with existing data
        trainMLModel();
        
        this.view = createView();
    }

    /**
     * Train the ML recommendation model with existing user data.
     */
    private void trainMLModel() {
        try {
            List<User> allUsers = userDAO.findAll();
            List<VideoGame> allGames = gameDAO.findAll();
            List<GameRating> allRatings = userDAO.getAllRatings();
            
            logger.info("Training ML model with {} users, {} games, {} ratings", 
                    allUsers.size(), allGames.size(), allRatings.size());
            
            recommendationEngine.trainMLModel(allUsers, allGames, allRatings);
            
            if (recommendationEngine.isUsingML()) {
                logger.info("ML Model trained successfully! Using ML-based recommendations.");
                logger.info(recommendationEngine.getMLStats());
            } else {
                logger.info("Not enough data for ML. Using rule-based recommendations.");
            }
        } catch (Exception e) {
            logger.error("Error training ML model, falling back to rule-based", e);
        }
    }

    private BorderPane createView() {
        BorderPane layout = new BorderPane();

        // Top: Header with user info and navigation
        layout.setTop(createHeader());

        // Left: Navigation menu
        layout.setLeft(createNavigationMenu());

        // Center: Content area
        contentArea = new VBox(20);
        contentArea.setPadding(new Insets(20));
        layout.setCenter(new ScrollPane(contentArea));

        // Load initial content (Browse Games)
        showBrowseGames();

        return layout;
    }

    private HBox createHeader() {
        HBox header = new HBox(20);
        header.setPadding(new Insets(15));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        Label titleLabel = new Label("SPEED WAGON Game Library");
        titleLabel.getStyleClass().add("header-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userLabel = new Label("Welcome, " + currentUser.getUsername());
        userLabel.getStyleClass().add("user-label");

        Button logoutButton = new Button("Logout");
        logoutButton.getStyleClass().add("secondary-button");
        logoutButton.setOnAction(e -> logout());

        header.getChildren().addAll(titleLabel, spacer, userLabel, logoutButton);
        return header;
    }

    private VBox createNavigationMenu() {
        VBox menu = new VBox(10);
        menu.setPadding(new Insets(20));
        menu.setPrefWidth(200);
        menu.getStyleClass().add("navigation-menu");

        Button browseButton = createNavButton("Browse Games", () -> showBrowseGames());
        Button recommendationsButton = createNavButton("Recommendations", () -> showRecommendations());
        Button libraryButton = createNavButton("My Library", () -> showLibrary());
        Button wishlistButton = createNavButton("Wishlist", () -> showWishlist());
        
        menu.getChildren().addAll(browseButton, recommendationsButton, libraryButton, wishlistButton);

        // Add admin menu if user is admin
        if (authService.isAdmin()) {
            Separator separator = new Separator();
            Label adminLabel = new Label("Admin");
            adminLabel.getStyleClass().add("menu-section-label");
            Button adminButton = createNavButton("Admin Panel", () -> showAdminPanel());
            menu.getChildren().addAll(separator, adminLabel, adminButton);
        }

        return menu;
    }

    private Button createNavButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> action.run());
        return button;
    }

    private void showBrowseGames() {
        contentArea.getChildren().clear();

        Label headerLabel = new Label("Browse Games");
        headerLabel.getStyleClass().add("content-header");

        // Search bar
        HBox searchBox = new HBox(10);
        TextField searchField = new TextField();
        searchField.setPromptText("Search games...");
        searchField.setPrefWidth(400);
        
        Button searchButton = new Button("Search");
        searchButton.setOnAction(e -> searchGames(searchField.getText()));
        
        searchBox.getChildren().addAll(searchField, searchButton);

        // Games grid
        FlowPane gamesGrid = new FlowPane(15, 15);
        gamesGrid.setPadding(new Insets(20, 0, 0, 0));

        // Load games from database
        List<VideoGame> games = gameDAO.findAll();
        
        if (games.isEmpty()) {
            Label emptyLabel = new Label("No games available. Click 'Load from IGDB' to fetch games.");
            gamesGrid.getChildren().add(emptyLabel);
        } else {
            for (VideoGame game : games) {
                gamesGrid.getChildren().add(createGameCard(game));
            }
        }

        Button loadIGDBButton = new Button("Load Games from IGDB");
        loadIGDBButton.setOnAction(e -> loadGamesFromIGDB());

        contentArea.getChildren().addAll(headerLabel, searchBox, loadIGDBButton, gamesGrid);
    }

    private void showRecommendations() {
        contentArea.getChildren().clear();

        Label headerLabel = new Label("Recommended for You");
        headerLabel.getStyleClass().add("content-header");

        // Show if using ML or rule-based
        String mlStatus = recommendationEngine.isUsingML() 
            ? "🤖 Powered by Machine Learning" 
            : "📋 Based on your preferences";
        Label descLabel = new Label(mlStatus + " - personalized for your gaming history");
        descLabel.getStyleClass().add("subtitle");

        FlowPane recommendationsGrid = new FlowPane(15, 15);
        recommendationsGrid.setPadding(new Insets(20, 0, 0, 0));

        // Get recommendations
        List<VideoGame> allGames = gameDAO.findAll();
        List<VideoGame> recommendations = recommendationEngine.getRecommendations(
            userDAO.findById(currentUser.getId()), allGames, 12
        );

        if (recommendations.isEmpty()) {
            Label emptyLabel = new Label("No recommendations yet. Start rating games to get personalized suggestions!");
            recommendationsGrid.getChildren().add(emptyLabel);
        } else {
            for (VideoGame game : recommendations) {
                recommendationsGrid.getChildren().add(createGameCard(game));
            }
        }

        contentArea.getChildren().addAll(headerLabel, descLabel, recommendationsGrid);
    }

    private void showLibrary() {
        contentArea.getChildren().clear();

        Label headerLabel = new Label("My Library");
        headerLabel.getStyleClass().add("content-header");

        FlowPane libraryGrid = new FlowPane(15, 15);
        libraryGrid.setPadding(new Insets(20, 0, 0, 0));

        User user = userDAO.findById(currentUser.getId());
        List<VideoGame> ownedGames = user.getOwnedGames();

        if (ownedGames.isEmpty()) {
            Label emptyLabel = new Label("Your library is empty. Start shopping for games!");
            libraryGrid.getChildren().add(emptyLabel);
        } else {
            for (VideoGame game : ownedGames) {
                libraryGrid.getChildren().add(createGameCard(game));
            }
        }

        contentArea.getChildren().addAll(headerLabel, libraryGrid);
    }

    private void showWishlist() {
        contentArea.getChildren().clear();

        Label headerLabel = new Label("My Wishlist");
        headerLabel.getStyleClass().add("content-header");

        FlowPane wishlistGrid = new FlowPane(15, 15);
        wishlistGrid.setPadding(new Insets(20, 0, 0, 0));

        User user = userDAO.findById(currentUser.getId());
        List<VideoGame> wishlist = user.getWishlist();

        if (wishlist.isEmpty()) {
            Label emptyLabel = new Label("Your wishlist is empty.");
            wishlistGrid.getChildren().add(emptyLabel);
        } else {
            for (VideoGame game : wishlist) {
                wishlistGrid.getChildren().add(createGameCard(game));
            }
        }

        contentArea.getChildren().addAll(headerLabel, wishlistGrid);
    }

    private void showAdminPanel() {
        contentArea.getChildren().clear();

        Label headerLabel = new Label("Admin Panel");
        headerLabel.getStyleClass().add("content-header");

        VBox adminControls = new VBox(15);
        adminControls.setPadding(new Insets(20));

        Button manageGamesButton = new Button("Manage Games");
        manageGamesButton.setOnAction(e -> showGameManagement());
        
        Button manageUsersButton = new Button("Manage Users");
        manageUsersButton.setOnAction(e -> showUserManagement());

        Button loadGamesButton = new Button("Load Games from IGDB");
        loadGamesButton.setOnAction(e -> loadGamesFromIGDB());

        adminControls.getChildren().addAll(manageGamesButton, manageUsersButton, loadGamesButton);

        contentArea.getChildren().addAll(headerLabel, adminControls);
    }

    private VBox createGameCard(VideoGame game) {
        VBox card = new VBox(10);
        card.setPrefWidth(200);
        card.setPrefHeight(300);
        card.getStyleClass().add("game-card");
        card.setPadding(new Insets(10));

        // Game cover (placeholder)
        Label coverLabel = new Label("🎮");
        coverLabel.setStyle("-fx-font-size: 48px;");
        coverLabel.setAlignment(Pos.CENTER);
        coverLabel.setPrefHeight(120);

        Label titleLabel = new Label(game.getTitle());
        titleLabel.getStyleClass().add("game-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(180);

        Label priceLabel = new Label(String.format("$%.2f", game.getPrice()));
        priceLabel.getStyleClass().add("game-price");

        Label ratingLabel = new Label(String.format("★ %.1f", game.getRating()));
        ratingLabel.getStyleClass().add("game-rating");

        Button viewButton = new Button("View Details");
        viewButton.setMaxWidth(Double.MAX_VALUE);
        viewButton.setOnAction(e -> showGameDetails(game));

        card.getChildren().addAll(coverLabel, titleLabel, priceLabel, ratingLabel, viewButton);
        return card;
    }

    private void showGameDetails(VideoGame game) {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle(game.getTitle());
        dialog.setHeaderText(null);

        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Title: " + game.getTitle()),
            new Label("Price: $" + String.format("%.2f", game.getPrice())),
            new Label("Rating: " + String.format("%.1f/5", game.getRating())),
            new Label("Developer: " + (game.getDeveloper() != null ? game.getDeveloper() : "N/A")),
            new Label("Tags: " + String.join(", ", game.getTags())),
            new Label("\nDescription:"),
            new Label(game.getDescription() != null ? game.getDescription() : "No description available")
        );

        dialog.getDialogPane().setContent(content);
        
        ButtonType buyButton = new ButtonType("Buy", ButtonBar.ButtonData.OK_DONE);
        ButtonType wishlistButton = new ButtonType("Add to Wishlist", ButtonBar.ButtonData.OTHER);
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        dialog.getButtonTypes().setAll(buyButton, wishlistButton, closeButton);
        
        dialog.showAndWait().ifPresent(response -> {
            if (response == buyButton) {
                purchaseGame(game);
            } else if (response == wishlistButton) {
                addToWishlist(game);
            }
        });
    }

    private void purchaseGame(VideoGame game) {
        userDAO.addOwnedGame(currentUser.getId(), game.getId());
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Purchase Successful");
        alert.setHeaderText(null);
        alert.setContentText("You have purchased " + game.getTitle() + "!");
        alert.showAndWait();
        
        // Update recommendations based on purchase
        recommendationEngine.updatePreferencesFromInteraction(
            currentUser, game, RecommendationEngine.InteractionType.PURCHASE
        );
    }

    private void addToWishlist(VideoGame game) {
        userDAO.addToWishlist(currentUser.getId(), game.getId());
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Added to Wishlist");
        alert.setHeaderText(null);
        alert.setContentText(game.getTitle() + " has been added to your wishlist!");
        alert.showAndWait();
    }

    private void searchGames(String query) {
        contentArea.getChildren().clear();

        Label headerLabel = new Label("Search Results for: " + query);
        headerLabel.getStyleClass().add("content-header");

        FlowPane resultsGrid = new FlowPane(15, 15);
        resultsGrid.setPadding(new Insets(20, 0, 0, 0));

        List<VideoGame> results = gameDAO.searchByTitle(query);

        if (results.isEmpty()) {
            Label emptyLabel = new Label("No games found.");
            resultsGrid.getChildren().add(emptyLabel);
        } else {
            for (VideoGame game : results) {
                resultsGrid.getChildren().add(createGameCard(game));
            }
        }

        Button backButton = new Button("Back to Browse");
        backButton.setOnAction(e -> showBrowseGames());

        contentArea.getChildren().addAll(headerLabel, backButton, resultsGrid);
    }

    private void loadGamesFromIGDB() {
        if (igdbService == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("IGDB Not Configured");
            alert.setHeaderText("IGDB API credentials not found");
            alert.setContentText("To load real games from IGDB:\n\n" +
                    "1. Go to dev.twitch.tv/console/apps\n" +
                    "2. Register your application\n" +
                    "3. Add your Client ID and Secret to:\n" +
                    "   src/main/resources/config.properties\n\n" +
                    "Loading sample games for now...");
            alert.showAndWait();
            
            createSampleGames();
            showBrowseGames();
            return;
        }
        
        // Show loading dialog
        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Loading Games");
        loadingAlert.setHeaderText("Fetching games from IGDB...");
        loadingAlert.setContentText("Please wait while we load popular games from the database.");
        
        // Use a separate thread to avoid blocking the UI
        new Thread(() -> {
            try {
                // Fetch popular games from IGDB
                List<VideoGame> games = igdbService.getPopularGames(50);
                
                // Save to database
                int savedCount = 0;
                for (VideoGame game : games) {
                    gameDAO.saveGame(game);
                    savedCount++;
                }
                
                final int count = savedCount;
                
                // Update UI on JavaFX thread
                javafx.application.Platform.runLater(() -> {
                    loadingAlert.close();
                    
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Success");
                    successAlert.setHeaderText("Games Loaded!");
                    successAlert.setContentText("Successfully loaded " + count + " games from IGDB.");
                    successAlert.showAndWait();
                    
                    showBrowseGames();
                });
                
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    loadingAlert.close();
                    
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Error");
                    errorAlert.setHeaderText("Failed to load games from IGDB");
                    errorAlert.setContentText("Error: " + e.getMessage() + "\n\nLoading sample games instead...");
                    errorAlert.showAndWait();
                    
                    createSampleGames();
                    showBrowseGames();
                });
            }
        }).start();
        
        loadingAlert.show();
    }

    private void createSampleGames() {
        // Sample games for demonstration
        String[] sampleGames = {
            "The Witcher 3|Epic RPG adventure|49.99|Action,RPG,Fantasy",
            "Cyberpunk 2077|Futuristic open world|59.99|Action,RPG,Sci-Fi",
            "Minecraft|Sandbox building game|26.95|Sandbox,Survival,Creative",
            "Elden Ring|Dark fantasy action RPG|59.99|Action,RPG,Fantasy",
            "God of War|Norse mythology adventure|49.99|Action,Adventure"
        };
        
        for (String gameData : sampleGames) {
            String[] parts = gameData.split("\\|");
            VideoGame game = new VideoGame();
            game.setTitle(parts[0]);
            game.setDescription(parts[1]);
            game.setPrice(Double.parseDouble(parts[2]));
            game.setRating(4.0 + Math.random());
            game.setRatingCount(1000 + (int)(Math.random() * 5000));
            
            List<String> tags = List.of(parts[3].split(","));
            game.setTags(tags);
            
            gameDAO.saveGame(game);
        }
    }

    private void showGameManagement() {
        // Placeholder for admin game management
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Management");
        alert.setContentText("Admin game management interface would go here.");
        alert.showAndWait();
    }

    private void showUserManagement() {
        // Placeholder for admin user management
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("User Management");
        alert.setContentText("Admin user management interface would go here.");
        alert.showAndWait();
    }

    private void logout() {
        authService.logout();
        LoginView loginView = new LoginView(stage);
        javafx.scene.Scene scene = new javafx.scene.Scene(loginView.getView(), 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        stage.setScene(scene);
    }

    public BorderPane getView() {
        return view;
    }
}
