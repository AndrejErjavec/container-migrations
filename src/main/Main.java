package main;

import main.blockchain.Chain;
import main.network.Network;
import main.types.Algorithm;
import main.types.TestCase;

import java.util.Random;

public class Main {
    public static void main(String[] args) {
        Configuration config = new Configuration();
        Random r = new Random();;
        int seed = r.nextInt(128);
        System.out.println("seed: " + seed);

        System.out.println("single");
        Chain chain1 = new Chain();
        Network n1 = new Network(config, chain1, seed, TestCase.WORST);
        n1.setMigrationAlgorithm(Algorithm.SINGLE);
        n1.outputCSV(true);
        n1.setCSVpath("analysis/migrations-single.csv");
        n1.run();
        chain1.print();

        System.out.println("multi diff");
        Chain chain2 = new Chain();
        Network n2 = new Network(config, chain2, seed, TestCase.WORST);
        n2.setMigrationAlgorithm(Algorithm.MULTI_DIFF);
        n2.outputCSV(true);
        n2.setCSVpath("analysis/migrations-multiple-diff.csv");
        n2.run();
        chain2.print();

        System.out.println("multi stddev");
        Chain chain3 = new Chain();
        Network n3 = new Network(config, chain3, seed, TestCase.WORST);
        n3.setMigrationAlgorithm(Algorithm.MULTI_STDDEV);
        n3.outputCSV(true);
        n3.setCSVpath("analysis/migrations-multiple-stddev.csv");
        n3.run();
        chain3.print();
    }
}
