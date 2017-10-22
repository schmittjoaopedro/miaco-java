package schmitt.lab;

public class ProgramData {

    public static String problemInstance;

    public static ChangeMode changeMode;

    public static double changeDegree;

    public static int changeSpeed;

    public static int maxIterations;

    public static int maxTrials;

    public static int problemSize;

    public static int problemSizeNoDepot;

    public static int maxCapacity;

    public static Vertex[] maskedObjects;

    public static Vertex[] initObjects;

    public static int[] maskedDemand;

    public static int[] initDemand;

    public static int depot;

    public static double[][] distances;

    public static double[][] performance;

    public static double[][] diversity;

    public static double alpha;

    public static double beta;

    public static double q_0;

    public static double trail0;

    public static int nAnts;

    public static int depth;

    public static int shortMemorySize;

    public static double immigrantRate;

    public static double pMi;

    public static int longMemorySize;

    public static Ant[] antPopulation;

    public static Ant bestSoFarAnt;

    public static Ant[] previousBest;

    public static Ant previousBestSoFarAnt;

    public static double[][] pheromone;

    public static double[][] heuristic;

    public static double[][] total;

    public static int[][] nnList;

    public static Ant[] shortMemory;

    public static Ant[] longMemory;

    public static boolean[] randomPoint;

    public static int[] randomVector;

    public static int[] reRandomVector;

    public static int cyclicBaseCount;

    public static int cyclicRandom;

    public static int[][] cyclicRandomVector;

    public static int[][] cyclicReRandomVector;

    public static int cyclicStates = 4;

    public static boolean noiseFlag = false;

    public static double pNoise = 0.05;

    public static int[] noise;

    public static int[] reNoise;

    public static int currentIteration;

    public static int tM;

    public static void checkInputParameters() {
        if(ProgramData.problemSize == 0) {
            System.out.println("wrong problem instance file");
            System.exit(0);
        }
        if(ProgramData.changeMode == null) {
            System.out.println("select a valid change mode (between 1..4)");
            System.exit(0);
        }
        if(ProgramData.changeDegree > 1.0 || ProgramData.changeDegree <= 0.0) {
            System.out.println("select a valid change degree (between 0..1)");
            System.exit(0);
        }
        if(ProgramData.changeSpeed <= 0) {
            System.out.println("select a valid change speed (int)");
            System.exit(0);
        }
    }

}
