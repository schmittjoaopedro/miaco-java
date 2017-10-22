package schmitt.lab;

/**
 *  Coverted from C to Java the implementation of MIACO.
 *
 *  M. Mavrovouniotis and S. Yang. Ant colony optimization with memory-based
 *  immigrants for the dynamic vehicle routing problem. Proceedings of the 2012 IEEE
 *  Congress on Evolutionary Computation, pp. 2645-2652, IEEE Press, 2012.
 */
public class App  {

    public static void main(String[] args) {

        ProgramData.problemInstance = "F-n45-k4.vrp";
        //ProgramData.problemInstance = "F-n72-k4.vrp";
        //ProgramData.problemInstance = "F-n135-k7.vrp";
        ProgramData.changeMode = ChangeMode.ReappearCyclic;
        ProgramData.changeDegree = 0.75;
        ProgramData.changeSpeed = 100;
        ProgramData.maxIterations = 1000;
        ProgramData.maxTrials = 30;

        ProblemUtils.readProblem(ProgramData.problemInstance);
        ProgramData.checkInputParameters();

        Statistics.openStats();

        ACO.setAlgorithmParameters();
        ACO.allocateAnts();
        ACO.allocateStrutures();


        for(int run = 1; run <= ProgramData.maxTrials; run++) {
            DBGP.initializeEnvironment();
            ProgramData.currentIteration = 1;
            ACO.initTry(run);
            System.out.println("Trial " + run);
            while(ProgramData.currentIteration <= ProgramData.maxIterations) {
                ACO.constructSolutions();
                ACO.updateBest();
                ACO.pheromoneUpdate();
                Statistics.statisticsAndOutput(run, ProgramData.currentIteration);
                if(ProgramData.currentIteration % ProgramData.changeSpeed == 0) {
                    DBGP.changeEnvironment();
                    DBGP.applyToAlgorithm();
                }
                ProgramData.currentIteration++;
            }
            ACO.exitTry();
        }

        Statistics.closeStats();
    }

}
