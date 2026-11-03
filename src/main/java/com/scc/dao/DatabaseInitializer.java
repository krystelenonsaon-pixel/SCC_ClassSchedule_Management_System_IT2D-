package com.scc.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public final class DatabaseInitializer {
    private DatabaseInitializer() {
    }

    public static void initialize() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            if (!tableExists(connection, "users")) {
                createUsersTable(connection, "users");
            } else {
                Set<String> columns = getColumns(connection, "users");
                if (!columns.contains("password_hash")
                        || !columns.contains("role")
                        || !columns.contains("created_at")) {
                    migrateLegacyUsersTable(connection, columns);
                }
            }

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS activity_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER,
                        action TEXT NOT NULL,
                        description TEXT,
                        event_timestamp TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
                    )
                    """);
            }

            // Confirm enforcement is enabled for this connection.
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("PRAGMA foreign_keys")) {
                if (!rs.next() || rs.getInt(1) != 1) {
                    throw new SQLException("SQLite foreign-key enforcement is disabled.");
                }
            }

            System.out.println("Database initialized successfully. Foreign keys are ON.");
        } catch (SQLException e) {
            throw new IllegalStateException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    private static boolean tableExists(Connection connection, String tableName)
            throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            statement.setString(1, tableName);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static Set<String> getColumns(Connection connection, String tableName)
            throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                columns.add(rs.getString("name").toLowerCase());
            }
        }
        return columns;
    }

    private static void createUsersTable(Connection connection, String tableName)
            throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS %s (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    role TEXT NOT NULL CHECK (role IN ('ADMIN', 'INSTRUCTOR', 'STUDENT')),
                    status TEXT NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(tableName));
        }
    }

    /*
     * Migrate the older project schema without discarding existing accounts.
     * The original password values are already hashes in this project, so they
     * are copied as-is into password_hash.
     */
    private static void migrateLegacyUsersTable(Connection connection, Set<String> columns)
            throws SQLException {
        boolean oldPasswordColumn = columns.contains("password");
        boolean oldRolesColumn = columns.contains("roles");
        if (!columns.contains("username") || (!oldPasswordColumn && !columns.contains("password_hash"))) {
            throw new SQLException("The existing users table has an unrecognized schema. Back up the database before changing it.");
        }

        try (Statement statement = connection.createStatement()) {
            // Foreign-key mode must be changed outside a transaction.
            statement.execute("PRAGMA foreign_keys = OFF");
            statement.execute("BEGIN");
            statement.execute("DROP TABLE IF EXISTS users_new");
            createUsersTable(connection, "users_new");

            String passwordExpression = oldPasswordColumn ? "password" : "password_hash";
            String roleExpression;
            if (oldRolesColumn) {
                roleExpression = """
                    CASE UPPER(roles)
                        WHEN 'TEACHER' THEN 'INSTRUCTOR'
                        WHEN 'INSTRUCTOR' THEN 'INSTRUCTOR'
                        WHEN 'ADMINISTRATOR' THEN 'ADMIN'
                        WHEN 'ADMIN' THEN 'ADMIN'
                        WHEN 'STUDENT' THEN 'STUDENT'
                        ELSE 'STUDENT'
                    END
                    """;
            } else if (columns.contains("role")) {
                roleExpression = """
                    CASE UPPER(role)
                        WHEN 'TEACHER' THEN 'INSTRUCTOR'
                        WHEN 'INSTRUCTOR' THEN 'INSTRUCTOR'
                        WHEN 'ADMINISTRATOR' THEN 'ADMIN'
                        WHEN 'ADMIN' THEN 'ADMIN'
                        WHEN 'STUDENT' THEN 'STUDENT'
                        ELSE 'STUDENT'
                    END
                    """;
            } else {
                roleExpression = "'STUDENT'";
            }

            String statusExpression = columns.contains("status")
                    ? "CASE UPPER(status) WHEN 'APPROVED' THEN 'APPROVED' WHEN 'REJECTED' THEN 'REJECTED' ELSE 'PENDING' END"
                    : "'PENDING'";
            String idExpression = columns.contains("id") ? "id" : "NULL";
            String createdExpression = columns.contains("created_at")
                    ? "COALESCE(created_at, CURRENT_TIMESTAMP)" : "CURRENT_TIMESTAMP";

            String copySql = """
                INSERT INTO users_new (id, username, password_hash, role, status, created_at)
                SELECT %s, username, %s, %s, %s, %s FROM users
                """.formatted(idExpression, passwordExpression, roleExpression,
                    statusExpression, createdExpression);
            statement.executeUpdate(copySql);
            statement.execute("DROP TABLE users");
            statement.execute("ALTER TABLE users_new RENAME TO users");
            statement.execute("COMMIT");
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            try (Statement rollback = connection.createStatement()) {
                rollback.execute("ROLLBACK");
                rollback.execute("PRAGMA foreign_keys = ON");
            } catch (SQLException ignored) {
                // Preserve the original migration error.
            }
            throw e;
        }
    }
}
