package com.cortex;

import com.cortex.ui.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application entry point.
 */
public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String APP_TITLE = "SPEED WAGON - Video Game Library";
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting SPEED WAGON Video Game Library");

            // Set up the login view
            LoginView loginView = new LoginView(primaryStage);
            Scene scene = new Scene(loginView.getView(), WINDOW_WIDTH, WINDOW_HEIGHT);
            
            // Add CSS styling
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            
            // Set application icon (optional)
            // primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));
            
            primaryStage.show();
            
            logger.info("Application started successfully");
        } catch (Exception e) {
            logger.error("Error starting application", e);
        }
    }

    @Override
    public void stop() {
        logger.info("Stopping application");
        // Clean up resources
        com.cortex.database.DatabaseManager.getInstance().close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
