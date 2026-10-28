
package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Menu {

    private final Scanner scanner;
    private final UserDao userDao;

    public Menu(Scanner scanner, UserDao userDao) {
        this.scanner = scanner;
        this.userDao = userDao;
    }

    public void start() {
        boolean running = true;

        while (running) {
            System.out.println("\n==================================");
            System.out.println(" SCC CLASS SCHEDULE MANAGEMENT");
            System.out.println("==================================");
            System.out.println("1. Register Account");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> register();
                case "2" -> login();
                case "3" -> {
                    running = false;
                    System.out.println("Thank you for using the system!");
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void register() {
        System.out.println("\n--- ACCOUNT REGISTRATION ---");

        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        System.out.println("\nSelect your role:");
        System.out.println("1. Administrator");
        System.out.println("2. Instructor");
        System.out.println("3. Student");
        System.out.print("Enter choice: ");

        String roleChoice = scanner.nextLine().trim();

        String role = switch (roleChoice) {
            case "1" -> "ADMIN";
            case "2" -> "INSTRUCTOR";
            case "3" -> "STUDENT";
            default -> "";
        };

        if (role.isEmpty()) {
            System.out.println("Invalid role selection.");
            return;
        }

        boolean success =
                userDao.register(username, password, role);

        if (success) {
            System.out.println(
                    "Your account is registered and PENDING approval."
            );
        }
    }

    private void login() {
        System.out.println("\n--- LOGIN ---");

        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        User user = userDao.login(username, password);

        if (user == null) {
            System.out.println("Login failed. Check your credentials.");
            return;
        }

        System.out.println("\nLogin successful!");
        System.out.println("Welcome, " + user.username() + "!");
        System.out.println("Role: " + user.role());
        System.out.println("Account status: " + user.status());

        if ("PENDING".equalsIgnoreCase(user.status())) {
            System.out.println(
                    "Your account is awaiting approval."
            );
            return;
        }

        if (!"APPROVED".equalsIgnoreCase(user.status())) {
            System.out.println(
                    "Your account is not approved for access."
            );
            return;
        }

        roleMenu(user);
    }

    private void roleMenu(User user) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n==================================");
            System.out.println(" " + user.role() + " MENU");
            System.out.println("==================================");

            switch (user.role()) {
                case "ADMIN" -> {
                    System.out.println("1. Manage Class Schedules");
                    System.out.println("2. View Class Schedules");
                    System.out.println("3. Logout");
                }
                case "INSTRUCTOR" -> {
                    System.out.println("1. View My Class Schedules");
                    System.out.println("2. Logout");
                }
                case "STUDENT" -> {
                    System.out.println("1. View My Class Schedule");
                    System.out.println("2. Logout");
                }
                default -> {
                    System.out.println("Unknown role.");
                    return;
                }
            }

            System.out.print("Choose an option: ");
            String choice = scanner.nextLine().trim();

            if ("ADMIN".equals(user.role())) {
                switch (choice) {
                    case "1" ->
                            System.out.println(
                                    "Schedule management is not implemented yet."
                            );
                    case "2" ->
                            System.out.println(
                                    "Schedule viewing is not implemented yet."
                            );
                    case "3" -> loggedIn = false;
                    default ->
                            System.out.println("Invalid choice.");
                }
            } else {
                switch (choice) {
                    case "1" ->
                            System.out.println(
                                    "Schedule viewing is not implemented yet."
                            );
                    case "2" -> loggedIn = false;
                    default ->
                            System.out.println("Invalid choice.");
                }
            }
        }

        System.out.println("Logged out successfully.");
    }
}
