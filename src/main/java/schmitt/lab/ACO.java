package schmitt.lab;

import java.util.Arrays;
import java.util.Comparator;

public class ACO {

    public static void setAlgorithmParameters() {
        ProgramData.alpha = 1.0;
        ProgramData.beta = 5.0;
        ProgramData.q_0 = 0.0;
        ProgramData.nAnts = 50;
        ProgramData.depth = 20;
        ProgramData.shortMemorySize = 6;
        ProgramData.immigrantRate = 0.4;
        ProgramData.pMi = 0.01;
        ProgramData.longMemorySize = 3;
    }

    public static void allocateAnts() {
        ProgramData.antPopulation = new Ant[ProgramData.nAnts];

        for(int i = 0; i < ProgramData.nAnts; i++) {
            ProgramData.antPopulation[i] = new Ant();
            ProgramData.antPopulation[i].tour = new int[ProgramData.problemSizeNoDepot];
            ProgramData.antPopulation[i].visited  = new boolean[ProgramData.problemSize];
            ProgramData.antPopulation[i].routes = new int[ProgramData.problemSizeNoDepot];
        }

        ProgramData.bestSoFarAnt = new Ant();
        ProgramData.bestSoFarAnt.tour = new int[ProgramData.problemSizeNoDepot];
        ProgramData.bestSoFarAnt.visited  = new boolean[ProgramData.problemSize];
        ProgramData.bestSoFarAnt.routes = new int[ProgramData.problemSizeNoDepot];

        ProgramData.previousBest = new Ant[2];
        for (int i = 0; i < 2; i++) {
            ProgramData.previousBest[i] = new Ant();
            ProgramData.previousBest[i].tour = new int[ProgramData.problemSizeNoDepot];
            ProgramData.previousBest[i].visited = new boolean[ProgramData.problemSize];
            ProgramData.previousBest[i].routes = new int[ProgramData.problemSizeNoDepot];
        }

        ProgramData.previousBestSoFarAnt = new Ant();
        ProgramData.previousBestSoFarAnt.tour = new int[ProgramData.problemSizeNoDepot];
        ProgramData.previousBestSoFarAnt.visited = new boolean[ProgramData.problemSize];
        ProgramData.previousBestSoFarAnt.routes = new int[ProgramData.problemSizeNoDepot];
    }

    public static void allocateStrutures() {
        ProgramData.pheromone = new double[ProgramData.problemSize][ProgramData.problemSize];
        ProgramData.heuristic = new double[ProgramData.problemSize][ProgramData.problemSize];
        ProgramData.total = new double[ProgramData.problemSize][ProgramData.problemSize];
        ProgramData.nnList = new int[ProgramData.problemSize][ProgramData.depth];
        ProgramData.shortMemory = new Ant[ProgramData.shortMemorySize];
        ProgramData.longMemory = new Ant[ProgramData.longMemorySize];
        ProgramData.randomPoint = new boolean[ProgramData.longMemorySize];
        for(int i = 0; i < ProgramData.shortMemorySize; i++) {
            ProgramData.shortMemory[i] = new Ant();
        }
        for(int i = 0; i < ProgramData.longMemorySize; i++) {
            ProgramData.longMemory[i] = new Ant();
        }
    }

    public static void initTry(int t) {
        ACO.computeNNList();
        ACO.initHeuristicInfo();
        ProgramData.trail0 = 1.0 / nnTour();
        initPheromoneTrails(ProgramData.trail0);
        computeTotalInfo();
        ProgramData.bestSoFarAnt.tourLength = Double.MAX_VALUE;
        ProgramData.previousBestSoFarAnt.tourLength = Double.MAX_VALUE;
        initMemoryRandomly();
    }

    public static void computeNNList() {
        final Double distanceVector[] = new Double[ProgramData.problemSize];
        Integer helpVector[] = new Integer[ProgramData.problemSize];
        for(int j = 0; j < ProgramData.problemSize; j++) {
            for(int i = 0; i < ProgramData.problemSize; i++) {
                distanceVector[i] = ProgramData.distances[j][i];
                helpVector[i] = i;
            }
            distanceVector[j] = Double.MAX_VALUE;
            Arrays.sort(helpVector, new Comparator<Integer>() {
                public int compare(final Integer o1, final Integer o2) {
                    return Double.compare(distanceVector[o1], distanceVector[o2]);
                }
            });
            for(int i = 0; i < ProgramData.depth; i++) {
                ProgramData.nnList[j][i] = helpVector[i];
            }
        }
    }

