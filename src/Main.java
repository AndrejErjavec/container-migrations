import utils.CsvUtils;
import utils.RandomUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Main {
    public static int NETWORK_SIZE = 256;
    public static int CONTAINERS = 1000;
    public static int BLOCKS = 1000;

    public static void main(String[] args) {

        Random r = new Random();
        // 1449;
        int seed = r.nextInt(128);
        System.out.println("seed: " + seed);

        Chain chain1 = new Chain();
        Network n1 = new Network(NETWORK_SIZE, CONTAINERS, BLOCKS, chain1, seed);
        n1.multiMigrationsPerBlock(true);
        n1.outputCSV(true);
        n1.setCSVpath("analysis/migrations-multiple.csv");
        n1.run();
        chain1.print();


        Chain chain2 = new Chain();
        Network n2 = new Network(NETWORK_SIZE, CONTAINERS, BLOCKS, chain2, seed);
        n2.multiMigrationsPerBlock(false);
        n2.outputCSV(true);
        n2.setCSVpath("analysis/migrations-single.csv");
        n2.run();
        chain2.print();


        // int[] dist = RandomUtils.randomDistribution(NETWORK_SIZE, CONTAINERS, seed);
        // int[] distUniform = RandomUtils.randomUniformDistribution(NETWORK_SIZE, CONTAINERS, seed);

        // System.out.println(Arrays.toString(dist));
        // System.out.println(Arrays.toString(distUniform));
    }
}
