
package com.scc;

import com.scc.dao.DatabaseConnection;
import com.scc.dao.UserDao;
import com.scc.ui.Menu;

import java.util.Scanner;

public 1class Main {

    public static void main(String[] args) {
        try {
            DatabaseConnection.initializeDatabase();

            Scanner scanner = new Scanner(System.in);
            UserDao userDao = new UserDao();
            Menu menu = new Menu(scanner, userDao);

            menu.start();

        } catch (RuntimeException e) {
            System.err.println(
                    "Application error: " + e.getMessage()
            );
        }
    }
}