    public static void initHeuristicInfo() {
        for(int i = 0; i < ProgramData.problemSize; i++) {
            for(int j = 0; j <= i; j++) {
                ProgramData.heuristic[i][j] = 1.0 / (double) (ProgramData.distances[i][j] + Double.MIN_VALUE);
                ProgramData.heuristic[j][i] = ProgramData.heuristic[i][j];
            }
        }
    }

    public static double nnTour() {
        Ant ant = ProgramData.antPopulation[0];
        ant.antEmptyMemory();
        ant.placeAnt();
        while(ant.noCustomers < ProgramData.problemSizeNoDepot) {
            ant.chooseBestNext();
        }
        ant.fitnessEvaluation();
        double help = ant.tourLength;
        ant.antEmptyMemory();
        return help;
    }

    public static void initPheromoneTrails(double trail) {
        for (int i = 0; i < ProgramData.problemSize; i++) {
            for (int j = 0; j <= i; j++) {
                ProgramData.pheromone[i][j] = trail;
                ProgramData.pheromone[j][i] = trail;
            }
        }
    }

    public static void computeTotalInfo() {
        for (int i = 0; i < ProgramData.problemSize; i++) {
            for (int j = 0; j <= i; j++) {
                ProgramData.total[i][j] = Math.pow(ProgramData.pheromone[i][j], ProgramData.alpha) * Math.pow(ProgramData.heuristic[i][j], ProgramData.beta);
                ProgramData.total[j][i] = ProgramData.total[i][j];
            }
        }
    }

    public static void initMemoryRandomly() {
        for(int i = 0; i < ProgramData.longMemorySize; i++) {
            ProgramData.longMemory[i] = new Ant();
            ProgramData.longMemory[i].generateRandomImmigrant();
            ProgramData.longMemory[i].randomR();
            ProgramData.longMemory[i].fitnessEvaluation();
            ProgramData.randomPoint[i] = true;
        }
        ProgramData.tM = 5 + ((int) (Math.random() * 6.0));
    }

    public static void constructSolutions() {
        int allAnts;
        for (int k = 0; k < ProgramData.nAnts; k++) {
            ProgramData.antPopulation[k].antEmptyMemory();
        }
        //place ants on the depot
        for (int k = 0; k < ProgramData.nAnts; k++) {
            ProgramData.antPopulation[k].placeAnt();
        }
        allAnts = 0;
        //select object until all objects are visited
        while (allAnts < ProgramData.nAnts) {
            for (int k = 0; k < ProgramData.nAnts; k++) {
                if (ProgramData.antPopulation[k].noCustomers < ProgramData.problemSizeNoDepot)
                    ProgramData.antPopulation[k].neighbourChooseAndMoveToNext();
            else
                allAnts++; //until all ants satisfied all customer demands
            }
        }
        for (int k = 0; k < ProgramData.nAnts; k++) {
            ProgramData.antPopulation[k].fitnessEvaluation();
        }
    }

    public static void updateBest() {
        int iterationBest = findBest();

        if (ProgramData.antPopulation[iterationBest].tourLength < ProgramData.bestSoFarAnt.tourLength) {
            copyFromTo(ProgramData.antPopulation[iterationBest], ProgramData.bestSoFarAnt);
        }
        copyFromTo(ProgramData.bestSoFarAnt, ProgramData.previousBestSoFarAnt);
        copyFromTo(ProgramData.previousBest[1], ProgramData.previousBest[0]);
        copyFromTo(ProgramData.previousBestSoFarAnt, ProgramData.previousBest[1]);
        copyFromTo(ProgramData.previousBest[0], ProgramData.previousBestSoFarAnt);
        if (ProgramData.currentIteration == 1) copyFromTo(ProgramData.bestSoFarAnt, ProgramData.previousBestSoFarAnt);
    }

    public static int findBest() {
        int kMin = 0;
        double min = ProgramData.antPopulation[0].tourLength;
        for (int k = 1; k < ProgramData.nAnts; k++) {
            if (ProgramData.antPopulation[k].tourLength < min) {
                min = ProgramData.antPopulation[k].tourLength;
                kMin = k;
            }
        }
        return kMin;
    }

    public static void copyFromTo(Ant a1, Ant a2) {
        a2.tourLength = a1.tourLength;
        a2.dummyDepots = a1.dummyDepots;
        for (int i = 0; i < ProgramData.problemSizeNoDepot; i++) {
            a2.tour[i] = a1.tour[i];
            a2.routes[i] = a1.routes[i];
        }
    }

