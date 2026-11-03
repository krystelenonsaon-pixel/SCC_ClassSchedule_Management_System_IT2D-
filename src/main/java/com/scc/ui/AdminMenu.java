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
            System.out.println("4. Update User Role and Status");
            System.out.println("5. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> displayUsers(userDao.getAllUsers(), "ALL SYSTEM USERS");
                case "2" -> displayUsers(userDao.getPendingUsers(), "PENDING REGISTRATIONS");
                case "3" -> approvePendingUser(admin);
                case "4" -> updateUserRoleAndStatus(admin);
                case "5" -> {
                    running = false;
                    System.out.println("Admin logged out.");
                }
                default -> System.out.println("Invalid choice. Please select 1-5.");
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


    private void updateUserRoleAndStatus(User admin) {
        List<User> users = userDao.getAllUsers();
        displayUsers(users, "ALL SYSTEM USERS");
        System.out.println("Enter 0 at any prompt to cancel.");

        int targetId = readIntInput("Target User ID: ");
        if (targetId == 0) {
            System.out.println("Update cancelled.");
            return;
        }
        if (targetId == admin.id()) {
            System.out.println("WARNING: Modifying the currently active admin account is restricted.");
            return;
        }

        boolean exists = users.stream().anyMatch(user -> user.id() == targetId);
        if (!exists) {
            System.out.println("No user found with ID " + targetId + ".");
            return;
        }

        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println("Update cancelled.");
            return;
        }

        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("Update cancelled.");
            return;
        }

        if (userDao.updateUserRoleAndStatus(targetId, newRole, newStatus)) {
            System.out.println("SUCCESS: User ID " + targetId + " updated to role "
                    + newRole + " and status " + newStatus + ".");
            userDao.logActivity(admin.id(), "UPDATE_USER",
                    "Administrator " + admin.username() + " set User ID " + targetId
                            + " to " + newRole + " / " + newStatus);
        } else {
            System.out.println("Update failed. No changes were made.");
        }
    }

    /** Re-prompts until the input is a whole number, so letters never crash the app. */
    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    /** Returns STUDENT, INSTRUCTOR or ADMIN (TEACHER is accepted), or null if cancelled with 0. */
    private String readValidRole() {
        while (true) {
            System.out.print("New role (STUDENT / TEACHER / ADMIN, 0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();
            switch (input) {
                case "0":
                    return null;
                case "STUDENT":
                case "ADMIN":
                    return input;
                case "TEACHER":
                case "INSTRUCTOR":
                    return "INSTRUCTOR";
                default:
                    System.out.println("Invalid role. Choose STUDENT, TEACHER or ADMIN.");
            }
        }
    }

    /** Returns APPROVED or PENDING, or null if cancelled with 0. */
    private String readValidStatus() {
        while (true) {
            System.out.print("New status (APPROVED / PENDING, 0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();
            switch (input) {
                case "0":
                    return null;
                case "APPROVED":
                case "PENDING":
                    return input;
                default:
                    System.out.println("Invalid status. Choose APPROVED or PENDING.");
            }
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
