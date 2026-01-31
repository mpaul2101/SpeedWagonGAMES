package com.cortex.database;

import com.cortex.model.User;
import com.cortex.model.Admin;
import com.cortex.model.VideoGame;
import com.cortex.model.GameRating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Data Access Object for User operations.
 */
public class UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);
    private final DatabaseManager dbManager;

    public UserDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Create a new user in the database.
     */
    public int createUser(User user) {
        String sql = "INSERT INTO users (username, email, password_hash, is_admin, admin_level) VALUES (?, ?, ?, ?, ?)";
        
        logger.debug("Attempting to create user: username={}, email={}", user.getUsername(), user.getEmail());
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setInt(4, user instanceof Admin ? 1 : 0);
            pstmt.setString(5, user instanceof Admin ? ((Admin) user).getAdminLevel() : null);
            
            int affected = pstmt.executeUpdate();
            logger.debug("Insert affected {} rows", affected);
            
            // SQLite: get last inserted ID using last_insert_rowid()
            try (Statement stmt = dbManager.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    int userId = rs.getInt(1);
                    user.setId(userId);
                    logger.info("Created user successfully: {} with ID {}", user.getUsername(), userId);
                    return userId;
                } else {
                    logger.error("No generated key returned after insert");
                }
            }
        } catch (SQLException e) {
            logger.error("Error creating user '{}': {}", user.getUsername(), e.getMessage());
            logger.error("SQL Error Code: {}", e.getErrorCode());
            if (e.getMessage().contains("UNIQUE") || e.getMessage().contains("unique")) {
                logger.error("Username or email already exists in database");
            }
        }
        
        return -1;
    }

    /**
     * Find user by username.
     */
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding user by username", e);
        }
        
        return null;
    }

    /**
     * Find user by email.
     */
    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding user by email", e);
        }
        
        return null;
    }

    /**
     * Find user by ID.
     */
    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = mapResultSetToUser(rs);
                loadUserRelations(user);
                return user;
            }
        } catch (SQLException e) {
            logger.error("Error finding user by ID", e);
        }
        
        return null;
    }

    /**
     * Update user's last login time.
     */
    public void updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating last login", e);
        }
    }

    /**
     * Add a game to user's owned games.
     */
    public void addOwnedGame(int userId, int gameId) {
        String sql = "INSERT OR IGNORE INTO user_owned_games (user_id, game_id) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, gameId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error adding owned game", e);
        }
    }

    /**
     * Add a game to user's wishlist.
     */
    public void addToWishlist(int userId, int gameId) {
        String sql = "INSERT OR IGNORE INTO user_wishlist (user_id, game_id) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, gameId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error adding to wishlist", e);
        }
    }

    /**
     * Remove a game from user's wishlist.
     */
    public void removeFromWishlist(int userId, int gameId) {
        String sql = "DELETE FROM user_wishlist WHERE user_id = ? AND game_id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, gameId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error removing from wishlist", e);
        }
    }

    /**
     * Add a preferred tag for a user.
     */
    public void addPreferredTag(int userId, String tag) {
        String sql = "INSERT OR IGNORE INTO user_preferred_tags (user_id, tag) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, tag);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error adding preferred tag", e);
        }
    }

    /**
     * Load user relations (owned games, wishlist, ratings, preferred tags).
     */
    private void loadUserRelations(User user) {
        loadOwnedGames(user);
        loadWishlist(user);
        loadPreferredTags(user);
        loadRatings(user);
    }

    /**
     * Load user's owned games.
     */
    private void loadOwnedGames(User user) {
        String sql = "SELECT vg.* FROM video_games vg " +
                    "JOIN user_owned_games uog ON vg.id = uog.game_id " +
                    "WHERE uog.user_id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, user.getId());
            ResultSet rs = pstmt.executeQuery();
            
            List<VideoGame> ownedGames = new ArrayList<>();
            while (rs.next()) {
                VideoGame game = new VideoGameDAO().mapResultSetToVideoGame(rs);
                ownedGames.add(game);
            }
            user.setOwnedGames(ownedGames);
        } catch (SQLException e) {
            logger.error("Error loading owned games", e);
        }
    }

    /**
     * Load user's wishlist.
     */
    private void loadWishlist(User user) {
        String sql = "SELECT vg.* FROM video_games vg " +
                    "JOIN user_wishlist uw ON vg.id = uw.game_id " +
                    "WHERE uw.user_id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, user.getId());
            ResultSet rs = pstmt.executeQuery();
            
            List<VideoGame> wishlist = new ArrayList<>();
            while (rs.next()) {
                VideoGame game = new VideoGameDAO().mapResultSetToVideoGame(rs);
                wishlist.add(game);
            }
            user.setWishlist(wishlist);
        } catch (SQLException e) {
            logger.error("Error loading wishlist", e);
        }
    }

    /**
     * Load user's preferred tags.
     */
    private void loadPreferredTags(User user) {
        String sql = "SELECT tag FROM user_preferred_tags WHERE user_id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, user.getId());
            ResultSet rs = pstmt.executeQuery();
            
            Set<String> tags = new HashSet<>();
            while (rs.next()) {
                tags.add(rs.getString("tag"));
            }
            user.setPreferredTags(tags);
        } catch (SQLException e) {
            logger.error("Error loading preferred tags", e);
        }
    }

    /**
     * Load user's game ratings.
     */
    private void loadRatings(User user) {
        String sql = "SELECT * FROM game_ratings WHERE user_id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, user.getId());
            ResultSet rs = pstmt.executeQuery();
            
            List<GameRating> ratings = new ArrayList<>();
            while (rs.next()) {
                GameRating rating = new GameRating();
                rating.setId(rs.getInt("id"));
                rating.setUserId(rs.getInt("user_id"));
                rating.setGameId(rs.getInt("game_id"));
                rating.setRating(rs.getInt("rating"));
                rating.setReview(rs.getString("review"));
                rating.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                ratings.add(rating);
            }
            user.setRatings(ratings);
        } catch (SQLException e) {
            logger.error("Error loading ratings", e);
        }
    }

    /**
     * Map ResultSet to User object.
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        boolean isAdmin = rs.getInt("is_admin") == 1;
        User user;
        
        if (isAdmin) {
            Admin admin = new Admin();
            admin.setAdminLevel(rs.getString("admin_level"));
            user = admin;
        } else {
            user = new User();
        }
        
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) {
            user.setLastLogin(lastLogin.toLocalDateTime());
        }
        
        return user;
    }

    /**
     * Get all users.
     */
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all users", e);
        }
        
        return users;
    }

    /**
     * Get all ratings from all users (for ML training).
     */
    public List<GameRating> getAllRatings() {
        List<GameRating> ratings = new ArrayList<>();
        String sql = "SELECT * FROM game_ratings";
        
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                GameRating rating = new GameRating();
                rating.setId(rs.getInt("id"));
                rating.setUserId(rs.getInt("user_id"));
                rating.setGameId(rs.getInt("game_id"));
                rating.setRating(rs.getInt("rating"));
                rating.setReview(rs.getString("review"));
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    rating.setCreatedAt(createdAt.toLocalDateTime());
                }
                ratings.add(rating);
            }
        } catch (SQLException e) {
            logger.error("Error getting all ratings", e);
        }
        
        return ratings;
    }
}