    public static void pheromoneUpdate() {
        updateLongTermMemory();
        updateShortTermMemory();
        generatePheromoneMatrix();
        computeTotalInfo();
    }

    public static void updateLongTermMemory() {
        int rnd;
        boolean flag = detectChange();
        if (flag == true) {
            updateMemoryEveryChange();
        }
        if (ProgramData.currentIteration == ProgramData.tM && flag == false) {
            updateMemoryDynamically();
            rnd = 5 + ((int) (Math.random() * 6.0)); //random number [5..10]
            ProgramData.tM = ProgramData.currentIteration + rnd;
        }
        //if both cases are true give priority to
        //update_memory_every_change(), but update the
        //time for the next update of update_memory_dynamically();
        if (ProgramData.currentIteration == ProgramData.tM && flag == true) {
            rnd = 5 + ((int) (Math.random() * 6.0)); //random number [5..10]
            ProgramData.tM = ProgramData.currentIteration + rnd;
        }
    }

    public static void updateShortTermMemory() {
        final double[] tours = new double[ProgramData.nAnts];
        Integer[] id = new Integer[ProgramData.nAnts];
        //number of immigrants
        int imSize = (int) (ProgramData.shortMemorySize * ProgramData.immigrantRate);
        Ant[] immigrants = new Ant[imSize];
        for (int i = 0; i < imSize; i++) {
            immigrants[i] = new Ant();
            immigrants[i].generateMemoryBasedImmigrant();
            immigrants[i].guidedR();
            immigrants[i].fitnessEvaluation();
        }
        //add new generation of the best ants to short memory
        for (int i = 0; i < ProgramData.nAnts; i++) {
            tours[i] = ProgramData.antPopulation[i].tourLength;
            id[i] = i;
        }
        Arrays.sort(id, new Comparator<Integer>() {
            public int compare(final Integer o1, final Integer o2) {
                return Double.compare(tours[o1], tours[o2]);
            }
        });
        for (int i = 0; i < ProgramData.shortMemorySize; i++) {
            ProgramData.shortMemory[i] = ProgramData.antPopulation[id[i]];
        }
        //replace immigrants with the worst ants
        for (int i = ProgramData.shortMemorySize - 1; i > ProgramData.shortMemorySize - imSize - 1; i--) {
            copyFromTo(immigrants[ProgramData.shortMemorySize - 1 - i], ProgramData.shortMemory[i]);
        }
    }

    public static void generatePheromoneMatrix() {
        double deltaT; //amount of pheromone
        deltaT = (1.0 - ProgramData.trail0) / (double) ProgramData.shortMemorySize;
        initPheromoneTrails(ProgramData.trail0);
        for (int i = 0; i < ProgramData.shortMemorySize; i++) {
            //update pheromone using the ants stored in short memory
            constantPheromoneDeposit(ProgramData.shortMemory[i], deltaT);
        }
    }

    public static void constantPheromoneDeposit(Ant a, double deltaT) {
        int j, h;
        ProgramData.pheromone[0][a.tour[0]] += deltaT;
        ProgramData.pheromone[a.tour[0]][0] = ProgramData.pheromone[0][a.tour[0]];
        for (int i = 0; i < ProgramData.problemSizeNoDepot - 1; i++) {
            j = a.tour[i];
            h = a.tour[i + 1];
            if (a.routes[i] == a.routes[i + 1]) {
                ProgramData.pheromone[j][h] += deltaT;
                ProgramData.pheromone[h][j] = ProgramData.pheromone[j][h];
            } else {
                ProgramData.pheromone[j][0] += deltaT;
                ProgramData.pheromone[0][j] = ProgramData.pheromone[j][0];
                ProgramData.pheromone[0][h] += deltaT;
                ProgramData.pheromone[h][0] = ProgramData.pheromone[0][h];
            }
        }
        ProgramData.pheromone[a.tour[ProgramData.problemSizeNoDepot - 1]][0] += deltaT;
        ProgramData.pheromone[0][a.tour[ProgramData.problemSizeNoDepot - 1]] = ProgramData.pheromone[a.tour[ProgramData.problemSizeNoDepot - 1]][0];
    }

    public static boolean detectChange() {
        double totalBefore = 0.0;
        double totalAfter = 0.0;
        boolean flag;

        //calculate total cost before dynamic change
        for (int i = 0; i < ProgramData.longMemorySize; i++) {
            totalBefore += ProgramData.longMemory[i].tourLength;
        }
        //calculate total cost after dynamic change
        for (int i = 0; i < ProgramData.longMemorySize; i++) {
            ProgramData.longMemory[i].fitnessEvaluation();
            totalAfter += ProgramData.longMemory[i].tourLength;
        }
        //is different then a dynamic change occured
        if (totalAfter == totalBefore)
            flag = false;
        else
            flag = true;

        return flag;
    }

