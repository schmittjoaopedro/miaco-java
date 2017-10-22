package schmitt.lab;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

public class Statistics {

    private static String performanceFile;

    private static String diversityFile;

    private static String varFile;

    private static String directoryFile;

    public static void openStats() {
        ProgramData.performance = new double[ProgramData.maxTrials][ProgramData.maxIterations];
        ProgramData.diversity = new double[ProgramData.maxTrials][ProgramData.maxIterations];

        Statistics.directoryFile = (new File("output")).getAbsolutePath();
        Statistics.performanceFile = String.format("%s/PerformanceAlg_M_%s_D_%.2f_S_%d_%s.txt",
                Statistics.directoryFile, ProgramData.changeMode.name(), ProgramData.changeDegree, ProgramData.changeSpeed, ProgramData.problemInstance);
        Statistics.diversityFile = String.format("%s/DiversityAlg_M_%s_D_%.2f_S_%d_%s.txt",
                Statistics.directoryFile, ProgramData.changeMode.name(), ProgramData.changeDegree, ProgramData.changeSpeed, ProgramData.problemInstance);
        Statistics.varFile = String.format("%s/VaryingAlg_M_%s_S_Rand_D_Rand_%s.txt",
                Statistics.directoryFile, ProgramData.changeMode.name(), ProgramData.problemInstance);
    }

    public static void appendLine(String fileName, String data) {
        try {
            Writer output = new BufferedWriter(new FileWriter(fileName, true));
            output.append(data);
            output.close();
        } catch (Exception er) {
            er.printStackTrace();
            System.exit(0);
        }
    }

    public static void statisticsAndOutput(int r, int t) {
        ProgramData.performance[r - 1][t - 1] = ProgramData.bestSoFarAnt.tourLength;
        ProgramData.diversity[r - 1][t - 1] = calcDiversity(ProgramData.nAnts);
        if (ProgramData.changeMode == ChangeMode.Varying && r == 1) {
            appendLine(varFile, String.format("%.2f\n", ProgramData.changeDegree));
        }
        //System.out.println("iteration: " + ProgramData.currentIteration + " best_so_far " + ProgramData.bestSoFarAnt.tourLength + " diversity " + ProgramData.diversity[r - 1][t - 1]);
        //System.out.println("" + ProgramData.currentIteration + ", " + ProgramData.bestSoFarAnt.tourLength + ", " + ProgramData.diversity[r - 1][t - 1]);
    }

    public static double calcDiversity(int size) {
        double div = 0.0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i != j) {
                    div += ACO.distanceBetween(ProgramData.antPopulation[i], ProgramData.antPopulation[j]); //common edges
                }
            }
        }
        return (1.0 / (size * (size - 1))) * div;
    }

    public static void closeStats() {
        double perfMeanValue, perfStdevValue;
        double divMeanValue, divStdevValue;
        double[] perfOfTrials = new double[ProgramData.maxTrials];
        double[] perfOfIterations = new double[ProgramData.maxIterations];
        double[] divOfTrials = new double[ProgramData.maxTrials];
        double[] divOfIterations = new double[ProgramData.maxIterations];
        //Initialize
        for (int i = 0; i < ProgramData.maxIterations; i++) {
            perfOfIterations[i] = 0.0;
            divOfIterations[i] = 0.0;
        }
        for (int i = 0; i < ProgramData.maxTrials; i++) {
            perfOfTrials[i] = 0.0;
            divOfTrials[i] = 0.0;
        }
        //For graph plots
        System.out.println("===================================\ntime,bsf,div");
        for (int i = 0; i < ProgramData.maxIterations; i++) {
            for (int j = 0; j < ProgramData.maxTrials; j++) {
                perfOfIterations[i] += ProgramData.performance[j][i];
                divOfIterations[i] += ProgramData.diversity[j][i];
            }
            perfOfIterations[i] /= ((double) ProgramData.maxTrials);
            divOfIterations[i] /= ((double) ProgramData.maxTrials);
            appendLine(performanceFile, String.format("%.2f\n", perfOfIterations[i]));
            appendLine(diversityFile, String.format("%.2f\n", divOfIterations[i]));
            System.out.println(i + "," + perfOfIterations[i] + "," + divOfIterations[i]);
        }
        System.out.println("===================================");
        appendLine(performanceFile, "\n");
        appendLine(diversityFile, "\n");
        appendLine(performanceFile, "Statistical results\n");
        appendLine(diversityFile, "Statistical results\n");
        //For statistics
        for (int i = 0; i < ProgramData.maxTrials; i++) {
            for (int j = 0; j < ProgramData.maxIterations; j++) {
                perfOfTrials[i] += ProgramData.performance[i][j];
                divOfTrials[i] += ProgramData.diversity[i][j];
            }
            perfOfTrials[i] /= ((double) ProgramData.maxIterations);
            divOfTrials[i] /= ((double) ProgramData.maxIterations);
            appendLine(performanceFile, String.format("%.2f\n", perfOfTrials[i]));
            appendLine(diversityFile, String.format("%.2f\n", divOfTrials[i]));
        }
        perfMeanValue = mean(perfOfTrials, ProgramData.maxTrials);
        perfStdevValue = stdev(perfOfTrials, ProgramData.maxTrials, perfMeanValue);
        divMeanValue = mean(divOfTrials, ProgramData.maxTrials);
        divStdevValue = stdev(divOfTrials, ProgramData.maxTrials, divMeanValue);
        appendLine(performanceFile, String.format("Mean %f\t ", perfMeanValue));
        appendLine(performanceFile, String.format("\tStd Dev %f\t ", perfStdevValue));
        appendLine(diversityFile, String.format("Mean %f\t ", divMeanValue));
        appendLine(diversityFile, String.format("\tStd Dev %f ", divStdevValue));
        System.out.println("Mean " + perfMeanValue + " Std " + perfStdevValue);
    }

    public static double mean(double[] values, int size) {
        int i;
        double m = 0.0;
        for (i = 0; i < size; i++) {
            m += values[i];
        }
        m = m / (double) size;
        return m; //mean
    }

    public static double stdev(double[] values, int size, double average) {
        int i;
        double dev = 0.0;

        if (size <= 1)
            return 0.0;
        for (i = 0; i < size; i++) {
            dev += (values[i] - average) * (values[i] - average);
        }
        return Math.sqrt(dev / (double) (size - 1)); //standard deviation
    }

}
