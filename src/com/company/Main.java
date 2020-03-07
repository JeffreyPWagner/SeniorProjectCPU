package com.company;

import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String input = scanner.nextLine();
                if ("stop".equals(input)) {
                    break;
                } else if ("dscore".equals(input)){
                    DScoreCalculator dScoreCalculator = new DScoreCalculator();
                    System.out.println("Please enter input file path:");
                    dScoreCalculator.calculateDScore(scanner.nextLine());
                } else {
                    System.out.println("unknown command");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
