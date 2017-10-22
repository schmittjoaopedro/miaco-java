package schmitt.lab;

public class Ant {

    public int[] tour;

    public boolean[] visited;

    public int[] routes;

    public double tourLength;

    public int capacity;

    public int dummyDepots;

    public boolean startNew;

    public int noCustomers;

    public void antEmptyMemory() {
        for(int i = 0; i < visited.length; i++) {
            this.visited[i] = false;
        }
        for(int i = 0; i < routes.length; i++) {
            this.routes[i] = 0;
        }
        this.capacity = ProgramData.maxCapacity;
        this.dummyDepots = 0;
        this.startNew = false;
        this.noCustomers = 0;
    }

    public void placeAnt() {
        this.startNew = true;
        this.visited[ProgramData.depot] = true;
        this.dummyDepots += 1;
    }

    public void chooseBestNext() {
        int next = ProgramData.problemSizeNoDepot - 1;
        int current;
        int phase = this.noCustomers;
        double min = Double.MAX_VALUE;
        if(this.startNew == true) {
            current = ProgramData.depot;
            this.visited[ProgramData.depot] = true;
            this.startNew = false;
        } else {
            current = this.tour[phase - 1];
        }
        for(int i = 0; i < ProgramData.problemSize; i++) {
            if(this.visited[i] == false && ProgramData.distances[current][i] < min) {
                next = i;
                min = ProgramData.distances[current][i];
            }
        }
        updateCapacity(next, phase);
    }

    public void updateCapacity(int next, int phase) {
        int cap = this.capacity - ProgramData.maskedDemand[next];
        if(cap >= 0) {
            this.capacity -= ProgramData.maskedDemand[next];
            this.visited[next] = true;
            this.tour[phase] = next;
            this.routes[phase] = this.dummyDepots;
            this.noCustomers += 1;
        } else {
            this.dummyDepots += 1;
            this.capacity = ProgramData.maxCapacity;
            this.startNew = true;
        }
    }

    public void fitnessEvaluation() {
        this.tourLength = ProgramData.distances[0][this.tour[0]]; //depot
        for(int i = 0; i < ProgramData.problemSizeNoDepot - 1; i++) {
            if(this.routes[i] == this.routes[i + 1]) {
                this.tourLength += ProgramData.distances[this.tour[i]][this.tour[i + 1]];
            } else {
                this.tourLength += ProgramData.distances[this.tour[i]][0];
                this.tourLength += ProgramData.distances[0][this.tour[i + 1]];
            }
        }
        this.tourLength += ProgramData.distances[this.tour[ProgramData.problemSizeNoDepot - 1]][0];
    }

    public void generateRandomImmigrant() {
        int totAssigned = 0;
        int[] randomImmigrant = new int[ProgramData.problemSizeNoDepot];
        for(int i = 0; i < ProgramData.problemSizeNoDepot; i++) {
            randomImmigrant[i] = i + 1;
        }
        for (int i = 0; i < ProgramData.problemSizeNoDepot; i++) {
            int object = (int) (Math.random() * (double) (ProgramData.problemSizeNoDepot - totAssigned));
            int help = randomImmigrant[i];
            randomImmigrant[i] = randomImmigrant[i + object];
            randomImmigrant[i + object] = help;
            totAssigned++;
        }
        this.tour = randomImmigrant;
    }

    public void randomR() {
        int r[] = new int[ProgramData.problemSizeNoDepot];
        int cap = ProgramData.maxCapacity;
        int veh = 1;
        for (int i = 0; i < ProgramData.problemSizeNoDepot; i++) {
            cap -= ProgramData.maskedDemand[this.tour[i]];
            if (cap >= 0) {
                r[i] = veh;
            } else if (Math.random() <= 0.3 || cap < 0) {
                cap = ProgramData.maxCapacity;
                veh++;
                r[i] = veh;
            }
        }
        this.routes = r;
    }

