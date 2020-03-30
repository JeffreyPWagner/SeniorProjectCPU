package com.company;

import java.util.Scanner;

/**
 * The main class, which is responsible for collecting and responding to user inputs.
 */
public class Main {

    /**
     * Monitors user inputs and launches d-score calculator
     * @param args command line args
     */
    public static void main(String[] args) {
        try {

            // Listen for user input
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String input = scanner.nextLine();

                // Stop the server and terminate the program
                if ("stop".equals(input)) {
                    break;

                // Calculate d-scores for genes
                } else if ("dscore".equals(input)){
                    DScoreCalculator dScoreCalculator = new DScoreCalculator();
                    System.out.println("Please enter input file path:");
                    dScoreCalculator.calculateDScore(scanner.nextLine());

                // Alert user of an unknown command
                } else {
                    System.out.println("unknown command");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
