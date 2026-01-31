package com.cortex.service;

import com.cortex.database.UserDAO;
import com.cortex.model.Admin;
import com.cortex.model.User;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for user authentication and authorization.
 */
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private final UserDAO userDAO;
    private User currentUser;

    public AuthenticationService() {
        this.userDAO = new UserDAO();
    }

    private String lastError = "";

    /**
     * Register a new user.
     */
    public boolean register(String username, String email, String password) {
        lastError = "";
        
        // Validate inputs
        if (username == null || username.trim().isEmpty()) {
            logger.warn("Registration failed: empty username");
            lastError = "Username cannot be empty";
            return false;
        }
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            logger.warn("Registration failed: invalid email");
            lastError = "Please enter a valid email address";
            return false;
        }
        if (password == null || password.length() < 6) {
            logger.warn("Registration failed: password too short");
            lastError = "Password must be at least 6 characters long";
            return false;
        }

        // Check if username already exists
        try {
            User existingUser = userDAO.findByUsername(username);
            if (existingUser != null) {
                logger.warn("Registration failed: username already exists - {}", username);
                lastError = "Username '" + username + "' is already taken";
                return false;
            }
        } catch (Exception e) {
            logger.error("Error checking username availability", e);
            lastError = "Database error. Please try again.";
            return false;
        }

        // Check if email already exists
        try {
            User existingEmail = userDAO.findByEmail(email);
            if (existingEmail != null) {
                logger.warn("Registration failed: email already exists - {}", email);
                lastError = "Email '" + email + "' is already registered";
                return false;
            }
        } catch (Exception e) {
            logger.error("Error checking email availability", e);
            lastError = "Database error. Please try again.";
            return false;
        }

        // Hash password and create user
        try {
            String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
            User user = new User(username, email, passwordHash);
            
            int userId = userDAO.createUser(user);
            if (userId > 0) {
                logger.info("User registered successfully: {}", username);
                return true;
            } else {
                lastError = "Failed to create user account. Please try again.";
                return false;
            }
        } catch (Exception e) {
            logger.error("Error during registration", e);
            lastError = "Registration error: " + e.getMessage();
            return false;
        }
    }
    
    /**
     * Get the last error message.
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * Register a new admin.
     */
    public boolean registerAdmin(String username, String email, String password, String adminLevel) {
        // Validate inputs
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            return false;
        }
        if (password == null || password.length() < 6) {
            return false;
        }

        // Check if username already exists
        if (userDAO.findByUsername(username) != null) {
            return false;
        }

        // Hash password and create admin
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        Admin admin = new Admin(username, email, passwordHash, adminLevel);
        
        int userId = userDAO.createUser(admin);
        if (userId > 0) {
            logger.info("Admin registered successfully: {}", username);
            return true;
        }
        
        return false;
    }

    /**
     * Login a user.
     */
    public User login(String username, String password) {
        User user = userDAO.findByUsername(username);
        
        if (user == null) {
            logger.warn("Login failed: user not found - {}", username);
            return null;
        }

        // Verify password
        if (BCrypt.checkpw(password, user.getPasswordHash())) {
            currentUser = user;
            userDAO.updateLastLogin(user.getId());
            logger.info("User logged in: {}", username);
            return user;
        } else {
            logger.warn("Login failed: incorrect password for user - {}", username);
            return null;
        }
    }

    /**
     * Logout current user.
     */
    public void logout() {
        if (currentUser != null) {
            logger.info("User logged out: {}", currentUser.getUsername());
            currentUser = null;
        }
    }

    /**
     * Get currently logged in user.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Check if a user is logged in.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Check if current user is an admin.
     */
    public boolean isAdmin() {
        return currentUser instanceof Admin;
    }

    /**
     * Get current user as Admin (if applicable).
     */
    public Admin getCurrentAdmin() {
        if (isAdmin()) {
            return (Admin) currentUser;
        }
        return null;
    }
}