    public void neighbourChooseAndMoveToNext() {
        int phase = this.noCustomers;
        int help, current, select;
        double rnd;
        double partialSum;
        double sumProb = 0.0;
        double[] probPtr = new double[ProgramData.depth + 1];

        if ((ProgramData.q_0 > 0.0) && (Math.random() < ProgramData.q_0)) {
            //with probability q_0 make the best possible choice
            this.neighbourChooseBestNext(phase);
            return;
        }
        if (this.startNew == true) {
            current = ProgramData.depot; //depot
            this.visited[ProgramData.depot] = true;
            this.startNew = false;
        } else {
            current = this.tour[phase - 1];
        }
        //compute selection probabilities of nearest neigbhour objects
        for (int i = 0; i < ProgramData.depth; i++) {
            if (this.visited[ProgramData.nnList[current][i]]) {
                probPtr[i] = 0.0;
            } else {
                probPtr[i] = Math.min(Double.MAX_VALUE, ProgramData.total[current][ProgramData.nnList[current][i]]);
                sumProb += probPtr[i];
            }
        }
        if (sumProb <= 0.0) {
            //in case all neighbour objects are visited
            chooseBestNext();
        } else {
            //probabilistic selection
            rnd = Math.random();
            rnd *= sumProb;
            select = 0;
            partialSum = probPtr[select];
            while (partialSum <= rnd) {
                select++;
                partialSum += probPtr[select];
            }
            //this may very rarely happen because of rounding if
            //rnd is close to 1
            if (select == ProgramData.depth) {
                neighbourChooseBestNext(phase);
                return;
            }
            help = ProgramData.nnList[current][select];
            updateCapacity(help, phase);
        }
    }

    public void neighbourChooseBestNext(int phase) {
        int current, temp;
        double help;
        int next = ProgramData.problemSizeNoDepot - 1;
        if (this.startNew == true) {
            current = ProgramData.depot; //depot
            this.visited[ProgramData.depot] = true;
            this.startNew = false;
        } else { //current object of ant
            current = this.tour[phase - 1];
        }
        double valueBest = -1.0; //values in the list are always >=0.0
        //choose the next object with maximal (pheromone+heuristic) value
        //among all the nearest neighbour objects
        for (int i = 0; i < ProgramData.depth; i++) {
            temp = ProgramData.nnList[current][i];
            if (this.visited[temp] == false) {
                //if object not visited
                help = ProgramData.total[current][temp];
                if (help > valueBest) {
                    valueBest = help;
                    next = temp;
                }
            }
        }
        if (next == ProgramData.problemSizeNoDepot - 1) {
            //if all nearest neighbour objects are already visited
            chooseBestNext();
        } else {
            updateCapacity(next, phase);
        }
    }

    public void generateMemoryBasedImmigrant() {
        int j, step, help, object, size;
        int[] temp;

        int[] memoryImmigrant = new int[ProgramData.problemSizeNoDepot];
        int memInd = ACO.findMemoryBest(); //retrieve best from long memory
        for (int i = 0; i < ProgramData.problemSizeNoDepot; i++) {
            memoryImmigrant[i] = ProgramData.longMemory[memInd].tour[i];
        }
        for (int v = 1; v <= ProgramData.longMemory[memInd].dummyDepots; v++) {
            size = 0;
            for (int i = 0; i < ProgramData.problemSizeNoDepot; i++) {
                if (ProgramData.longMemory[memInd].routes[i] == v) {
                    size++;
                }
            }
            temp = new int[size];
            step = 0;
            j = 0;
            //split the vehicle routes
            while (step < ProgramData.problemSizeNoDepot) {
                if (ProgramData.longMemory[memInd].routes[step] == v) {
                    temp[j] = ProgramData.longMemory[memInd].tour[step];
                    j++;
                }
                step++;
            }
            for (int i = 0; i < size; i++) {
                //perform random swaps according to p_mi (small)
                if (Math.random() <= ProgramData.pMi) {
                    help = 0;
                    object = (int) (Math.random() * (double) (size - 1));
                    help = temp[i];
                    temp[i] = temp[object];
                    temp[object] = help;
                }
            }
            j = 0;
            //copy the swapped routes
            for (int i = 0; i < ProgramData.problemSizeNoDepot; i++) {
                if (ProgramData.longMemory[memInd].routes[i] == v) {
                    memoryImmigrant[i] = temp[j];
                    j++;
                }
            }
        }
        this.tour = memoryImmigrant;
    }

    public void guidedR() {
        int[] r = new int[ProgramData.problemSizeNoDepot];
        for (int i = 0; i < ProgramData.problemSizeNoDepot; i++) {
            r[i] = ProgramData.previousBestSoFarAnt.routes[i];
        }
        this.routes = r;
    }

}
