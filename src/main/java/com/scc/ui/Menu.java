
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
                    System.out.println("Application closed.");
                }
                default -> System.out.println(
                        "Invalid option. Please try again."
                );
            }
        }
    }

    private void register() {
        System.out.println("\n--- REGISTER ACCOUNT ---");

        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        System.out.println("Select role:");
        System.out.println("1. TEACHER");
        System.out.println("2. STUDENT");
        System.out.print("Choose a role: ");

        String roleChoice = scanner.nextLine().trim();

        String role;

        switch (roleChoice) {
            case "1" -> role = "TEACHER";
            case "2" -> role = "STUDENT";
            default -> {
                System.out.println("Invalid role selection.");
                return;
            }
        }

        userDao.register(username, password, role);
    }

    private void login() {
        System.out.println("\n--- LOGIN ---");

        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        User user = userDao.login(username, password);

        if (user == null) {
            System.out.println("Invalid username or password.");
            return;
        }

        System.out.println("\nLogin successful!");
        System.out.println("Welcome, " + user.username() + "!");
        System.out.println("Role: " + user.roles());
        System.out.println("Account status: " + user.status());

        if (user.status().equals("PENDING")) {
            System.out.println(
                    "Your account is awaiting approval."
            );
        }

        userMenu(user);
    }

    private void userMenu(User user) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n--- USER MENU ---");
            System.out.println("Logged in as: " + user.username());
            System.out.println("1. View Account Information");
            System.out.println("2. Logout");
            System.out.println("3. Exit Application");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    System.out.println("Username: " + user.username());
                    System.out.println("Role: " + user.roles());
                    System.out.println("Status: " + user.status());
                }
                case "2" -> {
                    loggedIn = false;
                    System.out.println("Logout successful.");
                }
                case "3" -> {
                    System.out.println("Application closed.");
                    System.exit(0);
                }
                default -> System.out.println(
                        "Invalid option. Please try again."
                );
            }
        }
    }
}
