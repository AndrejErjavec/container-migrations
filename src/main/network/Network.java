package main.network;

import main.blockchain.Block;
import main.blockchain.Chain;
import main.types.Algorithm;
import main.utils.CsvUtils;
import main.Configuration;
import main.types.TestCase;

import java.util.*;

public class Network {
    public int size;
    public int containers;
    public int blocks;
    public int blockDuration;
    public Chain chain;
    public int seed;
    public ArrayList<Node> nodes;
    public Block currentBlock;

    private float loadStdDev;
    // private float loadStdDevPrevBlock;
    private List<Node> snapshot;

    // ------- configuration -------
    private Configuration config;
    private Algorithm algorithm;
    private boolean outputCSV = false;
    private String csvFilePath;

    public Network(Configuration config, Chain chain, int seed, TestCase testCase) {
        this.size = config.NETWORK_SIZE;
        this.blocks = config.BLOCKS;
        this.containers = config.CONTAINERS;
        this.blockDuration = config.BLOCK_DURATION;
        this.chain = chain;
        this.seed = seed;
        this.config = config;

        this.nodes = new ArrayList<Node>();
        for (int i = 0; i < size; i++) {
            nodes.add(new Node());
        }
        switch (testCase) {
            case AVERAGE -> generateAverageCase();
            case WORST -> generateWorstCase();
        }
        /*
        if (fixedContainers) {
            int[] containerDistribution = RandomUtils.randomUniformDistribution(size, containers, seed);
            // System.out.println("container distribution: " + Arrays.toString(containerDistribution));
            for (int i = 0; i < size; i++) {
                network.Node node = new network.Node(containerDistribution[i], seed + i);
                this.nodes.add(node);
            }
        }
        else {
            for (int i = 0; i < this.size; i++) {
                this.nodes.add(new network.Node(seed + i));
            }
        }

         */

        snapshot = List.copyOf(nodes);
        loadStdDev = this.getLoadStdDev();
        // loadStdDevPrevBlock = Float.MAX_VALUE;
    }

    // ------- test case generators -------
    private void generateAverageCase() {
        Random r;
        for (int i = 0; i < containers; i++) {
            r = new Random((long) seed * i + i);
            int cpu = r.nextInt(config.MIN_CONTAINER_CPU, config.MAX_CONTAINER_CPU + 1);
            int node = r.nextInt(0, size);
            nodes.get(node).addContainer(new Container(cpu));
        }
    }

    private void generateWorstCase() {
        Random r = new Random(seed);
        int n = r.nextInt(0, size);
        Node node = nodes.get(n);
        for (int i = 0; i < containers; i++) {
            Random r2 = new Random((long) seed * i + i);
            int cpu = r2.nextInt(config.MIN_CONTAINER_CPU, config.MAX_CONTAINER_CPU + 1);
            node.addContainer(new Container(cpu));
        }
    }

