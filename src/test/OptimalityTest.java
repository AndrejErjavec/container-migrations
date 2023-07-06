package test;

import main.Configuration;
import main.blockchain.Chain;
import main.network.Container;
import main.network.Network;
import main.network.Node;
import main.types.Algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

public class OptimalityTest {
    public static void run() {
        Configuration config = new Configuration();
        Random r = new Random();
        int seed = 77; // r.nextInt(128);
        System.out.println("seed: " + seed);

        System.out.println("multi stddev");
        Chain chain = new Chain();
        Network n = new Network(config, chain, seed);
        n.setMigrationAlgorithm(Algorithm.MULTI_STDDEV_IMPROVED);
        n.outputCSV(false);
        System.out.println("Initial standard deviation: " + n.getLoadStdDev());
        n.run();
        System.out.println("Multi stddev result standard deviation: " + n.getLoadStdDev());

        testLPT(n);
        testSPT(n);
    }

    /**
     * The goal is to use all bins and minimize standard deviation.
     * LPT does just that
     * Multifit has a subroutine called FFD algorithm, which minimizes the number of used bins (not our goal)
     * SPT performance is worse
     */

    // Longest Processing Time First
    public static void testLPT(Network source) {
        ArrayList<Container> containers = source.getAllContainers();
        containers.sort(new Comparator<Container>() {
            @Override
            public int compare(Container c1, Container c2) {
                return c2.getCpuUsage() - c1.getCpuUsage();
            }
        });
        Network testNet = new Network(source.getConfig());
        for (int i = 0; i < containers.size(); i++) {
            Node minLoadedNode = testNet.getMinLoadedNode(testNet.nodes);
            minLoadedNode.addContainer(containers.get(i));
        }
        System.out.println("LPT result standard deviation: " + testNet.getLoadStdDev());
    }

    // Shortest Processing Time First
    public static void testSPT(Network source) {
        ArrayList<Container> containers = source.getAllContainers();
        containers.sort(new Comparator<Container>() {
            @Override
            public int compare(Container c1, Container c2) {
                return c1.getCpuUsage() - c2.getCpuUsage();
            }
        });
        Network testNet = new Network(source.getConfig());
        for (int i = 0; i < containers.size(); i++) {
            Node minLoadedNode = testNet.getMinLoadedNode(testNet.nodes);
            minLoadedNode.addContainer(containers.get(i));
        }
        System.out.println("SPT result standard deviation: " + testNet.getLoadStdDev());
    }
}
