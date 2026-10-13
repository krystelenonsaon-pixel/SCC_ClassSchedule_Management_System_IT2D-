
package com.scc.dao;

import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDao {

    // REGISTER ACCOUNT
    public boolean register(
            String username,
            String password,
            String role
    ) {
        if (username == null || username.isBlank()
                || password == null || password.isEmpty()) {
            System.out.println("Username and password are required.");
            return false;
        }

        username = username.trim();

        if (role == null) {
            System.out.println("Please select a role.");
            return false;
        }

        role = role.trim().toUpperCase();

        if (!role.equals("TEACHER") && !role.equals("STUDENT")) {
            System.out.println("Invalid role. Select TEACHER or STUDENT.");
            return false;
        }

        String sql = """
                INSERT INTO users (username, password, roles)
                VALUES (?, ?, ?)
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username);

            // Store the exact password for this classroom demonstration.
            statement.setString(2, password);

            statement.setString(3, role);

            statement.executeUpdate();

            System.out.println("Registration successful!");
            System.out.println("Username: " + username);
            System.out.println("Role: " + role);
            System.out.println("Status: PENDING");

            return true;

        } catch (SQLException e) {
            if (e.getMessage() != null
                    && e.getMessage().contains("UNIQUE")) {
                System.out.println("Username already exists.");
            } else {
                System.out.println(
                        "Registration failed: " + e.getMessage()
                );
            }

            return false;
        }
    }

    // LOGIN ACCOUNT
    public User login(String username, String password) {
        if (username == null || username.isBlank()
                || password == null) {
            System.out.println("Username and password are required.");
            return null;
        }

        String sql = """
                SELECT id, username, password, roles, status
                FROM users
                WHERE username = ? AND password = ?
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username.trim());
            statement.setString(2, password);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return new User(
                            result.getInt("id"),
                            result.getString("username"),
                            result.getString("password"),
                            result.getString("roles"),
                            result.getString("status")
                    );
                }
            }

            System.out.println("Invalid username or password.");
            return null;

        } catch (SQLException e) {
            System.out.println("Login failed: " + e.getMessage());
            return null;
        }
    }
}