    private Node getMaxLoadedNode(ArrayList<Node> nodes) {
        Node maxLoaded = nodes.get(0);
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getCpuUsage() > maxLoaded.getCpuUsage()) {
                maxLoaded = nodes.get(i);
            }
        }
        return maxLoaded;
    }

    private Node getMinLoadedNode(ArrayList<Node> nodes) {
        Node minLoaded = nodes.get(0);
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getCpuUsage() < minLoaded.getCpuUsage()) {
                minLoaded = nodes.get(i);
                if (nodes.get(i).getCpuUsage() == 0) break;
            }
        }
        return minLoaded;
    }

    private float getAverageLoad() {
        float sum = 0;
        for (int i = 0; i < nodes.size(); i++) {
            sum += nodes.get(i).getCpuUsage();
        }
        return sum / nodes.size();
    }

    private float getLoadStdDev() {
        float sum = 0;
        float mean = getAverageLoad();
        for (int i = 0; i < nodes.size(); i++) {
            sum += Math.abs(nodes.get(i).getCpuUsage() - mean);
        }
        return sum / nodes.size();
    }

    private void simulateMigration(Container ct, Node src, Node dst) {
        src.removeContainer(ct);
        dst.addContainer(ct);
        // System.out.println("Moved container with CPU usage " + ct.getCpuUsage() + " from node " + nodes.indexOf(src) + " to node " + nodes.indexOf(dst));
    }

    private void takeSnapshot(ArrayList<Node> nodes) {
        snapshot = new ArrayList<>();
        nodes.forEach(node -> snapshot.add(node.copy()));
    }

    private void restorePreviousState() {
        nodes.clear();
        snapshot.forEach(node -> {
            Node n = node.restore();
            nodes.add(n);
        });
    }

    // ------- algorithms -------

    /**
     * exsisting migration plan algorithm
     * single migration per block
     */

    private ArrayList<Migration> generateMigrationPlanSingle() {
        ArrayList migrationPlan = new ArrayList<>();
        Node maxLoadedNode = getMaxLoadedNode(nodes);
        Node minLoadedNode = getMinLoadedNode(nodes);
        Container toMigrate = maxLoadedNode.getMaxLoaded();

        int loadDelta = maxLoadedNode.getCpuUsage() - minLoadedNode.getCpuUsage();
        int nextLoadDelta = Math.abs((maxLoadedNode.getCpuUsage() - toMigrate.getCpuUsage()) - (minLoadedNode.getCpuUsage() + toMigrate.getCpuUsage()));
        if (loadDelta > nextLoadDelta) {
            simulateMigration(toMigrate, maxLoadedNode, minLoadedNode);
            Migration migration = new Migration(toMigrate.id, maxLoadedNode.id, minLoadedNode.id);
            migrationPlan.add(migration);
            if(outputCSV) { writeMigrationToCsv(migration, toMigrate); }
            return migrationPlan;
        }
        if (outputCSV && !(migrationPlan.size() > 0)) { writeMigrationToCsv(null, null); }
        return new ArrayList<>();
    }


    /**
     * new migration plan algorithm
     * multiple migrations per block
     * uses difference between min and max loaded node
     */

    private ArrayList<Migration> generateMigrationPlanMultiDiff() {
        ArrayList<Migration> migrationPlan = new ArrayList<Migration>();
        ArrayList<Node> availableNodes = new ArrayList<>(nodes);
        Node maxLoadedNode = getMaxLoadedNode(nodes);
        Node minLoadedNode = getMinLoadedNode(nodes);
        Container toMigrate = maxLoadedNode.getMaxLoaded();

        int loadDelta = maxLoadedNode.getCpuUsage() - minLoadedNode.getCpuUsage();
        int nextLoadDelta = Math.abs((maxLoadedNode.getCpuUsage() - toMigrate.getCpuUsage()) - (minLoadedNode.getCpuUsage() + toMigrate.getCpuUsage()));

        while (loadDelta > nextLoadDelta && availableNodes.size() > 0) {
            // skip migration if all nodes are equally loaded
            if (minLoadedNode.equals(maxLoadedNode)) break;

            // toMigrate = maxLoadedNode.getMaxLoaded();
            // loadDelta = maxLoadedNode.getCpuUsage() - minLoadedNode.getCpuUsage();
            // nextLoadDelta = Math.abs((maxLoadedNode.getCpuUsage() - toMigrate.getCpuUsage()) - (minLoadedNode.getCpuUsage() + toMigrate.getCpuUsage()));

            // System.out.println(loadDelta);
            // System.out.println(nextLoadDelta);

            // perform migration
            simulateMigration(toMigrate, maxLoadedNode, minLoadedNode);
            availableNodes.remove(minLoadedNode);
            // availableNodes.remove(maxLoadedNode);
            Migration migration = new Migration(toMigrate.id, maxLoadedNode.id, minLoadedNode.id);
            migrationPlan.add(migration);
            if (outputCSV) { writeMigrationToCsv(migration, toMigrate); }

            // calculate next migration
            maxLoadedNode = getMaxLoadedNode(nodes);
            minLoadedNode = getMinLoadedNode(availableNodes);
            toMigrate = maxLoadedNode.getMaxLoaded();

            loadDelta = maxLoadedNode.getCpuUsage() - minLoadedNode.getCpuUsage();
            nextLoadDelta = Math.abs((maxLoadedNode.getCpuUsage() - toMigrate.getCpuUsage()) - (minLoadedNode.getCpuUsage() + toMigrate.getCpuUsage()));

            // System.out.println("max loaded: " + maxLoadedNode.getCpuUsage());
            // System.out.println("min loaded: " + minLoadedNode.getCpuUsage());
            // System.out.println("load delta: " + loadDelta);
            // System.out.println("next load delta: " + nextLoadDelta);
        }

        if (outputCSV && !(migrationPlan.size() > 0)) { writeMigrationToCsv(null, null); }
        return migrationPlan;
    }


    /**
     * new migration plan algorithm
     * multiple migrations per block
     * uses standard deviation
     */

    private ArrayList<Migration> generateMigrationPlanMultiStdDev() {
        ArrayList<Migration> migrationPlan = new ArrayList<Migration>();
        ArrayList<Node> availableNodes = new ArrayList<>(nodes);
        float dev = getLoadStdDev();
        float devPrevious = dev;

        while (dev <= devPrevious && availableNodes.size() > 0) {
            Node maxLoadedNode = getMaxLoadedNode(nodes);
            Node minLoadedNode = getMinLoadedNode(availableNodes);
            // skip migration if all nodes are equally loaded
            if (minLoadedNode.equals(maxLoadedNode)) break;

            Container toMigrate = maxLoadedNode.getMaxLoaded();

            // perform migration
            simulateMigration(toMigrate, maxLoadedNode, minLoadedNode);
            availableNodes.remove(minLoadedNode);
            // availableNodes.remove(maxLoadedNode);

            // calculate standard deviation after migration
            devPrevious = dev;
            dev = getLoadStdDev();

            if (dev < devPrevious) {
                takeSnapshot(nodes);
                Migration migration = new Migration(toMigrate.id, maxLoadedNode.id, minLoadedNode.id);
                migrationPlan.add(migration);
                if (outputCSV) { writeMigrationToCsv(migration, toMigrate); }
            }
        }

        if (dev > devPrevious) {
            restorePreviousState();
            if (migrationPlan.size() > 0) migrationPlan.remove(migrationPlan.size()-1);
        }
        if (outputCSV && !(migrationPlan.size() > 0)) { writeMigrationToCsv(null, null); }
        return migrationPlan;
    }

    // ========== main loop ==========

    public void run() {
        // Timer timer = new Timer();
        // write initial state to csv
        writeMigrationToCsv(null, null);

        for (int i = 0; i < this.blocks; i++) {
            // loadStdDevPrevBlock = loadStdDev;
            ArrayList<Migration> migrationPlan = new ArrayList<>();
            currentBlock = chain.produceEmptyBlock();
            switch (algorithm) {
                case SINGLE -> migrationPlan = generateMigrationPlanSingle();
                case MULTI_DIFF -> migrationPlan = generateMigrationPlanMultiDiff();
                case MULTI_STDDEV -> migrationPlan = generateMigrationPlanMultiStdDev();
                default -> migrationPlan = new ArrayList<>();
            }
            currentBlock.addMigrationPlan(migrationPlan);
            chain.addBlock(currentBlock);
            // timer.schedule(new chain.ProduceBlock(migrationPlan, chain), 0, 1000);
            loadStdDev = getLoadStdDev();
            // printState();
            // System.out.println("------------ END OF BLOCK ------------");
        }
        // timer.cancel();
    }


    private void writeMigrationToCsv(Migration migration, Container container) {
        if (csvFilePath == null) setCSVpath("analysis/migrations.csv");

        // block, sourceNode, destinationNode, containerID, containerCPU, averageCPU, stddevCPU, minCPU, maxCPU, algorithm
        String blockHeight = (currentBlock != null) ? Integer.toString(currentBlock.blockHeight) : "0";
        // String blockId = currentBlock.id;
        String sourceNode = (migration != null && migration.source != null) ? migration.source : "";
        String destinationNode = (migration != null && migration.destination != null) ? migration.destination : "";
        String containerID = (migration != null && migration.container != null) ? migration.container : "";
        String containerCPU = (container != null) ? Integer.toString(container.getCpuUsage()) : "";
        String averageCPU = Float.toString(getAverageLoad());
        String stdDevCPU = Float.toString(getLoadStdDev());
        String minCPU = Integer.toString(getMinLoadedNode(nodes).getCpuUsage());
        String maxCPU = Integer.toString(getMaxLoadedNode(nodes).getCpuUsage());
        String algorithm = this.algorithm.toString();

        String[] line = {blockHeight, sourceNode, destinationNode, containerID, containerCPU, averageCPU, stdDevCPU, minCPU, maxCPU, algorithm};
        CsvUtils.writeLine(line, csvFilePath);
    }

    public void printState() {
        for (int i = 0; i < nodes.size(); i++) {
            System.out.print("network.Node " + i + ": ");
            nodes.get(i).print();
        }
        System.out.println("Std dev: " + getLoadStdDev());
        System.out.println("-------");
    }

    public void printStdDev() {
        System.out.println("Std dev: " + getLoadStdDev());
    }

    //  ------- configuration setters -------
    public void setMigrationAlgorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    public void outputCSV(boolean outputCVS) {
        this.outputCSV = outputCVS;
    }

    public void setCSVpath(String path) {
        this.csvFilePath = path;
        String[] headers = {"block", "sourceNode", "destinationNode", "containerID", "containerCPU", "averageCPU", "stddevCPU", "minCPU", "maxCPU", "algorithm"};
        CsvUtils.initCsv(headers, csvFilePath);
    }
}
