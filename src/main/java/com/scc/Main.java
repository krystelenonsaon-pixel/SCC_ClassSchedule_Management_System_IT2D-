package com.scc;

import com.scc.dao.DatabaseInitializer;
import com.scc.dao.UserDao;
import com.scc.ui.Authentication;

import java.util.Scanner;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try {
            // The single application entry point always initializes the database first.
            DatabaseInitializer.initialize();

            try (Scanner scanner = new Scanner(System.in)) {
                Authentication authentication =
                        new Authentication(scanner, new UserDao());
                authentication.start();
            }
        } catch (RuntimeException e) {
            System.err.println("Application error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
