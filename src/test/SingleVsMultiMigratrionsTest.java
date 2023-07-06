package test;

import main.Configuration;
import main.blockchain.Chain;
import main.network.Network;
import main.types.Algorithm;

import java.util.Random;

public class SingleVsMultiMigratrionsTest {
    private static String csvOutputRoot = "analysis/testdata/single-multi/";

    public static void run() {
        System.out.println("Testing single vs multiple migrations");

        Configuration config = new Configuration();
        Random r = new Random();
        int seed = 77; //r.nextInt(128);
        System.out.println("seed: " + seed);

        Chain chain = new Chain();
        Network n = new Network(config, chain, seed);
        n.setMigrationAlgorithm(Algorithm.SINGLE);
        n.outputCSV(true);
        n.setCSVpath(csvOutputRoot + "migrations-single.csv");
        n.run();
        chain.print();

        Chain chain2 = new Chain();
        Network n2 = new Network(config, chain2, seed);
        n2.setMigrationAlgorithm(Algorithm.MULTI_DIFF);
        n2.outputCSV(true);
        n2.setCSVpath(csvOutputRoot + "migrations-multi.csv");
        n2.run();
        chain2.print();
    }
}
