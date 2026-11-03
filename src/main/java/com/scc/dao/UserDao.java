package com.scc.dao;

import com.scc.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public boolean register(String username, String password, String role) {
        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            System.out.println("Username and password are required.");
            return false;
        }

        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        if (!normalizedRole.matches("ADMIN|INSTRUCTOR|STUDENT")) {
            System.out.println("Invalid role.");
            return false;
        }

        String sql = """
            INSERT INTO users (username, password_hash, role, status)
            VALUES (?, ?, ?, 'PENDING')
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username.trim());
            statement.setString(2, hashPassword(password));
            statement.setString(3, normalizedRole);
            statement.executeUpdate();
            System.out.println("Registration successful. Account status: PENDING.");
            return true;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toUpperCase().contains("UNIQUE")) {
                System.out.println("Username already exists.");
            } else {
                System.out.println("Registration failed: " + e.getMessage());
            }
            return false;
        }
    }

    public User login(String username, String password) {
        String sql = """
            SELECT id, username, password_hash, role, status
            FROM users
            WHERE username = ?
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username == null ? "" : username.trim());

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || password == null) {
                    System.out.println("Invalid username or password.");
                    return null;
                }

                String storedPassword = result.getString("password_hash");
                String enteredPassword = hashPassword(password);
                if (storedPassword == null || !MessageDigest.isEqual(
                        storedPassword.getBytes(StandardCharsets.UTF_8),
                        enteredPassword.getBytes(StandardCharsets.UTF_8))) {
                    System.out.println("Invalid username or password.");
                    return null;
                }

                return new User(result.getInt("id"), result.getString("username"),
                        result.getString("role"), result.getString("status"));
            }
        } catch (SQLException e) {
            System.out.println("Login failed: " + e.getMessage());
            return null;
        }
    }

    /** Returns every account, regardless of role or status. */
    public List<User> getAllUsers() {
        return queryUsers("SELECT id, username, role, status FROM users ORDER BY id");
    }

    /** Returns every account awaiting approval, regardless of role. */
    public List<User> getPendingUsers() {
        return queryUsers("""
            SELECT id, username, role, status
            FROM users
            WHERE status = 'PENDING'
            ORDER BY id
            """);
    }

    /** Sets the account status to APPROVED using a parameterized update. */
    public boolean approveUser(int userId) {
        String sql = "UPDATE users SET status = 'APPROVED' WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Could not approve account: " + e.getMessage());
            return false;
        }
    }

    public void logActivity(Integer userId, String action, String description) {
        String sql = """
            INSERT INTO activity_logs (user_id, action, description, event_timestamp)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
            """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (userId == null) {
                statement.setNull(1, Types.INTEGER);
            } else {
                statement.setInt(1, userId);
            }
            statement.setString(2, action);
            statement.setString(3, description);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Activity log failed: " + e.getMessage());
        }
    }

    private List<User> queryUsers(String sql) {
        List<User> users = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                users.add(new User(result.getInt("id"), result.getString("username"),
                        result.getString("role"), result.getString("status")));
            }
        } catch (SQLException e) {
            System.out.println("Could not retrieve users: " + e.getMessage());
        }
        return users;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Password hashing failed.", e);
        }
    }
}
