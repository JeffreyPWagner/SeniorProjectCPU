package com.company;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The DScoreCalculator represents the dedicated gene processing functionality of the server. It takes in input data,
 * processes it using multiple threads, and outputs the results.
 */
public class DScoreCalculator {

    // All gene data to be processed
    private double[][] geneData;

    // The gene identifiers
    private List<String> geneLabels;

    // Numerical identifiers for the genes to assist in processing
    private List<Integer> geneNumbers;

    // The list of gene scores that will become the final output
    private static Map<Integer, Double> dScores;

    // Apache Commons Math object used to calculate means
    private Mean mean;

    // Apache Commons Math object used to calculate standard deviations
    private StandardDeviation standardDeviation;

    /**
     * Helper method to load the gene data from a CSV file
     * @param filePath the path to the CSV file
     */
    private void loadData(String filePath) {
        try {
            // Instantiate objects to hold gene data
            dScores = Collections.synchronizedMap(new TreeMap<>());
            geneLabels = new ArrayList<>();
            List<List<String>> fileData = new ArrayList<>();

            // read in the raw file data
            String row;
            BufferedReader csvReader = new BufferedReader(new FileReader(filePath));
            while ((row = csvReader.readLine()) != null) {
                fileData.add(new ArrayList<>(Arrays.asList(row.split(","))));
            }
            csvReader.close();

            // replace missing genes at the end of rows with 0
            for (int i = 0; i < fileData.size(); i++) {
                while (fileData.get(i).size() < fileData.get(0).size()) {
                    fileData.get(i).add("0");
                }
            }

            // create geneData array now that we know the size of the input
            geneData = new double[fileData.size() - 1][fileData.get(0).size() - 1];

            // convert the gene data to floats and load into geneData, replace missing with 0
            for (int j = 1; j < fileData.size(); j++) {
                geneLabels.add(fileData.get(j).get(0));
                for (int k = 1; k < fileData.get(0).size(); k++) {
                    if (!fileData.get(j).get(k).isEmpty()) {
                        geneData[j - 1][k - 1] = Double.parseDouble(fileData.get(j).get(k));
                    } else {
                        geneData[j - 1][k - 1] = 0.0;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to write the final scores to an output CSV file
     * @param dScores the complete score map
     */
    private void writeData(Map<Integer, Double> dScores) {
        try {

            // Create new stringbuilder and printwriter to assemble the result CSV
            PrintWriter printWriter = new PrintWriter(new File("DScoreOutput.csv"));
            StringBuilder stringBuilder = new StringBuilder();

            // Write the scores and corresponding genes to the stringbuilder, one per line
            for (Map.Entry<Integer, Double> dScore : dScores.entrySet()) {
                stringBuilder.append(geneLabels.get(dScore.getKey())).append(", ").append(dScore.getValue()).append('\n');
            }

            // Write the contents of the stringbuilder to file
            printWriter.write(stringBuilder.toString());
            printWriter.close();

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculates the d-score of the gene associated with the given number
     * @param geneNumber the numerical identifier of the gene to process
     */
    private void processGene(Integer geneNumber) {

        // The gene data
        double[] gene = geneData[geneNumber];

        // The t-stat of the original data
        double tStat = getTStat(gene);

        // The t-stats of the data shuffle permutations
        double[] permutationTStats = new double[10000];

        // Shuffle the data and record the t-stat of the newly ordered data x times
        for(int i=0; i<10000; i++) {
            shuffleArray(gene);
            permutationTStats[i] = getTStat(gene);
        }

        // Calculate the gene's d-score and record it to the output map
        double permutationMean = mean.evaluate(permutationTStats);
        double permutationStandardDeviation = standardDeviation.evaluate(permutationTStats);
        double dScore = Math.abs(tStat - permutationMean) / permutationStandardDeviation;
        dScores.putIfAbsent(geneNumber, dScore);
    }

    /**
     * Helper method to calculate the t-stat of the given gene
     * @param gene the gene to calculate the t-stat for
     * @return the gene's t-stat
     */
    private double getTStat(double[] gene) {

        // Record statistics of the first 8 patients, who have renal cancer in the non-shuffled data
        double[] first = Arrays.copyOfRange(gene, 0, 8);
        double firstMean = mean.evaluate(first);
        double firstStandardDeviation = standardDeviation.evaluate(first);

        // Record statistics of the last 52 patients, who do not have renal cancer in the non-shuffled data
        double[] last = Arrays.copyOfRange(gene, 8, gene.length);
        double lastMean = mean.evaluate(last);
        double lastStandardDeviation = standardDeviation.evaluate(last);

        // Calculate and return the t-stat
        return (firstMean - lastMean) / Math.sqrt((Math.pow(firstStandardDeviation, 2) / 8) + Math.pow(lastStandardDeviation, 2) / 52);
    }

    /**
     * Helper method to shuffle the gene data.
     * @param array the array to shuffle
     */
    private void shuffleArray(double[] array) {
        int index;
        double temp;
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            index = random.nextInt(i + 1);
            temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }

    /**
     * Calculates d-scores for the genes contained in an input file.
     * @param filePath path to the input file
     */
    public void calculateDScore (String filePath) {
        // Create statistics objects
        mean = new Mean();
        standardDeviation = new StandardDeviation();

        // Load the input data
        loadData(filePath);

        // Generate the list of numerical gene identifiers to assist in processing
        geneNumbers = Collections.synchronizedList(new ArrayList<>());
        for (int i=0; i<geneData.length; i++) {
            geneNumbers.add(i);
        }

        // Begin processing and timing
        System.out.println("Processing genes");
        long startTime = System.currentTimeMillis();

        // Create a thread pool to simulate x number of client phones
        ExecutorService threadPool = Executors.newFixedThreadPool(8);

        // Iterate through the genes and calculate scores whenever a thread from the pool is available
        for (Integer geneNumber : geneNumbers) {
            threadPool.execute(() -> {
                processGene(geneNumber);
            });
        }
        threadPool.shutdown();

        try {

            // Wait for all threads to finish processing
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Once all genes have been scored, output the elapsed wall clock processing time
        System.out.println("Calculation complete, elapsed time " + (System.currentTimeMillis() - startTime) + " milliseconds");
        System.out.println("Writing D-Score results to file");

        // Write results to file
        writeData(dScores);
        System.out.println("D-Score calculation complete");
    }
}
