package com.cortex.ui;

import com.cortex.model.User;
import com.cortex.service.AuthenticationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * Login view for user authentication.
 */
public class LoginView {
    private final Stage stage;
    private final AuthenticationService authService;
    private final VBox view;

    public LoginView(Stage stage) {
        this.stage = stage;
        this.authService = new AuthenticationService();
        this.view = createView();
    }

    private VBox createView() {
        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(50));
        container.getStyleClass().add("login-container");

        // Title
        Label titleLabel = new Label("SPEED WAGON Game Library");
        titleLabel.getStyleClass().add("title");

        // Login form
        VBox loginForm = createLoginForm();

        // Register link
        Hyperlink registerLink = new Hyperlink("Don't have an account? Register here");
        registerLink.setOnAction(e -> showRegisterForm());

        container.getChildren().addAll(titleLabel, loginForm, registerLink);
        return container;
    }

    private VBox createLoginForm() {
        VBox form = new VBox(15);
        form.setMaxWidth(400);
        form.setAlignment(Pos.CENTER);

        Label headerLabel = new Label("Login");
        headerLabel.getStyleClass().add("form-header");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("text-field");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("text-field");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);

        Button loginButton = new Button("Login");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);

        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Please fill in all fields");
                errorLabel.setVisible(true);
                return;
            }

            User user = authService.login(username, password);
            if (user != null) {
                openMainView(user);
            } else {
                errorLabel.setText("Invalid username or password");
                errorLabel.setVisible(true);
            }
        });

        // Allow Enter key to login
        passwordField.setOnAction(e -> loginButton.fire());

        form.getChildren().addAll(headerLabel, usernameField, passwordField, errorLabel, loginButton);
        return form;
    }

    private void showRegisterForm() {
        VBox form = new VBox(15);
        form.setMaxWidth(400);
        form.setAlignment(Pos.CENTER);

        Label headerLabel = new Label("Register");
        headerLabel.getStyleClass().add("form-header");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("text-field");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        emailField.getStyleClass().add("text-field");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password (min 6 characters)");
        passwordField.getStyleClass().add("text-field");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Password");
        confirmPasswordField.getStyleClass().add("text-field");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);

        Button registerButton = new Button("Register");
        registerButton.getStyleClass().add("primary-button");
        registerButton.setMaxWidth(Double.MAX_VALUE);

        registerButton.setOnAction(e -> {
            String username = usernameField.getText();
            String email = emailField.getText();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Please fill in all fields");
                errorLabel.setVisible(true);
                return;
            }

            if (!password.equals(confirmPassword)) {
                errorLabel.setText("Passwords do not match");
                errorLabel.setVisible(true);
                return;
            }

            if (authService.register(username, email, password)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("Registration successful! Please login.");
                alert.showAndWait();
                
                view.getChildren().clear();
                view.getChildren().addAll(createView().getChildren());
            } else {
                // Show specific error message from authentication service
                String errorMsg = authService.getLastError();
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = "Registration failed. Please try again.";
                }
                errorLabel.setText(errorMsg);
                errorLabel.setVisible(true);
            }
        });

        Hyperlink backLink = new Hyperlink("Back to login");
        backLink.setOnAction(e -> {
            view.getChildren().clear();
            view.getChildren().addAll(createView().getChildren());
        });

        form.getChildren().addAll(headerLabel, usernameField, emailField, passwordField, 
                                   confirmPasswordField, errorLabel, registerButton, backLink);

        view.getChildren().clear();
        view.getChildren().add(form);
    }

    private void openMainView(User user) {
        MainView mainView = new MainView(stage, authService, user);
        Scene scene = new Scene(mainView.getView(), 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        stage.setScene(scene);
    }

    public VBox getView() {
        return view;
    }
}
