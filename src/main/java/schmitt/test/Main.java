package schmitt.test;

import java.io.*;
import java.security.SecureRandom;
import java.util.Random;
import java.util.StringTokenizer;

public class Main {

    public static double EPSILON = 0.000000000000000000000001;

    static Random random;

    static vertex init_objects[]; //Actual objects of the instance
    static vertex masked_objects[]; //Masked objects of the instance
    static int init_demand[]; //Actual customer demands
    static int masked_demand[]; //Masked customer demands
    static int distances[][]; //Distance matrix
    static int problem_size; //Size of the instance (includes depot)
    static int problem_size_no_depot; //Size of the instance (excludes depot)
    static String problem_instance; //Name of the instance

    static int max_capacity; //capacity of vehicles
    static int depot; //depot id (usually 0)

    static int change_mode; //How environment changes, reappear or not
    //1:random, 2:reappear(cyclic), 3:reappear(randomly), 4:varying
    static double change_degree; //Ratio of swapped objects
    static int change_speed; //Peiord of changes in algorithmic iterations

    static int cyclic_states = 4; //Base states for environments that reappaear
    static boolean noise_flag = false; //whether noise is added or not
    static double p_noise = 0.05; //Noise probability for environments that reappear

    static int cyclic_base_count; //Base states index for cyclical reappeared environments
    static int cyclic_random; //Base states index for randomly reappeared environments
    static int[] random_vector; //Mask of random objects
    static int[] re_random_vector; //Mask of re-ordered random objects
    static int[][] cyclic_random_vector; //Masks of objects used in reappered environments
    static int[][] cyclic_re_random_vector; //Masks of re-ordered objects used in reappeared environments
    static int[] noise; //Mask of random objects for noisy environments
    static int[] re_noise; //Mask of re-ordered objects for noisy environments

    static int current_iteration; //Used to calculate the period of change
    static int max_iterations;
    static int max_trials;

    //Used to output offline performance and population diversity
    static int[][] performance;
    static double[][] diversity;
    static Writer log_performance;
    static Writer log_diversity;
    static Writer log_varying_values;

    //output files
    static String perf_filename;
    static String div_filename;
    static String var_filename;

    /*static double env_random_number(double low, double high) {
        return low + ((high - low) * random.nextDouble());
    }*/
    static double env_random_number(double low, double high) {
        int rand = (int) (Math.random() * Integer.MAX_VALUE);
        return ((rand % 10000) / 10000.0) * (high - low) + low;
    }

    static int[] generate_random_vector(int size) {
        int tot_assigned = 0;
        int r[] = new int[size];
        for (int i = 0; i < size; i++) {
            r[i] = i + 1;
        }
        for (int i = 0; i < size; i++) {
            double rnd = env_random_number(0.0, 1.0);
            int node = (int) (rnd * (size - tot_assigned));
            int help = r[i];
            r[i] = r[i + node];
            r[i + node] = help;
            tot_assigned++;
        }
        return r;
    }

    static int[] generate_reordered_random_vector() {
        int changes = (int) Math.abs(change_degree * problem_size_no_depot);
        int[] r = new int[changes];
        for (int i = 0; i < changes; i++) {
            r[i] = random_vector[i];
        }
        for (int i = 0; i < changes; i++) {
            int help = r[i];
            int r_index = (int) (env_random_number(0.0, 1.0) * (double) changes); //random number 0..changes
            r[i] = r[r_index];
            r[r_index] = help;
        }
        return r;
    }

    static int euclidean_distance(int i, int j) {
        double xd = masked_objects[i].x - masked_objects[j].x;
        double yd = masked_objects[i].y - masked_objects[j].y;
        return (int) (Math.sqrt(xd * xd + yd * yd) + 0.5);
    }

    static void compute_distances() {
        for (int i = 0; i < problem_size; i++) {
            for (int j = 0; j < problem_size; j++) {
                distances[i][j] = euclidean_distance(i, j);
            }
        }
    }

    static void swap_masked_objects(int obj1, int obj2) {
        double help1 = masked_objects[obj1].x;
        double help2 = masked_objects[obj1].y;
        masked_objects[obj1].x = masked_objects[obj2].x;
        masked_objects[obj1].y = masked_objects[obj2].y;
        masked_objects[obj2].x = help1;
        masked_objects[obj2].y = help2;
        int help3 = masked_demand[obj1];
        masked_demand[obj1] = masked_demand[obj2];
        masked_demand[obj2] = help3;
    }

    static void add_random_change() {
        int changes = (int) Math.abs(change_degree * problem_size_no_depot); //exclude depot
        random_vector = generate_random_vector(problem_size_no_depot);
        re_random_vector = generate_reordered_random_vector();
        for (int i = 0; i < changes; i++) {
            int obj1 = masked_objects[random_vector[i]].id;
            int obj2 = masked_objects[re_random_vector[i]].id;
            swap_masked_objects(obj1, obj2);
        }
    }

    static void add_noise() {
        int noise_change = (int) Math.abs(p_noise * problem_size_no_depot);
        noise = generate_random_vector(problem_size_no_depot);
        re_noise = generate_random_vector(problem_size_no_depot);
        for (int i = 0; i < noise_change; i++) {
            int obj1 = masked_objects[noise[i]].id;
            int obj2 = masked_objects[re_noise[i]].id;
            swap_masked_objects(obj1, obj2);
        }
    }

