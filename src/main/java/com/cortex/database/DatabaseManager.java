package com.cortex.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_URL = "jdbc:sqlite:cortex_library.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

   
    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            createTables();
            logger.info("Database initialized successfully");
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
        }
    }

    /**
     * Create all necessary tables.
     */
    private void createTables() throws SQLException {
        Statement stmt = connection.createStatement();

        // Users table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS users (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "username TEXT UNIQUE NOT NULL, " +
            "email TEXT UNIQUE NOT NULL, " +
            "password_hash TEXT NOT NULL, " +
            "is_admin INTEGER DEFAULT 0, " +
            "admin_level TEXT, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "last_login TIMESTAMP" +
            ")"
        );

        // Video games table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS video_games (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "igdb_id INTEGER UNIQUE, " +
            "title TEXT NOT NULL, " +
            "description TEXT, " +
            "price REAL NOT NULL, " +
            "cover_image_url TEXT, " +
            "release_date DATE, " +
            "rating REAL DEFAULT 0.0, " +
            "rating_count INTEGER DEFAULT 0, " +
            "developer TEXT, " +
            "publisher TEXT, " +
            "available INTEGER DEFAULT 1" +
            ")"
        );

        // Game tags table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS game_tags (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "game_id INTEGER NOT NULL, " +
            "tag TEXT NOT NULL, " +
            "FOREIGN KEY (game_id) REFERENCES video_games(id) ON DELETE CASCADE, " +
            "UNIQUE(game_id, tag)" +
            ")"
        );

        // Game platforms table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS game_platforms (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "game_id INTEGER NOT NULL, " +
            "platform TEXT NOT NULL, " +
            "FOREIGN KEY (game_id) REFERENCES video_games(id) ON DELETE CASCADE, " +
            "UNIQUE(game_id, platform)" +
            ")"
        );

        // User owned games table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS user_owned_games (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "user_id INTEGER NOT NULL, " +
            "game_id INTEGER NOT NULL, " +
            "purchased_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
            "FOREIGN KEY (game_id) REFERENCES video_games(id) ON DELETE CASCADE, " +
            "UNIQUE(user_id, game_id)" +
            ")"
        );

        // User wishlist table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS user_wishlist (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "user_id INTEGER NOT NULL, " +
            "game_id INTEGER NOT NULL, " +
            "added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
            "FOREIGN KEY (game_id) REFERENCES video_games(id) ON DELETE CASCADE, " +
            "UNIQUE(user_id, game_id)" +
            ")"
        );

        // User preferred tags table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS user_preferred_tags (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "user_id INTEGER NOT NULL, " +
            "tag TEXT NOT NULL, " +
            "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
            "UNIQUE(user_id, tag)" +
            ")"
        );

        // Game ratings table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS game_ratings (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "user_id INTEGER NOT NULL, " +
            "game_id INTEGER NOT NULL, " +
            "rating INTEGER NOT NULL CHECK(rating >= 1 AND rating <= 5), " +
            "review TEXT, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
            "FOREIGN KEY (game_id) REFERENCES video_games(id) ON DELETE CASCADE, " +
            "UNIQUE(user_id, game_id)" +
            ")"
        );

        // Create indexes for better performance
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_games_title ON video_games(title)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_games_rating ON video_games(rating)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_game_tags_tag ON game_tags(tag)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_user_owned_games_user ON user_owned_games(user_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_user_wishlist_user ON user_wishlist(user_id)");

        stmt.close();
        logger.info("Database tables created successfully");
    }

    /**
     * Get database connection.
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (SQLException e) {
            logger.error("Failed to get database connection", e);
        }
        return connection;
    }

    /**
     * Close database connection.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed");
            }
        } catch (SQLException e) {
            logger.error("Error closing database connection", e);
        }
    }
}
