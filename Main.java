/* Author Name : Sayani Raut Dessai
Roll no. : 2647
Description : A two-player Chess game with a Swing GUI, implemented using OOP concepts in Java. It supports all the standard rules and features of chess */

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// Entry point class: launches the Chess window on the Swing event thread
public class Main {

    public static void main(String[] args) {
        try {
            // Use the look and feel of the user's operating system
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        // Swing components must be created on the event dispatch thread
        SwingUtilities.invokeLater(() -> {
            Chess game = new Chess(); // Object creation
            game.setVisible(true);
        });
    }
}
