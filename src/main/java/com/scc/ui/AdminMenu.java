package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {
    private final Scanner scanner;
    private final UserDao userDao;

    public AdminMenu(Scanner scanner, UserDao userDao) {
        this.scanner = scanner;
        this.userDao = userDao;
    }

    /** Returns to Authentication when the administrator chooses logout. */
    public void start(User admin) {
        boolean running = true;
        while (running) {
            System.out.println("\n==========================================");
            System.out.println(" ADMIN MENU - USER ACCOUNT MANAGEMENT");
            System.out.println("==========================================");
            System.out.println("Signed in as: " + admin.username());
            System.out.println("1. View All System Users");
            System.out.println("2. View Pending Registrations");
            System.out.println("3. Approve User Account");
            System.out.println("4. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> displayUsers(userDao.getAllUsers(), "ALL SYSTEM USERS");
                case "2" -> displayUsers(userDao.getPendingUsers(), "PENDING REGISTRATIONS");
                case "3" -> approvePendingUser(admin);
                case "4" -> {
                    running = false;
                    System.out.println("Admin logged out.");
                }
                default -> System.out.println("Invalid choice. Please select 1-4.");
            }
        }
    }

    private void approvePendingUser(User admin) {
        List<User> pendingUsers = userDao.getPendingUsers();
        if (pendingUsers.isEmpty()) {
            System.out.println("There are no pending registrations to approve.");
            return;
        }

        displayUsers(pendingUsers, "PENDING REGISTRATIONS");
        System.out.println("Enter 0 at the User ID prompt to cancel.");
        System.out.print("Target User ID: ");
        String input = scanner.nextLine().trim();

        final int targetId;
        try {
            targetId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid User ID. Approval cancelled.");
            return;
        }

        if (targetId == 0) {
            System.out.println("Approval cancelled.");
            return;
        }
        if (targetId < 0) {
            System.out.println("User ID must be positive. Approval cancelled.");
            return;
        }

        boolean isPending = pendingUsers.stream().anyMatch(user -> user.id() == targetId);
        if (!isPending) {
            System.out.println("That ID is not in the pending registrations list.");
            return;
        }

        if (userDao.approveUser(targetId)) {
            System.out.println("SUCCESS: User ID " + targetId + " has been approved.");
            userDao.logActivity(admin.id(), "APPROVE_USER",
                    "Administrator " + admin.username() + " approved User ID " + targetId);
        } else {
            System.out.println("Approval failed. The account may no longer be pending.");
        }
    }


    private void displayUsers(List<User> users, String title) {
        System.out.println("\n--- " + title + " ---");
        String border = "+-------+-----------------+--------------+------------+";
        System.out.println(border);
        System.out.printf("| %-5s | %-15s | %-12s | %-10s |%n",
                "ID", "USERNAME", "ROLE", "STATUS");
        System.out.println(border);
        if (users.isEmpty()) {
            System.out.printf("| %-52s |%n", "No users found.");
        } else {
            for (User user : users) {
                System.out.printf("| %-5d | %-15s | %-12s | %-10s |%n",
                        user.id(), shorten(user.username(), 15),
                        shorten(user.role(), 12), shorten(user.status(), 10));
            }
        }
        System.out.println(border);
        System.out.println("Total accounts: " + users.size());
    }

    private String shorten(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
