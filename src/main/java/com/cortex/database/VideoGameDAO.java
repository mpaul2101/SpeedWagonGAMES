package com.cortex.database;

import com.cortex.model.VideoGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for VideoGame operations.
 */
public class VideoGameDAO {
    private static final Logger logger = LoggerFactory.getLogger(VideoGameDAO.class);
    private final DatabaseManager dbManager;

    public VideoGameDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Create or update a video game in the database.
     */
    public int saveGame(VideoGame game) {
        // Check if game already exists by IGDB ID
        if (game.getIgdbId() > 0) {
            VideoGame existing = findByIgdbId(game.getIgdbId());
            if (existing != null) {
                game.setId(existing.getId());
                updateGame(game);
                return existing.getId();
            }
        }
        
        String sql = "INSERT INTO video_games (igdb_id, title, description, price, cover_image_url, " +
                    "release_date, rating, rating_count, developer, publisher, available) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, game.getIgdbId());
            pstmt.setString(2, game.getTitle());
            pstmt.setString(3, game.getDescription());
            pstmt.setDouble(4, game.getPrice());
            pstmt.setString(5, game.getCoverImageUrl());
            pstmt.setDate(6, game.getReleaseDate() != null ? Date.valueOf(game.getReleaseDate()) : null);
            pstmt.setDouble(7, game.getRating());
            pstmt.setInt(8, game.getRatingCount());
            pstmt.setString(9, game.getDeveloper());
            pstmt.setString(10, game.getPublisher());
            pstmt.setInt(11, game.isAvailable() ? 1 : 0);
            
            pstmt.executeUpdate();
            
            // SQLite: get last inserted ID
            try (Statement stmt = dbManager.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    int gameId = rs.getInt(1);
                    game.setId(gameId);
                    
                    // Save tags and platforms
                    saveTags(gameId, game.getTags());
                    savePlatforms(gameId, game.getPlatforms());
                    
                    logger.info("Created game: {}", game.getTitle());
                    return gameId;
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving game", e);
        }
        
        return -1;
    }

