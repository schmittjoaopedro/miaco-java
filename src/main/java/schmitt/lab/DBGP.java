package schmitt.lab;

public class DBGP {

    public static void initializeEnvironment() {

        for (int i = 0; i < ProgramData.problemSize; i++) {
            ProgramData.maskedObjects[i].x = ProgramData.initObjects[i].x;
            ProgramData.maskedObjects[i].y = ProgramData.initObjects[i].y;
            ProgramData.maskedObjects[i].id = ProgramData.initObjects[i].id;
            ProgramData.maskedDemand[i] = ProgramData.initDemand[i];
        }

        switch (ProgramData.changeMode) {
            case Random:
                DBGP.addRandomChange();
                break;
            case ReappearCyclic:
            case ReappearRandomly:
                generateCyclicEnvironment();
                ProgramData.cyclicBaseCount = 0;
                addCyclicChance(ProgramData.cyclicBaseCount);
                ProgramData.cyclicRandom = 0;
                break;
            case Varying:
                ProgramData.changeSpeed = 10;
                ProgramData.changeDegree = 0.1;
                varyingEnvironmentalChanges();
                break;
        }
        ProblemUtils.computeDistances();
    }

    public static void addRandomChange() {
        int obj1, obj2;
        ProgramData.randomVector = generateRandomVector(ProgramData.problemSizeNoDepot);
        ProgramData.reRandomVector = generateReOrderedRandomVector();
        int changes = (int) Math.abs(ProgramData.changeDegree * (ProgramData.problemSizeNoDepot));
        for(int i = 0; i < changes; i++) {
            obj1 = ProgramData.maskedObjects[ProgramData.randomVector[i]].id;
            obj2 = ProgramData.maskedObjects[ProgramData.reRandomVector[i]].id;
            swapMaskedObjects(obj1, obj2);
        }
    }

    public static int[] generateRandomVector(int size) {
        int totAssigned = 0, node, help;
        double rnd;
        int r[] = new int[size];
        //input object indexes
        for(int i = 0; i < size; i++) {
            r[i] = i + 1; //exclude depot
        }
        for(int i = 0; i < size; i++) {
            rnd = Math.random();
            node = (int) (rnd * (size - totAssigned));
            help = r[i];
            r[i] = r[i + node];
            r[i + node] = help;
            totAssigned++;
        }
        return r;
    }

    public static int[] generateReOrderedRandomVector() {
        int changes, help, rIndex;
        changes = (int) Math.abs(ProgramData.changeDegree * (ProgramData.problemSizeNoDepot));
        int r[] = new int[changes];
        for (int i = 0; i < changes; i++) {
            r[i] = ProgramData.randomVector[i];
        }
        for (int i = 0; i < changes; i++) {
            help = r[i];
            rIndex = (int) (Math.random() * (double) changes);
            r[i] = r[rIndex];
            r[rIndex] = help;
        }
        return r;
    }

    public static void swapMaskedObjects(int obj1, int obj2) {
        double help1 = ProgramData.maskedObjects[obj1].x;
        double help2 = ProgramData.maskedObjects[obj1].y;
        ProgramData.maskedObjects[obj1].x = ProgramData.maskedObjects[obj2].x;
        ProgramData.maskedObjects[obj1].y = ProgramData.maskedObjects[obj2].y;
        ProgramData.maskedObjects[obj2].x = help1;
        ProgramData.maskedObjects[obj2].y = help2;

        int help3 = ProgramData.maskedDemand[obj1];
        ProgramData.maskedDemand[obj1] = ProgramData.maskedDemand[obj2];
        ProgramData.maskedDemand[obj2] = help3;
    }

    public static void generateCyclicEnvironment() {
        int changes = (int) Math.abs(ProgramData.changeDegree * ProgramData.problemSizeNoDepot);
        ProgramData.cyclicRandomVector = new int[ProgramData.cyclicStates][changes];
        ProgramData.cyclicReRandomVector = new int[ProgramData.cyclicStates][changes];
        for(int i = 0; i < ProgramData.cyclicStates; i++) {
            ProgramData.randomVector = generateRandomVector(ProgramData.problemSizeNoDepot);
            ProgramData.reRandomVector = generateReOrderedRandomVector();
            for(int j = 0; j < changes; j++) {
                ProgramData.cyclicRandomVector[i][j] = ProgramData.randomVector[j];
                ProgramData.cyclicReRandomVector[i][j] = ProgramData.reRandomVector[j];
            }
        }
    }

