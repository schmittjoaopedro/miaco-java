package schmitt.test;

/**
 * Created by root on 22/10/17.
 */
public class ant {
    int[] tour;
    boolean[] visited;
    int[] routes; //shows the vehicles that cover the customers
    int tour_length;
    int capacity; //vehicles current capacity
    int dummy_depots; //No. of vehicles
    boolean start_new; //start new route
    int no_customers; //No. of customers
}