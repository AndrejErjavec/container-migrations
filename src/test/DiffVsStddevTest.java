package test;

import main.Configuration;
import main.blockchain.Chain;
import main.network.Network;
import main.types.Algorithm;

import java.util.Random;

public class DiffVsStddevTest {
    private static String csvOutputRoot = "analysis/testdata/diff-stddev/";

    public static void run() {
        System.out.println("Testing diff vs stddev");

        Configuration config = new Configuration();
        Random r = new Random();
        int seed = 77; //r.nextInt(128);
        System.out.println("seed: " + seed);

        Chain chain2 = new Chain();
        Network n2 = new Network(config, chain2, seed);
        n2.setMigrationAlgorithm(Algorithm.MULTI_DIFF_IMPROVED);
        n2.outputCSV(true);
        n2.setCSVpath(csvOutputRoot + "migrations-diff.csv");
        n2.run();
        chain2.print();

        Chain chain3 = new Chain();
        Network n3 = new Network(config, chain3, seed);
        n3.setMigrationAlgorithm(Algorithm.MULTI_STDDEV_IMPROVED);
        n3.outputCSV(true);
        n3.setCSVpath(csvOutputRoot + "migrations-stddev.csv");
        n3.run();
        chain3.print();
    }
}