    public static void updateMemoryEveryChange() {
        int index = -1;
        int closestInd = -1;
        double d, closest = Double.MAX_VALUE;

        //if random point still exist, i.e., if index = -1; then replace
        for (int i = 0; i < ProgramData.longMemorySize; i++) {
            if (ProgramData.randomPoint[i] == true) {
                index = i;
                ProgramData.randomPoint[i] = false;
                break;
            }
        }
        //re-evaluate/repair the best ant from the previous environment
        ProgramData.previousBestSoFarAnt.fitnessEvaluation();
        if (index != -1) {
            copyFromTo(ProgramData.previousBestSoFarAnt, ProgramData.longMemory[index]);
        } else {
            for (int i = 0; i < ProgramData.longMemorySize; i++) {
                //find the closest ant of the memory with the previous best ant
                d = distanceBetween(ProgramData.previousBestSoFarAnt, ProgramData.longMemory[i]);
                if (closest > d) {
                    closest = d;
                    closestInd = i;
                }
            }
            //replace
            if (ProgramData.previousBestSoFarAnt.tourLength < ProgramData.longMemory[closestInd].tourLength) {
                copyFromTo(ProgramData.previousBestSoFarAnt, ProgramData.longMemory[closestInd]);
            }
        }
    }

    public static double distanceBetween(Ant a1, Ant a2) {
        int j, h, pos;
        double distance;
        int[] pos2 = new int[ProgramData.problemSize];
        //indexes of cities of ant2
        for (int i = 0; i < ProgramData.problemSizeNoDepot; i++) {
            pos2[a2.tour[i]] = i;
        }
        distance = 0.0;
        for (int i = 0; i < ProgramData.problemSizeNoDepot - 1; i++) {
            if (a1.routes[i] == a2.routes[i + 1]) {
                j = a1.tour[i];
                h = a1.tour[i + 1];
                pos = pos2[j];
                if (a2.tour.length > pos + 1 && a2.tour[pos + 1] == h)
                    distance++; //common edges
            }
        }
        return 1.0 - (distance / (ProgramData.problemSize + ((a1.dummyDepots + a2.dummyDepots) / 2.0)));
    }

    public static void updateMemoryDynamically() {
        int index = -1;
        int closestInd = -1;
        double d, closest = Double.MAX_VALUE;
        //if random point still exist, i.e., if index = -1; then replace
        for (int i = 0; i < ProgramData.longMemorySize; i++) {
            if (ProgramData.randomPoint[i] == true) {
                index = i;
                ProgramData.randomPoint[i] = false;
                break;
            }
        }
        if (index != -1) {
            copyFromTo(ProgramData.previousBestSoFarAnt, ProgramData.longMemory[index]);
        } else {
            for (int i = 0; i < ProgramData.longMemorySize; i++) {
                //find the closest ant of the memory with the current best ant
                d = distanceBetween(ProgramData.previousBestSoFarAnt, ProgramData.longMemory[i]);
                if (closest > d) {
                    closest = d;
                    closestInd = i;
                }
            }
            //replace
            if (ProgramData.bestSoFarAnt.tourLength < ProgramData.longMemory[closestInd].tourLength) {
                copyFromTo(ProgramData.bestSoFarAnt, ProgramData.longMemory[closestInd]);
            }
        }
    }

    public static int findMemoryBest() {
        double min = ProgramData.longMemory[0].tourLength;
        int kMin = 0;
        for (int k = 1; k < ProgramData.longMemorySize; k++) {
            if (ProgramData.longMemory[k].tourLength < min) {
                min = ProgramData.longMemory[k].tourLength;
                kMin = k;
            }
        }
        return kMin;
    }

    public static void exitTry() {
        if(ProgramData.changeMode == ChangeMode.ReappearCyclic || ProgramData.changeMode == ChangeMode.ReappearRandomly) {
            ProgramData.cyclicRandomVector = null;
            ProgramData.cyclicReRandomVector = null;
        }
        if(ProgramData.noiseFlag) {
            ProgramData.noise = null;
            ProgramData.reNoise = null;
        }
        for (int i = 0; i < ProgramData.longMemorySize; i++)
            ProgramData.longMemory[i] = null;
    }

}
