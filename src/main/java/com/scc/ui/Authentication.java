
package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Authentication {

    private final Scanner scanner;
    private final UserDao userDao;

    public Authentication(
            Scanner scanner,
            UserDao userDao) {
        this.scanner = scanner;
        this.userDao = userDao;
    }

    public void start() {

        boolean running = true;

        while (running) {
            System.out.println("\n================================");
            System.out.println(" SCC CLASS SCHEDULE SYSTEM");
            System.out.println("================================");
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
                    System.out.println("Application closed.");
                }
                default ->
                        System.out.println("Invalid option.");
            }
        }
    }

    private void register() {

        System.out.println("\n--- REGISTRATION ---");

        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        System.out.println("\nSelect your role:");
        System.out.println("1. Administrator");
        System.out.println("2. Instructor");
        System.out.println("3. Student");
        System.out.print("Choose role: ");

        String role = switch (scanner.nextLine().trim()) {
            case "1" -> "ADMIN";
            case "2" -> "INSTRUCTOR";
            case "3" -> "STUDENT";
            default -> "";
        };

        if (role.isEmpty()) {
            System.out.println("Invalid role selection.");
            return;
        }

        boolean registered =
                userDao.register(username, password, role);

        if (registered) {
            userDao.logActivity(
                    null,
                    "REGISTER",
                    "Registered account: " + username
                            + ", Role: " + role
            );
        }
    }

    private void login() {

        System.out.println("\n--- LOGIN ---");

        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        User user = userDao.login(username, password);

        if (user == null) {
            return;
        }

        userDao.logActivity(
                user.id(),
                "LOGIN",
                "Successful login"
        );

        System.out.println(
                "\nWelcome, " + user.username() + "!"
        );
        System.out.println("Role: " + user.role());
        System.out.println("Status: " + user.status());

        if ("PENDING".equals(user.status())) {
            System.out.println(
                    "Your account is awaiting approval."
            );
        }

        routeToRoleMenu(user);
    }

    private void routeToRoleMenu(User user) {

        switch (user.role()) {
            case "ADMIN" ->
                    roleMenu(user, "ADMINISTRATOR MENU");

            case "INSTRUCTOR" ->
                    roleMenu(user, "INSTRUCTOR MENU");

            case "STUDENT" ->
                    roleMenu(user, "STUDENT MENU");

            default ->
                    System.out.println("Unknown user role.");
        }
    }

    private void roleMenu(User user, String title) {

        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n--- " + title + " ---");
            System.out.println(
                    "Signed in as: " + user.username()
            );
            System.out.println("1. View Account Information");
            System.out.println("2. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> {
                    System.out.println(
                            "Username: " + user.username()
                    );
                    System.out.println(
                            "Role: " + user.role()
                    );
                    System.out.println(
                            "Status: " + user.status()
                    );
                }

                case "2" -> {
                    loggedIn = false;
                    System.out.println("Logged out.");
                }

                default ->
                        System.out.println("Invalid option.");
            }
        }
    }
}
