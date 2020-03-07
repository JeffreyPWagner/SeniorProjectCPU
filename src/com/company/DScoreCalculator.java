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

public class DScoreCalculator {

    private double[][] geneData;
    private List<String> geneLabels;
    private List<Integer> geneNumbers;
    private static Map<Integer, Double> dScores;
    private Mean mean;
    private StandardDeviation standardDeviation;

    private void loadData(String filePath) {
        try {
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

    private void writeData(Map<Integer, Double> dScores) {
        try {
            PrintWriter printWriter = new PrintWriter(new File("DScoreOutput.csv"));
            StringBuilder stringBuilder = new StringBuilder();

            for (Map.Entry<Integer, Double> dScore : dScores.entrySet()) {
                stringBuilder.append(geneLabels.get(dScore.getKey())).append(", ").append(dScore.getValue()).append('\n');
            }

            printWriter.write(stringBuilder.toString());
            printWriter.close();

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void processGene(Integer geneNumber) {
        double[] gene = geneData[geneNumber];
        double tStat = getTStat(gene);
        double[] permutationTStats = new double[1000];

        for(int i=0; i<1000; i++) {
            shuffleArray(gene);
            permutationTStats[i] = getTStat(gene);
        }

        double permutationMean = mean.evaluate(permutationTStats);
        double permutationStandardDeviation = standardDeviation.evaluate(permutationTStats);

        double dScore = Math.abs(tStat - permutationMean) / permutationStandardDeviation;
        dScores.putIfAbsent(geneNumber, dScore);
    }

    private double getTStat(double[] gene) {
        double[] first = Arrays.copyOfRange(gene, 0, 8);
        double firstMean = mean.evaluate(first);
        double firstStandardDeviation = standardDeviation.evaluate(first);

        double[] last = Arrays.copyOfRange(gene, 8, gene.length);
        double lastMean = mean.evaluate(last);
        double lastStandardDeviation = standardDeviation.evaluate(last);

        return (firstMean - lastMean) / Math.sqrt((Math.pow(firstStandardDeviation, 2) / 8) + Math.pow(lastStandardDeviation, 2) / 52);
    }

    // helper method to shuffle permutations
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

    public void calculateDScore (String filePath) {
        mean = new Mean();
        standardDeviation = new StandardDeviation();
        loadData(filePath);
        Random random = new Random();
        geneNumbers = Collections.synchronizedList(new ArrayList<>());
        for (int i=0; i<geneData.length; i++) {
            geneNumbers.add(i);
        }

        System.out.println("Processing genes");

        ExecutorService threadPool = Executors.newFixedThreadPool(4);

        for (Integer geneNumber: geneNumbers)  {
            threadPool.execute(() -> {
                processGene(geneNumber);
            });
        }
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Writing D-Score results to file");

        writeData(dScores);

        System.out.println("D-Score calculation complete");
    }

    public Map<Integer, Double> getDScores() {
        return dScores;
    }

    public List<Integer> getGeneNumbers() {
        return geneNumbers;
    }
}