    static void remove_noise() {
        int noise_change = (int) Math.abs(p_noise * problem_size_no_depot);
        for (int i = noise_change - 1; i >= 0; i--) {
            int obj1 = masked_objects[re_noise[i]].id;
            int obj2 = masked_objects[noise[i]].id;
            swap_masked_objects(obj1, obj2);
        }
        noise = null;
        re_noise = null;
    }

    static void reverse_changes() {
        if (noise_flag == true) remove_noise();
        int changes = (int) Math.abs(change_degree * problem_size_no_depot);
        for (int i = changes - 1; i >= 0; i--) {
            int obj1 = masked_objects[re_random_vector[i]].id;
            int obj2 = masked_objects[random_vector[i]].id;
            swap_masked_objects(obj1, obj2);
        }
    }

    static void generate_cyclic_environment() {
        int changes = (int) Math.abs(change_degree * problem_size_no_depot);
        cyclic_random_vector = new int[cyclic_states][changes];
        cyclic_re_random_vector = new int[cyclic_states][changes];
        for (int i = 0; i < cyclic_states; i++) {
            random_vector = generate_random_vector(problem_size_no_depot);
            re_random_vector = generate_reordered_random_vector();
            for (int j = 0; j < changes; j++) {
                cyclic_random_vector[i][j] = random_vector[j];
                cyclic_re_random_vector[i][j] = re_random_vector[j];
            }
        }
    }

    static void add_cyclic_change(int state) {
        int changes = (int) Math.abs(change_degree * problem_size_no_depot);
        for (int i = 0; i < changes; i++) {
            int obj1 = masked_objects[cyclic_random_vector[state][i]].id;
            int obj2 = masked_objects[cyclic_re_random_vector[state][i]].id;
            random_vector[i] = cyclic_random_vector[state][i];
            re_random_vector[i] = cyclic_re_random_vector[state][i];
            swap_masked_objects(obj1, obj2);
        }
        if (noise_flag == true) add_noise();
    }

    static void varying_environmental_changes() {
        change_speed = (int) env_random_number(1, 100); //random number 1..100
        change_degree = env_random_number(0.0, 1.0); //random number 0..1
        int changes = (int) Math.abs(change_degree * (problem_size_no_depot));
        random_vector = generate_random_vector(problem_size_no_depot);
        re_random_vector = generate_reordered_random_vector();
        for (int i = 0; i < changes; i++) {
            int obj1 = masked_objects[random_vector[i]].id;
            int obj2 = masked_objects[re_random_vector[i]].id;
            swap_masked_objects(obj1, obj2);
        }
        random_vector = null;
        re_random_vector = null;
    }

