
package com.scc;

import com.scc.dao.DatabaseInitializer;
import com.scc.dao.UserDao;
import com.scc.ui.Authentication;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try {
            // Step 1: Initialize the database
            DatabaseInitializer.initialize();

            // Step 2: Create shared objects
            Scanner scanner = new Scanner(System.in);
            UserDao userDao = new UserDao();

            // Step 3: Start authentication
            Authentication authentication =
                    new Authentication(scanner, userDao);

            authentication.start();

        } catch (RuntimeException e) {
            System.err.println(
                    "Application error: " + e.getMessage()
            );
        }
    }
}