    public static void addCyclicChance(int state) {
        int obj1, obj2;
        int changes = (int) Math.abs(ProgramData.changeDegree * ProgramData.problemSizeNoDepot);
        for(int i = 0; i < changes; i++) {
            obj1 = ProgramData.maskedObjects[ProgramData.cyclicRandomVector[state][i]].id;
            obj2 = ProgramData.maskedObjects[ProgramData.cyclicReRandomVector[state][i]].id;
            ProgramData.randomVector[i] = ProgramData.cyclicRandomVector[state][i];
            ProgramData.reRandomVector[i] = ProgramData.cyclicReRandomVector[state][i];
            swapMaskedObjects(obj1, obj2);
        }
        if(ProgramData.noiseFlag) {
            addNoise();
        }
    }

    public static void addNoise() {
        int noiseChange = (int) Math.abs(ProgramData.pNoise * ProgramData.problemSizeNoDepot);
        ProgramData.noise = generateRandomVector(ProgramData.problemSizeNoDepot);
        ProgramData.reNoise = generateRandomVector(ProgramData.problemSizeNoDepot);
        for(int i = 0; i < noiseChange; i++) {
            int obj1 = ProgramData.maskedObjects[ProgramData.noise[i]].id;
            int obj2 = ProgramData.maskedObjects[ProgramData.reNoise[i]].id;
            swapMaskedObjects(obj1, obj2);
        }
    }

    public static void varyingEnvironmentalChanges() {
        ProgramData.changeSpeed = (int) Math.max(100, 1 + (Math.random() * 100));
        ProgramData.changeDegree = Math.random();
        int changes = (int) Math.abs(ProgramData.changeDegree * ProgramData.problemSizeNoDepot);
        ProgramData.randomVector = generateRandomVector(ProgramData.problemSizeNoDepot);
        ProgramData.reRandomVector = generateReOrderedRandomVector();
        for (int i = 0; i < changes; i++) {
            int obj1 = ProgramData.maskedObjects[ProgramData.randomVector[i]].id;
            int obj2 = ProgramData.maskedObjects[ProgramData.reRandomVector[i]].id;
            swapMaskedObjects(obj1, obj2);
        }
        ProgramData.randomVector = null;
        ProgramData.reRandomVector = null;
    }


    public static void changeEnvironment() {
        //period of change
        switch (ProgramData.changeMode) {
            case Random: // random
                reverseChanges();
                addRandomChange();
                break;
            case ReappearCyclic: //reappear cyclic pattern
                //cycle on the base states as a fixed logical ring
                if (ProgramData.cyclicBaseCount == ProgramData.cyclicStates - 1)
                    ProgramData.cyclicBaseCount = 0;
                else
                    ProgramData.cyclicBaseCount++;
                reverseChanges();
                addCyclicChance(ProgramData.cyclicBaseCount);
                break;
            case ReappearRandomly: //reappear random pattern
                ProgramData.cyclicBaseCount = ((int) (Math.random() * Integer.MAX_VALUE)) % ProgramData.cyclicStates;
                //avoid the same base state
                while (ProgramData.cyclicRandom == ProgramData.cyclicBaseCount) ProgramData.cyclicBaseCount = ((int) (Math.random() * Integer.MAX_VALUE)) % ProgramData.cyclicStates;
                ProgramData.cyclicRandom = ProgramData.cyclicBaseCount;
                reverseChanges();
                addCyclicChance(ProgramData.cyclicBaseCount);
                break;
            case Varying: // varying
                varyingEnvironmentalChanges();
                break;
        }
        ProblemUtils.computeDistances();
    }

    public static void reverseChanges() {
        int obj1, obj2;
        //remove noise first if any
        if (ProgramData.noiseFlag == true) removeNoise();
        //reverse swapped objects
        int changes = (int) Math.abs(ProgramData.changeDegree * (ProgramData.problemSizeNoDepot)); //exclude depot
        for (int i = changes - 1; i >= 0; i--) {
            obj1 = ProgramData.maskedObjects[ProgramData.reRandomVector[i]].id;
            obj2 = ProgramData.maskedObjects[ProgramData.randomVector[i]].id;
            swapMaskedObjects(obj1, obj2);
        }
    }

    public static void removeNoise() {
        int obj1, obj2;
        int noiseChange = (int) Math.abs(ProgramData.pNoise * (ProgramData.problemSizeNoDepot)); //exclude depot
        //reverse the noisy objects
        for (int i = noiseChange - 1; i >= 0; i--) {
            obj1 = ProgramData.maskedObjects[ProgramData.reNoise[i]].id;
            obj2 = ProgramData.maskedObjects[ProgramData.noise[i]].id;
            swapMaskedObjects(obj1, obj2);
        }
        ProgramData.noise = null;
        ProgramData.reNoise = null;
    }

    public static void applyToAlgorithm() {
        ACO.computeNNList();
        ACO.initHeuristicInfo();
        ACO.computeTotalInfo();
        ProgramData.bestSoFarAnt.tourLength = Double.MAX_VALUE;
    }

}