    static void read_problem(String filename) {
        filename = (new File("scenarios")).getAbsolutePath() + "/" + filename;
        try {
            File file = new File(filename);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            String delimiters = " :=\\n\\t\\r\\f\\v";
            while ((line = bufferedReader.readLine()) != null) {
                String keywords[] = getTokens(line, delimiters, true);
                if (keywords.length > 0) {
                    if ("DIMENSION".equals(keywords[0])) {
                        problem_size = Integer.valueOf(keywords[1]);
                    } else if ("EDGE_WEIGHT_TYPE".equals(keywords[0]) && !"EUC_2D".equals(keywords[1])) {
                        System.out.println("Not EUC_2D");
                        System.exit(0);
                    } else if ("CAPACITY".equals(keywords[0])) {
                        max_capacity = Integer.valueOf(keywords[1]);
                    } else if ("NODE_COORD_SECTION".equals(keywords[0])) {
                        if (problem_size != 0) {
                            problem_size_no_depot = problem_size - 1;
                            masked_objects = new vertex[problem_size];
                            init_objects = new vertex[problem_size];
                            for (int i = 0; i < problem_size; i++) {
                                keywords = getTokens(bufferedReader.readLine(), delimiters, true);
                                init_objects[i] = new vertex();
                                init_objects[i].id = Integer.parseInt(keywords[0]) - 1;
                                init_objects[i].x = Double.parseDouble(keywords[1]);
                                init_objects[i].y = Double.parseDouble(keywords[2]);
                                masked_objects[i] = new vertex();
                                masked_objects[i].id = init_objects[i].id;
                                masked_objects[i].x = init_objects[i].x;
                                masked_objects[i].y = init_objects[i].y;
                            }
                            distances = new int[problem_size][problem_size];
                            compute_distances();
                        }
                    } else if ("DEMAND_SECTION".equals(keywords[0])) {
                        if (problem_size != 0) {
                            masked_demand = new int[problem_size];
                            init_demand = new int[problem_size];
                            for (int i = 0; i < problem_size; i++) {
                                keywords = getTokens(bufferedReader.readLine(), delimiters, true);
                                init_demand[i] = Integer.parseInt(keywords[1]);
                                masked_demand[i] = init_demand[i];
                            }
                        }
                    } else if ("DEPOT_SECTION".equals(keywords[0])) {
                        keywords = getTokens(bufferedReader.readLine(), delimiters, true);
                        depot = Integer.parseInt(keywords[0]) - 1;
                    }
                }
            }
            fileReader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String[] getTokens(String str, String delim, boolean trim) {
        StringTokenizer stok = new StringTokenizer(str, delim);
        String tokens[] = new String[stok.countTokens()];
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = stok.nextToken();
            if (trim)
                tokens[i] = tokens[i].trim();
        }
        return tokens;
    }

    static void initialize_environment() {

        for (int i = 0; i < problem_size; i++) {
            masked_objects[i].x = init_objects[i].x;
            masked_objects[i].y = init_objects[i].y;
            masked_objects[i].id = init_objects[i].id;
            masked_demand[i] = init_demand[i];
        }

        switch (change_mode) {
            case 1://random environment
                add_random_change();
                break;
            case 2:
            case 3://reappear environments
                generate_cyclic_environment();
                cyclic_base_count = 0;
                add_cyclic_change(cyclic_base_count);
                cyclic_random = 0;
                break;
            case 4://varying environments
                change_speed = 10; //reset
                change_degree = 0.1;
                varying_environmental_changes();
                break;
        }
        compute_distances();
    }

    static void change_environment() {
        switch (change_mode) {
            case 1: // random
                reverse_changes();
                add_random_change();
                break;
            case 2: //reappear cyclic pattern
                if (cyclic_base_count == cyclic_states - 1)
                    cyclic_base_count = 0;
                else
                    cyclic_base_count++;
                reverse_changes();
                add_cyclic_change(cyclic_base_count);
                break;
            case 3: //reappear random pattern
                cyclic_base_count = ((int) random.nextDouble() * Integer.MAX_VALUE) % cyclic_states;
                while (cyclic_random == cyclic_base_count)
                    cyclic_base_count = ((int) random.nextDouble() * Integer.MAX_VALUE) % cyclic_states;
                cyclic_random = cyclic_base_count;
                reverse_changes();
                add_cyclic_change(cyclic_base_count);
                break;
            case 4: // varying
                varying_environmental_changes();
                break;
        }
        compute_distances();
    }

    static void check_input_parameters() {
        if (problem_size == 0) {
            System.err.println("wrong problem instance file");
            System.exit(0);
        }
        if (change_mode > 4 || change_mode <= 0) {
            System.err.println("select a valid change mode (between 1..4)");
            System.exit(0);
        }
        if (change_degree > 1.0 || change_degree <= 0.0) {
            System.err.println("select a valid change degree (between 0..1)");
            System.exit(0);
        }
        if (change_speed <= 0) {
            System.err.println("select a valid change speed (int)");
            System.exit(0);
        }
    }

    static int fitness_evaluation(int[] t, int[] id) {
        int tour_length = distances[0][t[0]];
        for (int i = 0; i < problem_size_no_depot - 1; i++) {
            if (id[i] == id[i + 1]) {
                tour_length += distances[t[i]][t[i + 1]];
            } else {
                tour_length += distances[t[i]][0];
                tour_length += distances[0][t[i + 1]];
            }
        }
        tour_length += distances[t[problem_size_no_depot - 1]][0];
        return tour_length;
    }

    /**************************************************************************************/
    /* This is an implementation of the RIACO, EIACO, HIACO and MIACO algorithms proposed */
    /* for the dynamic vehicle routing problem (DVRP) in Java of the following papers:    */
    /*                                                                                    */
    /*  M. Mavrovouniotis and S. Yang. Ant colony optimization with immigrants schemes    */
    /*  for the dynamic vehicle routing problem. EvoApplications 2012: Applications of    */
    /*  Evolutionary Computation, LNCS, vol. 7248, pp. 519-528, Springer-Verlag, 2012.    */
    /*                                                                                    */
    /*  M. Mavrovouniotis and S. Yang. Ant colony optimization with memory-based          */
    /*  immigrants for the dynamic vehicle routing problem. Proceedings of the 2012 IEEE  */
    /*  Congress on Evolutionary Computation, pp. 2645-2652, IEEE Press, 2012.            */
    /*                                                                                    */
    /*                                                                                    */
    /*  All ACO algorithms are integrated with the DBGP as an example                     */
    /*                                                                                    */
    /*  Some ACO methods are based on the original ACO framework implementation:          */
    /*                                                                                    */
    /*    Thomas Stuetzle. ACOTSP, Version 1.02. Available from                           */
    /*    http://www.aco-metaheuristic.org/aco-code, 2004.                                */
    /*                                                                                    */
    /* Written by:                                                                        */
    /*   Michalis Mavrovouniotis, De Montfort University, UK; refined July 2013           */
    /*                                                                                    */
    /* If any query, please email Michalis Mavrovouniotis at mmavrovouniotis@dmu.ac.uk    */
    /*                                                                                    */
    /**************************************************************************************/

    //Ant or individual structure that represent the TSP tour and tour cost

    static ant[] ant_population; //Population of ants or individuals
    static ant best_so_far_ant; //current best so far tour
    static ant previous_best_so_far_ant; //current-1 best so far tour
    static ant[] previous_best;

    static double[][] pheromone; //Pheromone matrix
    static double[][] heuristic; //Heuristic information matrix
    static double[][] total; //Pheromone + Heuristic information matrix

    //General ACO parameters
    static double alpha;
    static double beta;
    static double q_0;
    static double trail0;

    static int n_ants; //Population size
    static int depth; //Candidate list size (nearest neighbour)
    static int[][] nn_list; //Candidate lists

    static ant[] short_memory; //Short memory for RIACO,EIACO,HIACO and MIACO
    static ant[] long_memory; //Long  memory for MIACO

    static double immigrant_rate; //Immigrants replacement ratio
    static double p_mi; //Immigrants mutation probability(EIACO,HIACO,MIACO)

    static int short_memory_size; //Short memory size

    static int long_memory_size; //Long memory size
    static int t_m; //Long memory dynamic update
    static boolean[] random_point; //Initialization with random points

    static void set_algorithm_parameters() {
        alpha = 1.0;
        beta = 5.0;
        q_0 = 0.0;
        n_ants = 50;
        depth = 20;
        short_memory_size = 10;
        immigrant_rate = 0.4;
        p_mi = 0.01;
        long_memory_size = 4;
    }

    static void allocate_ants() {
        ant_population = new ant[n_ants];
        for (int i = 0; i < n_ants; i++) {
            ant_population[i] = new ant();
            ant_population[i].tour = new int[problem_size_no_depot];
            ant_population[i].visited = new boolean[problem_size];
            ant_population[i].routes = new int[problem_size_no_depot];
        }

        best_so_far_ant = new ant();
        best_so_far_ant.tour = new int[problem_size_no_depot];
        best_so_far_ant.visited = new boolean[problem_size];
        best_so_far_ant.routes = new int[problem_size_no_depot];

        previous_best = new ant[2];
        for (int i = 0; i < 2; i++) {
            previous_best[i] = new ant();
            previous_best[i].tour = new int[problem_size_no_depot];
            previous_best[i].visited = new boolean[problem_size];
            previous_best[i].routes = new int[problem_size_no_depot];
        }

        previous_best_so_far_ant = new ant();
        previous_best_so_far_ant.tour = new int[problem_size_no_depot];
        previous_best_so_far_ant.visited = new boolean[problem_size];
        previous_best_so_far_ant.routes = new int[problem_size_no_depot];
    }

    static void allocate_structures() {
        pheromone = new double[problem_size][problem_size];
        heuristic = new double[problem_size][problem_size];
        total = new double[problem_size][problem_size];
        nn_list = new int[problem_size][depth];
        short_memory = new ant[short_memory_size];
        long_memory = new ant[long_memory_size];
        random_point = new boolean[long_memory_size];
    }

    static void swap(int v[], int v2[], int i, int j) {
        int tmp;
        tmp = v[i];
        v[i] = v[j];
        v[j] = tmp;
        tmp = v2[i];
        v2[i] = v2[j];
        v2[j] = tmp;
    }

    static void sort(int v[], int v2[], int left, int right) {
        int k, last;
        if (left >= right)
            return;
        swap(v, v2, left, (left + right) / 2);
        last = left;
        for (k = left + 1; k <= right; k++)
            if (v[k] < v[left])
                swap(v, v2, ++last, k);
        swap(v, v2, left, last);
        sort(v, v2, left, last);
        sort(v, v2, last + 1, right);
    }

    static void compute_nn_lists() {
        int[] distance_vector = new int[problem_size];
        int[] help_vector = new int[problem_size];
        for (int j = 0; j < problem_size; j++) {
            for (int i = 0; i < problem_size; i++) {
                distance_vector[i] = distances[j][i];
                help_vector[i] = i;
            }
            distance_vector[j] = Integer.MAX_VALUE;
            sort(distance_vector, help_vector, 0, problem_size - 1);
            for (int i = 0; i < depth; i++) {
                nn_list[j][i] = help_vector[i];
            }
        }
    }

    static void init_pheromone_trails(double initial_trail) {
        for (int i = 0; i < problem_size; i++) {
            for (int j = 0; j <= i; j++) {
                pheromone[i][j] = initial_trail;
                pheromone[j][i] = initial_trail;
            }
        }
    }

    static void init_heuristic_info() {
        for (int i = 0; i < problem_size; i++) {
            for (int j = 0; j <= i; j++) {
                heuristic[i][j] = 1.0 / (distances[i][j] + EPSILON);
                heuristic[j][i] = heuristic[i][j];
            }
        }
    }

    static void compute_total_info() {
        for (int i = 0; i < problem_size; i++) {
            for (int j = 0; j <= i; j++) {
                total[i][j] = Math.pow(pheromone[i][j], alpha) * Math.pow(heuristic[i][j], beta);
                total[j][i] = total[i][j];
            }
        }
    }

    static void ant_empty_memory(ant a) {
        for (int i = 0; i < a.visited.length; i++) {
            a.visited[i] = false;
        }
        for (int i = 0; i < a.routes.length; i++) {
            a.routes[i] = 0;
            a.tour[i] = 0;
        }
        a.capacity = max_capacity;
        a.dummy_depots = 0;
        a.start_new = false;
        a.no_customers = 0;
    }

    static void place_ant(ant a) {
        a.start_new = true;
        a.visited[depot] = true;
        a.dummy_depots += 1;
    }

    static void update_capacity(ant a, int c, int p) {
        int cap = a.capacity - masked_demand[c];
        if (cap >= 0) {
            a.capacity -= masked_demand[c];
            a.visited[c] = true;
            a.tour[p] = c;
            a.routes[p] = a.dummy_depots;
            a.no_customers += 1;
        } else {
            a.dummy_depots += 1;
            a.capacity = max_capacity;
            a.start_new = true;
        }
    }

    static void choose_best_next(ant a, int phase) {
        int i, current, next;
        next = problem_size_no_depot - 1;
        if (a.start_new == true) {
            current = depot;
            a.visited[depot] = true;
            a.start_new = false;
        } else {
            current = a.tour[phase - 1];
        }
        double value_best = -1.0;
        for (i = 0; i < problem_size; i++) {
            if (a.visited[i] == false && total[current][i] > value_best) {
                next = i;
                value_best = total[current][i];
            }
        }
        update_capacity(a, next, phase);
    }

    static void neighbour_choose_best_next(ant a, int phase) {
        int current;
        int next = problem_size_no_depot - 1;
        if (a.start_new == true) {
            current = depot;
            a.visited[depot] = true;
            a.start_new = false;
        } else {
            current = a.tour[phase - 1];
        }
        double value_best = -1.0;
        for (int i = 0; i < depth; i++) {
            int temp = nn_list[current][i];
            if (a.visited[temp] == false) {
                double help = total[current][temp];
                if (help > value_best) {
                    value_best = help;
                    next = temp;
                }
            }
        }
        if (next == problem_size_no_depot - 1) {
            choose_best_next(a, phase);
        } else {
            update_capacity(a, next, phase);
        }
    }

    static void neighbour_choose_and_move_to_next(ant a, int phase) {
        if ((q_0 > 0.0) && (random.nextDouble() < q_0)) {
            neighbour_choose_best_next(a, phase);
        } else {
            int current;
            double sum_prob = 0.0;
            double[] prob_ptr = new double[depth + 1];
            if (a.start_new == true) {
                current = depot;
                a.visited[depot] = true;
                a.start_new = false;
            } else {
                current = a.tour[phase - 1];
            }
            for (int i = 0; i < depth; i++) {
                if (a.visited[nn_list[current][i]]) {
                    prob_ptr[i] = 0.0;
                } else {
                    prob_ptr[i] = total[current][nn_list[current][i]];
                    sum_prob += prob_ptr[i];
                }
            }
            if (sum_prob <= 0.0) {
                choose_best_next(a, phase);
            } else {
                double rnd = random.nextDouble() * sum_prob;
                int select = 0;
                double partial_sum = prob_ptr[select];
                while (partial_sum <= rnd) {
                    select++;
                    partial_sum += prob_ptr[select];
                }
                if (select == depth) {
                    neighbour_choose_best_next(a, phase);
                    return;
                } else {
                    int help = nn_list[current][select];
                    update_capacity(a, help, phase);
                }
            }
        }
    }

    static void construct_solutions() {
        int all_ants = 0;
        for (int k = 0; k < n_ants; k++) {
            ant_empty_memory(ant_population[k]);
            place_ant(ant_population[k]);
        }
        while (all_ants < n_ants) {
            for (int k = 0; k < n_ants; k++) {
                if (ant_population[k].no_customers < problem_size_no_depot)
                    neighbour_choose_and_move_to_next(ant_population[k], ant_population[k].no_customers);
                else
                    all_ants++;
            }
        }
        for (int k = 0; k < n_ants; k++) {
            ant_population[k].tour_length = fitness_evaluation(ant_population[k].tour, ant_population[k].routes);
        }
    }

    static void choose_closest_next(ant a, int phase) {
        int current;
        int min = Integer.MAX_VALUE;
        int next = problem_size_no_depot - 1;
        if (a.start_new == true) {
            current = depot;
            a.visited[depot] = true;
            a.start_new = false;
        } else {
            current = a.tour[phase - 1];
        }
        for (int i = 0; i < problem_size; i++) {
            if (a.visited[i] == false && distances[current][i] < min) {
                next = i;
                min = distances[current][i];
            }
        }
        update_capacity(a, next, phase);
    }

    static int nn_tour() {
        ant_empty_memory(ant_population[0]);
        place_ant(ant_population[0]);
        while (ant_population[0].no_customers < problem_size_no_depot) {
            choose_closest_next(ant_population[0], ant_population[0].no_customers);
        }
        ant_population[0].tour_length = fitness_evaluation(ant_population[0].tour, ant_population[0].routes);
        int help = ant_population[0].tour_length;
        ant_empty_memory(ant_population[0]);
        return help;
    }

    static int[] generate_random_immigrant() {
        int[] random_immigrant = new int[problem_size_no_depot];
        int tot_assigned = 0;
        for (int i = 0; i < problem_size_no_depot; i++) {
            random_immigrant[i] = i + 1;
        }
        for (int i = 0; i < problem_size_no_depot; i++) {
            int object = (int) (random.nextDouble() * (double) (problem_size_no_depot - tot_assigned));
            int help = random_immigrant[i];
            random_immigrant[i] = random_immigrant[i + object];
            random_immigrant[i + object] = help;
            tot_assigned++;
        }
        return random_immigrant;
    }

    static int[] random_r(int[] t) {
        int[] r = new int[problem_size_no_depot];
        int cap = max_capacity;
        int veh = 1;
        for (int i = 0; i < problem_size_no_depot; i++) {
            cap -= masked_demand[t[i]];
            if (cap >= 0) {
                r[i] = veh;
            } else if (random.nextDouble() <= 0.3 || cap < 0) {
                cap = max_capacity;
                veh++;
                r[i] = veh;
            }
        }
        return r;
    }

    static int[] guided_r(int[] t) {
        int[] r;
        r = new int[problem_size_no_depot];
        for (int i = 0; i < problem_size_no_depot; i++) {
            r[i] = previous_best_so_far_ant.routes[i];
        }
        return r;
    }

    static int find_memory_best() {
        int k, k_min, min;
        min = long_memory[0].tour_length;
        k_min = 0;
        for (k = 1; k < long_memory_size; k++) {
            if (long_memory[k].tour_length < min) {
                min = long_memory[k].tour_length;
                k_min = k;
            }
        }
        return k_min;
    }

    static int[] generate_memory_based_immigrant() {
        int[] memory_immigrant = new int[problem_size_no_depot];
        int mem_ind = find_memory_best();
        for (int i = 0; i < problem_size_no_depot; i++) {
            memory_immigrant[i] = long_memory[mem_ind].tour[i];
        }
        for (int v = 1; v <= long_memory[mem_ind].dummy_depots; v++) {
            int size = 0;
            for (int i = 0; i < problem_size_no_depot; i++) {
                if (long_memory[mem_ind].routes[i] == v) {
                    size++;
                }
            }
            int[] temp = new int[size];
            int step = 0;
            int j = 0;
            while (step < problem_size_no_depot) {
                if (long_memory[mem_ind].routes[step] == v) {
                    temp[j] = long_memory[mem_ind].tour[step];
                    j++;
                }
                step++;
            }
            for (int i = 0; i < size; i++) {
                if (random.nextDouble() <= p_mi) {
                    int object = (int) (random.nextDouble() * (double) (size - 1));
                    int help = temp[i];
                    temp[i] = temp[object];
                    temp[object] = help;
                }
            }
            j = 0;
            for (int i = 0; i < problem_size_no_depot; i++) {
                if (long_memory[mem_ind].routes[i] == v) {
                    memory_immigrant[i] = temp[j];
                    j++;
                }
            }
        }
        return memory_immigrant;
    }

    static void quick_swap(int v[], int v2[], int i, int j) {
        int tmp = v[i];
        v[i] = v[j];
        v[j] = tmp;
        tmp = v2[i];
        v2[i] = v2[j];
        v2[j] = tmp;
    }

    static void quick_sort(int v[], int v2[], int left, int right) {
        int k, last;
        if (left >= right)
            return;
        quick_swap(v, v2, left, (left + right) / 2);
        last = left;
        for (k = left + 1; k <= right; k++)
            if (v[k] < v[left])
                quick_swap(v, v2, ++last, k);
        quick_swap(v, v2, left, last);
        quick_sort(v, v2, left, last);
        quick_sort(v, v2, last + 1, right);
    }

    static void copy_from_to(ant a1, ant a2) {
        a2.tour_length = a1.tour_length;
        a2.dummy_depots = a1.dummy_depots;
        for (int i = 0; i < problem_size_no_depot; i++) {
            a2.tour[i] = a1.tour[i];
            a2.routes[i] = a1.routes[i];
        }
    }

    static void update_short_term_memory() {
        int im_size = (int) (short_memory_size * immigrant_rate);
        int[] tours = new int[n_ants];
        int[] id = new int[n_ants];
        ant[] immigrants = new ant[im_size];
        for (int i = 0; i < im_size; i++) {
            immigrants[i] = new ant();
            immigrants[i].tour = generate_memory_based_immigrant();
            immigrants[i].routes = guided_r(immigrants[i].tour);
            immigrants[i].tour_length = fitness_evaluation(immigrants[i].tour, immigrants[i].routes);
        }
        for (int i = 0; i < n_ants; i++) {
            tours[i] = ant_population[i].tour_length;
            id[i] = i;
        }
        quick_sort(tours, id, 0, n_ants - 1);
        for (int i = 0; i < short_memory_size; i++) {
            short_memory[i] = ant_population[id[i]];
        }
        for (int i = short_memory_size - 1; i > short_memory_size - im_size - 1; i--) {
            copy_from_to(immigrants[short_memory_size - 1 - i], short_memory[i]);
        }
    }


    static void init_memory_randomly() {
        for (int i = 0; i < long_memory_size; i++) {
            long_memory[i] = new ant();
            long_memory[i].tour = generate_random_immigrant();
            long_memory[i].routes = random_r(long_memory[i].tour);
            long_memory[i].tour_length = fitness_evaluation(long_memory[i].tour, long_memory[i].routes);
            random_point[i] = true;
        }
        t_m = 5 + ((int) (random.nextDouble() * 6.0));
    }

    static boolean detect_change() {
        int i, total_before, total_after;
        total_before = total_after = 0;
        for (i = 0; i < long_memory_size; i++) {
            total_before += long_memory[i].tour_length;
        }
        for (i = 0; i < long_memory_size; i++) {
            long_memory[i].tour_length = fitness_evaluation(long_memory[i].tour, long_memory[i].routes);
            total_after += long_memory[i].tour_length;
        }
        if (total_after == total_before)
            return false;
        else
            return true;
    }

    static double distance_between(ant a1, ant a2) {
        int distance = 0;
        int[] pos2 = new int[problem_size];
        for (int i = 0; i < problem_size_no_depot; i++) {
            pos2[a2.tour[i]] = i;
        }
        for (int i = 0; i < problem_size_no_depot - 1; i++) {
            if (a1.routes[i] == a2.routes[i + 1]) {
                int j = a1.tour[i];
                int h = a1.tour[i + 1];
                int pos = pos2[j];
                if (pos + 1 < a2.tour.length && a2.tour[pos + 1] == h)
                    distance++;
            }
        }
        return 1.0 - (distance / (problem_size_no_depot + ((a1.dummy_depots + a2.dummy_depots) / 2.0)));
    }

    static void update_memory_every_change() {
        int index = -1;
        for (int i = 0; i < long_memory_size; i++) {
            if (random_point[i] == true) {
                index = i;
                random_point[i] = false;
                break;
            }
        }
        previous_best_so_far_ant.tour_length = fitness_evaluation(previous_best_so_far_ant.tour, previous_best_so_far_ant.routes);
        if (index != -1) {
            copy_from_to(previous_best_so_far_ant, long_memory[index]);
        } else {
            double closest = Integer.MAX_VALUE;
            int closest_ind = -1;
            for (int i = 0; i < long_memory_size; i++) {
                double d = distance_between(previous_best_so_far_ant, long_memory[i]);
                if (closest > d) {
                    closest = d;
                    closest_ind = i;
                }
            }
            if (previous_best_so_far_ant.tour_length < long_memory[closest_ind].tour_length) {
                copy_from_to(previous_best_so_far_ant, long_memory[closest_ind]);
            }
        }
    }

    static void update_memory_dynamically() {
        int index = -1;
        for (int i = 0; i < long_memory_size; i++) {
            if (random_point[i] == true) {
                index = i;
                random_point[i] = false;
                break;
            }
        }
        if (index != -1) {
            copy_from_to(best_so_far_ant, long_memory[index]);
        } else {
            int closest_ind = -1;
            double closest = Integer.MAX_VALUE;
            for (int i = 0; i < long_memory_size; i++) {
                double d = distance_between(best_so_far_ant, long_memory[i]);
                if (closest > d) {
                    closest = d;
                    closest_ind = i;
                }
            }
            if (best_so_far_ant.tour_length < long_memory[closest_ind].tour_length) {
                copy_from_to(best_so_far_ant, long_memory[closest_ind]);
            }
        }
    }

    static void update_long_term_memory(int t) {
        boolean flag = detect_change();
        if (flag == true) {
            update_memory_every_change();
        }
        if (t == t_m && flag == false) {
            update_memory_dynamically();
            int rnd = 5 + ((int) (random.nextDouble() * 6.0));
            t_m = t + rnd;
        }
        if (t == t_m && flag == true) {
            int rnd = 5 + ((int) (random.nextDouble() * 6.0));
            t_m = t + rnd;
        }
    }

    static void constant_pheromone_deposit(ant a, double deltaT) {
        pheromone[0][a.tour[0]] += deltaT;
        pheromone[a.tour[0]][0] = pheromone[0][a.tour[0]];
        for (int i = 0; i < problem_size_no_depot - 1; i++) {
            int j = a.tour[i];
            int h = a.tour[i + 1];
            if (a.routes[i] == a.routes[i + 1]) {
                pheromone[j][h] += deltaT;
                pheromone[h][j] = pheromone[j][h];
            } else {
                pheromone[j][0] += deltaT;
                pheromone[0][j] = pheromone[j][0];
                pheromone[0][h] += deltaT;
                pheromone[h][0] = pheromone[0][h];
            }
        }
        pheromone[a.tour[problem_size_no_depot - 1]][0] += deltaT;
        pheromone[0][a.tour[problem_size_no_depot - 1]] = pheromone[a.tour[problem_size_no_depot - 1]][0];
    }

    static void generate_pheromone_matrix() {
        double deltaT = (1.0 - trail0) / (double) short_memory_size;
        init_pheromone_trails(trail0);
        for (int i = 0; i < short_memory_size; i++) {
            constant_pheromone_deposit(short_memory[i], deltaT);
        }
    }

    static void pheromone_update() {
        update_long_term_memory(current_iteration);
        update_short_term_memory();
        generate_pheromone_matrix();
        compute_total_info();
    }

    static int find_best() {
        int min = ant_population[0].tour_length;
        int k_min = 0;
        for (int k = 1; k < n_ants; k++) {
            if (ant_population[k].tour_length < min) {
                min = ant_population[k].tour_length;
                k_min = k;
            }
        }
        return k_min;
    }

    static void update_best() {
        int iteration_best = find_best();
        if (ant_population[iteration_best].tour_length < best_so_far_ant.tour_length) {
            copy_from_to(ant_population[iteration_best], best_so_far_ant);
        }
        copy_from_to(best_so_far_ant, previous_best_so_far_ant);
        copy_from_to(previous_best[1], previous_best[0]);
        copy_from_to(previous_best_so_far_ant, previous_best[1]);
        copy_from_to(previous_best[0], previous_best_so_far_ant);
        if (current_iteration == 1) copy_from_to(best_so_far_ant, previous_best_so_far_ant);
    }

    static void open_stats() {
        performance = new int[max_trials][max_iterations];
        diversity = new double[max_trials][max_iterations];
        perf_filename = String.format("PerformanceAlg_M_%d_D_%.2f_S_%d_%s.txt", change_mode, change_degree, change_speed, problem_instance);
        String file_path = (new File("output")).getAbsolutePath() + "/";
        try {
            log_performance = new BufferedWriter(new FileWriter(file_path + perf_filename, true));
        } catch (Exception e) {
            e.printStackTrace();
        }
        div_filename = String.format("DiversityAlg_M_%d_D_%.2f_S_%d_%s.txt", change_mode, change_degree, change_speed, problem_instance);
        try {
            log_diversity = new BufferedWriter(new FileWriter(file_path + div_filename, true));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (change_mode == 4) {
            var_filename = String.format("VaryingAlg_M_%d_S_Rand_D_Rand_%s.txt", change_mode, problem_instance);
            try {
                log_varying_values = new BufferedWriter(new FileWriter(file_path + var_filename, true));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static double mean(double[] values, int size) {
        int i;
        double m = 0.0;
        for (i = 0; i < size; i++) {
            m += values[i];
        }
        return m / (double) size;
    }

    static double stdev(double[] values, int size, double average) {
        int i;
        double dev = 0.0;
        if (size <= 1)
            return 0.0;
        for (i = 0; i < size; i++) {
            dev += (values[i] - average) * (values[i] - average);
        }
        return Math.sqrt(dev / (double) (size - 1));
    }

    static double calc_diversity(int size) {
        double div = 0.0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i != j) {
                    div += distance_between(ant_population[i], ant_population[j]);
                }
            }
        }
        return (1.0 / (size * (size - 1.0))) * div;
    }

    static void close_stats() {
        double perf_mean_value, perf_stdev_value;
        double div_mean_value, div_stdev_value;
        double[] perf_of_trials = new double[max_trials];
        double[] perf_of_iterations = new double[max_iterations];
        double[] div_of_trials = new double[max_trials];
        double[] div_of_iterations = new double[max_iterations];
        for (int i = 0; i < max_iterations; i++) {
            perf_of_iterations[i] = 0.0;
            div_of_iterations[i] = 0.0;
        }
        for (int i = 0; i < max_trials; i++) {
            perf_of_trials[i] = 0.0;
            div_of_trials[i] = 0.0;
        }
        try {
            System.out.println("time,bsf,div");
            for (int i = 0; i < max_iterations; i++) {
                for (int j = 0; j < max_trials; j++) {
                    perf_of_iterations[i] += performance[j][i];
                    div_of_iterations[i] += diversity[j][i];
                }
                perf_of_iterations[i] /= ((double) max_trials);
                div_of_iterations[i] /= ((double) max_trials);
                System.out.println(i + ", " + perf_of_iterations[i] + ", " + div_of_iterations[i]);
                log_performance.append(String.format("%.2f\n", perf_of_iterations[i]));
                log_diversity.append(String.format("%.2f\n", div_of_iterations[i]));
            }
            log_performance.append("\n");
            log_diversity.append("\n");
            log_performance.append("Statistical results\n");
            log_diversity.append("Statistical results\n");
            for (int i = 0; i < max_trials; i++) {
                for (int j = 0; j < max_iterations; j++) {
                    perf_of_trials[i] += performance[i][j];
                    div_of_trials[i] += diversity[i][j];
                }
                perf_of_trials[i] /= ((double) max_iterations);
                div_of_trials[i] /= ((double) max_iterations);
                log_performance.append(String.format("%.2f", perf_of_trials[i]));
                log_performance.append("\n");
                log_diversity.append(String.format("%.2f", div_of_trials[i]));
                log_diversity.append("\n");
            }
            perf_mean_value = mean(perf_of_trials, max_trials);
            perf_stdev_value = stdev(perf_of_trials, max_trials, perf_mean_value);
            div_mean_value = mean(div_of_trials, max_trials);
            div_stdev_value = stdev(div_of_trials, max_trials, div_mean_value);
            log_performance.append(String.format("Mean %f\t ", perf_mean_value));
            log_performance.append(String.format("\tStd Dev %f\t ", perf_stdev_value));
            log_diversity.append(String.format("Mean %f\t ", div_mean_value));
            log_diversity.append(String.format("\tStd Dev %f ", div_stdev_value));
            System.out.println("Mean " + perf_mean_value + " Std " + perf_stdev_value);
            log_performance.close();
            log_diversity.close();
            if (change_mode == 4) log_varying_values.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static void statistics_and_output(int r, int t) {
        performance[r - 1][t - 1] = best_so_far_ant.tour_length;
        diversity[r - 1][t - 1] = calc_diversity(n_ants);
        if (change_mode == 4 && r == 1) {
            try {
                log_varying_values.append(String.format("%.2f", change_degree));
                log_varying_values.append("\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //System.out.println("iteration: " + current_iteration + " best_so_far " + best_so_far_ant.tour_length + " diversity " + diversity[r - 1][t - 1]);
        //System.out.println(current_iteration + ", " + best_so_far_ant.tour_length + ", " + diversity[r - 1][t - 1]);
    }

    static void apply_to_algorithm() {
        compute_nn_lists();
        init_heuristic_info();
        compute_total_info();
        best_so_far_ant.tour_length = Integer.MAX_VALUE;
    }

    static void init_try(int t) {
        compute_nn_lists();
        init_heuristic_info();
        trail0 = 1.0 / nn_tour();
        init_pheromone_trails(trail0);
        compute_total_info();
        best_so_far_ant.tour_length = Integer.MAX_VALUE;
        previous_best_so_far_ant.tour_length = Integer.MAX_VALUE;
        init_memory_randomly();
    }

    public static void main(String argv[]) {
        //problem_instance = "F-n135-k7.vrp";
        problem_instance = "F-n72-k4.vrp";
        change_mode = 2;
        change_degree = 0.75;
        change_speed = 100;
        max_iterations = 1000;
        max_trials = 30;
        read_problem(problem_instance);
        check_input_parameters();
        open_stats();
        set_algorithm_parameters();
        allocate_ants();
        allocate_structures();
        for (int run = 1; run <= max_trials; run++) {
            System.out.println("-------------Run: " + run + "------------------");
            current_iteration = 1;
            //random = new Random(run);
            random = new SecureRandom();
            random.setSeed(run);
            initialize_environment();
            init_try(run);
            while (current_iteration <= max_iterations) {
                construct_solutions();
                update_best();
                pheromone_update();
                statistics_and_output(run, current_iteration);
                if (current_iteration % change_speed == 0) {
                    change_environment();
                    apply_to_algorithm();
                }
                current_iteration++;
            }
        }
        close_stats();
    }

}
