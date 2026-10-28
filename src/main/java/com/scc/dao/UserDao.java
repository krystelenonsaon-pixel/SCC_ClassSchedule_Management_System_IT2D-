
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

public class UserDao {

    // Register a new user account
    public boolean register(
            String username,
            String password,
            String role) {

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            System.out.println(
                    "Username and password are required.");
            return false;
        }

        String normalizedRole =
                role == null ? "" : role.trim().toUpperCase();

        if (!normalizedRole.matches(
                "ADMIN|INSTRUCTOR|STUDENT")) {
            System.out.println("Invalid role.");
            return false;
        }

        String sql = """
                INSERT INTO users (username, password, roles, status)
                VALUES (?, ?, ?, 'PENDING')
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username.trim());
            statement.setString(2, hashPassword(password));
            statement.setString(3, normalizedRole);

            statement.executeUpdate();

            System.out.println("Registration successful!");
            System.out.println("Account status: PENDING");
            return true;

        } catch (SQLException e) {
            if (e.getMessage() != null
                    && e.getMessage().toUpperCase()
                    .contains("UNIQUE")) {
                System.out.println("Username already exists.");
            } else {
                System.out.println(
                        "Registration failed: " + e.getMessage());
            }
            return false;
        }
    }

    // Authenticate a user
    public User login(String username, String password) {

        String sql = """
                SELECT id, username, password, roles, status
                FROM users
                WHERE username = ?
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1, username == null ? "" : username.trim());

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next() || password == null) {
                    System.out.println(
                            "Invalid username or password.");
                    return null;
                }

                String storedPassword =
                        result.getString("password");

                String enteredPassword =
                        hashPassword(password);

                if (storedPassword == null
                        || !MessageDigest.isEqual(
                        storedPassword.getBytes(
                                StandardCharsets.UTF_8),
                        enteredPassword.getBytes(
                                StandardCharsets.UTF_8))) {
                    System.out.println(
                            "Invalid username or password.");
                    return null;
                }

                return new User(
                        result.getInt("id"),
                        result.getString("username"),
                        result.getString("roles"),
                        result.getString("status")
                );
            }

        } catch (SQLException e) {
            System.out.println(
                    "Login failed: " + e.getMessage());
            return null;
        }
    }

    // Record user activity in activity_logs
    public void logActivity(
            Integer userId,
            String eventType,
            String description) {

        String sql = """
                INSERT INTO activity_logs
                    (user_id, event_type, description, event_timestamp)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            if (userId == null) {
                statement.setNull(1, Types.INTEGER);
            } else {
                statement.setInt(1, userId);
            }

            statement.setString(2, eventType);
            statement.setString(3, description);

            statement.executeUpdate();

        } catch (SQLException e) {
            System.err.println(
                    "Activity log failed: " + e.getMessage());
        }
    }

    // SHA-256 password hashing
    private String hashPassword(String password) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8));

            return java.util.HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "Password hashing failed.", e);
        }
    }
}