    /**
     * Update an existing game.
     */
    private void updateGame(VideoGame game) {
        String sql = "UPDATE video_games SET title = ?, description = ?, price = ?, " +
                    "cover_image_url = ?, release_date = ?, rating = ?, rating_count = ?, " +
                    "developer = ?, publisher = ?, available = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, game.getTitle());
            pstmt.setString(2, game.getDescription());
            pstmt.setDouble(3, game.getPrice());
            pstmt.setString(4, game.getCoverImageUrl());
            pstmt.setDate(5, game.getReleaseDate() != null ? Date.valueOf(game.getReleaseDate()) : null);
            pstmt.setDouble(6, game.getRating());
            pstmt.setInt(7, game.getRatingCount());
            pstmt.setString(8, game.getDeveloper());
            pstmt.setString(9, game.getPublisher());
            pstmt.setInt(10, game.isAvailable() ? 1 : 0);
            pstmt.setInt(11, game.getId());
            
            pstmt.executeUpdate();
            
            // Update tags and platforms
            saveTags(game.getId(), game.getTags());
            savePlatforms(game.getId(), game.getPlatforms());
            
            logger.info("Updated game: {}", game.getTitle());
        } catch (SQLException e) {
            logger.error("Error updating game", e);
        }
    }

    /**
     * Save game tags.
     */
    private void saveTags(int gameId, List<String> tags) {
        // Delete existing tags
        String deleteSql = "DELETE FROM game_tags WHERE game_id = ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(deleteSql)) {
            pstmt.setInt(1, gameId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting old tags", e);
        }
        
        // Insert new tags
        String insertSql = "INSERT INTO game_tags (game_id, tag) VALUES (?, ?)";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(insertSql)) {
            for (String tag : tags) {
                pstmt.setInt(1, gameId);
                pstmt.setString(2, tag);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Error inserting tags", e);
        }
    }

    /**
     * Save game platforms.
     */
    private void savePlatforms(int gameId, List<String> platforms) {
        // Delete existing platforms
        String deleteSql = "DELETE FROM game_platforms WHERE game_id = ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(deleteSql)) {
            pstmt.setInt(1, gameId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting old platforms", e);
        }
        
        // Insert new platforms
        String insertSql = "INSERT INTO game_platforms (game_id, platform) VALUES (?, ?)";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(insertSql)) {
            for (String platform : platforms) {
                pstmt.setInt(1, gameId);
                pstmt.setString(2, platform);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Error inserting platforms", e);
        }
    }

    /**
     * Find game by ID.
     */
    public VideoGame findById(int id) {
        String sql = "SELECT * FROM video_games WHERE id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                VideoGame game = mapResultSetToVideoGame(rs);
                loadGameRelations(game);
                return game;
            }
        } catch (SQLException e) {
            logger.error("Error finding game by ID", e);
        }
        
        return null;
    }

    /**
     * Find game by IGDB ID.
     */
    public VideoGame findByIgdbId(long igdbId) {
        String sql = "SELECT * FROM video_games WHERE igdb_id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, igdbId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                VideoGame game = mapResultSetToVideoGame(rs);
                loadGameRelations(game);
                return game;
            }
        } catch (SQLException e) {
            logger.error("Error finding game by IGDB ID", e);
        }
        
        return null;
    }

    /**
     * Find all games.
     */
    public List<VideoGame> findAll() {
        List<VideoGame> games = new ArrayList<>();
        String sql = "SELECT * FROM video_games WHERE available = 1 ORDER BY rating DESC";
        
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                VideoGame game = mapResultSetToVideoGame(rs);
                loadGameRelations(game);
                games.add(game);
            }
        } catch (SQLException e) {
            logger.error("Error finding all games", e);
        }
        
        return games;
    }

    /**
     * Search games by title.
     */
    public List<VideoGame> searchByTitle(String query) {
        List<VideoGame> games = new ArrayList<>();
        String sql = "SELECT * FROM video_games WHERE title LIKE ? AND available = 1";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, "%" + query + "%");
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                VideoGame game = mapResultSetToVideoGame(rs);
                loadGameRelations(game);
                games.add(game);
            }
        } catch (SQLException e) {
            logger.error("Error searching games by title", e);
        }
        
        return games;
    }

    /**
     * Find games by tag.
     */
    public List<VideoGame> findByTag(String tag) {
        List<VideoGame> games = new ArrayList<>();
        String sql = "SELECT DISTINCT vg.* FROM video_games vg " +
                    "JOIN game_tags gt ON vg.id = gt.game_id " +
                    "WHERE gt.tag = ? AND vg.available = 1";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, tag);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                VideoGame game = mapResultSetToVideoGame(rs);
                loadGameRelations(game);
                games.add(game);
            }
        } catch (SQLException e) {
            logger.error("Error finding games by tag", e);
        }
        
        return games;
    }

    /**
     * Load game relations (tags and platforms).
     */
    private void loadGameRelations(VideoGame game) {
        loadTags(game);
        loadPlatforms(game);
    }

    /**
     * Load game tags.
     */
    private void loadTags(VideoGame game) {
        String sql = "SELECT tag FROM game_tags WHERE game_id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, game.getId());
            ResultSet rs = pstmt.executeQuery();
            
            List<String> tags = new ArrayList<>();
            while (rs.next()) {
                tags.add(rs.getString("tag"));
            }
            game.setTags(tags);
        } catch (SQLException e) {
            logger.error("Error loading tags", e);
        }
    }

    /**
     * Load game platforms.
     */
    private void loadPlatforms(VideoGame game) {
        String sql = "SELECT platform FROM game_platforms WHERE game_id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, game.getId());
            ResultSet rs = pstmt.executeQuery();
            
            List<String> platforms = new ArrayList<>();
            while (rs.next()) {
                platforms.add(rs.getString("platform"));
            }
            game.setPlatforms(platforms);
        } catch (SQLException e) {
            logger.error("Error loading platforms", e);
        }
    }

    /**
     * Map ResultSet to VideoGame object.
     */
    public VideoGame mapResultSetToVideoGame(ResultSet rs) throws SQLException {
        VideoGame game = new VideoGame();
        
        game.setId(rs.getInt("id"));
        game.setIgdbId(rs.getLong("igdb_id"));
        game.setTitle(rs.getString("title"));
        game.setDescription(rs.getString("description"));
        game.setPrice(rs.getDouble("price"));
        game.setCoverImageUrl(rs.getString("cover_image_url"));
        
        Date releaseDate = rs.getDate("release_date");
        if (releaseDate != null) {
            game.setReleaseDate(releaseDate.toLocalDate());
        }
        
        game.setRating(rs.getDouble("rating"));
        game.setRatingCount(rs.getInt("rating_count"));
        game.setDeveloper(rs.getString("developer"));
        game.setPublisher(rs.getString("publisher"));
        game.setAvailable(rs.getInt("available") == 1);
        
        return game;
    }

    /**
     * Delete a game.
     */
    public void delete(int gameId) {
        String sql = "DELETE FROM video_games WHERE id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            pstmt.executeUpdate();
            logger.info("Deleted game with ID: {}", gameId);
        } catch (SQLException e) {
            logger.error("Error deleting game", e);
        }
    }
}
