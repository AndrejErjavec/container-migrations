package main.network;

import main.blockchain.Block;
import main.blockchain.Chain;
import main.types.Algorithm;
import main.utils.CsvUtils;
import main.Configuration;
import main.types.TestCase;

import java.util.*;
import java.util.stream.Collectors;

public class Network {
    public int size;
    public int containers;
    public int blocks;
    public int blockDuration;
    public Chain chain;
    public int seed;
    public ArrayList<Node> nodes;
    public Block currentBlock;

    // private float loadStdDev;
    // private float loadStdDevPrevBlock;
    private List<Node> snapshot;

    // ------- configuration -------
    private final Configuration config;
    private Algorithm algorithm;
    private boolean improved;
    private boolean outputCSV = false;
    private String csvFilePath;

    // completely unloaded network for testing
    public Network(Configuration config) {
        this.config = config;
        this.size = config.NETWORK_SIZE;
        this.nodes = new ArrayList<Node>();

        for (int i = 0; i < size; i++) {
            nodes.add(new Node());
        }
    }

    public Network(Configuration config, Chain chain, int seed) {
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

        switch (config.testCase) {
            case AVERAGE:
                if (config.ONLINE) { generateAverageCaseOnline(); }
                else { generateAverageCaseOffline(); }
                break;
            case WORST:
                if (config.ONLINE) { generateWorstCaseOnline(); }
                else { generateWorstCaseOffline(); }
                break;
        }

        snapshot = List.copyOf(nodes);
        // loadStdDev = this.getLoadStdDev();
        // loadStdDevPrevBlock = Float.MAX_VALUE;
    }

    /////////////////////// TEST CASE GENERATORS //////////////////////

    private void generateAverageCaseOffline() {
        Random r;
        for (int i = 0; i < containers; i++) {
            r = new Random((long) seed * i + i);
            int cpu = r.nextInt(config.MIN_CONTAINER_CPU, config.MAX_CONTAINER_CPU + 1);
            int node = r.nextInt(0, size);
            nodes.get(node).addContainer(new Container(cpu));
            // System.out.println(i + " Added container with CPU " + cpu + " to node " + node);
        }
    }

    private void generateWorstCaseOffline() {
        Random r = new Random(seed);
        int n = r.nextInt(0, size);
        Node node = nodes.get(n);
        for (int i = 0; i < containers; i++) {
            Random r2 = new Random((long) seed * i + i);
            int cpu = r2.nextInt(config.MIN_CONTAINER_CPU, config.MAX_CONTAINER_CPU + 1);
            node.addContainer(new Container(cpu));
        }
    }

    private void generateAverageCaseOnline() {
        if (totalContainers() + config.CONTAINERS_PER_BLOCK <= containers) {
            Random r;
            for (int i = 0; i < config.CONTAINERS_PER_BLOCK; i++) {
                r = new Random((long) seed * i + i);
                int cpu = r.nextInt(config.MIN_CONTAINER_CPU, config.MAX_CONTAINER_CPU + 1);
                int node = r.nextInt(0, size);
                nodes.get(node).addContainer(new Container(cpu));
            }
        }

    }

    private void generateWorstCaseOnline() {
        if (totalContainers() + config.CONTAINERS_PER_BLOCK <= containers) {
            Random r = new Random(seed);
            int n = r.nextInt(0, size);
            Node node = nodes.get(n);
            for (int i = 0; i < config.CONTAINERS_PER_BLOCK; i++) {
                Random r2 = new Random((long) seed * i + i);
                int cpu = r2.nextInt(config.MIN_CONTAINER_CPU, config.MAX_CONTAINER_CPU + 1);
                node.addContainer(new Container(cpu));
            }
        }
    }


    /////////////////////// NETWORK LOAD CALCULATIONS //////////////////////

    public Node getMaxLoadedNode(ArrayList<Node> nodes) {
        Node maxLoaded = nodes.get(0);
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getCpuUsage() > maxLoaded.getCpuUsage()) {
                maxLoaded = nodes.get(i);
            }
        }
        return maxLoaded;
    }

    public Node getMinLoadedNode(ArrayList<Node> nodes) {
        Node minLoaded = nodes.get(0);
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getCpuUsage() < minLoaded.getCpuUsage()) {
                minLoaded = nodes.get(i);
                if (nodes.get(i).getCpuUsage() == 0) break;
            }
        }
        return minLoaded;
    }

    public float getAverageLoad() {
        float sum = 0;
        for (int i = 0; i < nodes.size(); i++) {
            sum += nodes.get(i).getCpuUsage();
        }
        return sum / nodes.size();
    }

    public float getLoadStdDev() {
        float sum = 0;
        float mean = getAverageLoad();
        for (int i = 0; i < nodes.size(); i++) {
            sum += Math.abs(nodes.get(i).getCpuUsage() - mean);
        }
        return sum / nodes.size();
    }

    private int totalContainers() {
        int total = 0;
        for (int i = 0; i < nodes.size(); i++) {
            total += nodes.get(i).getContainers().size();
        }
        return total;
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

    /////////////////////// ALGORITHMS //////////////////////

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

    private ArrayList<Migration> generateMigrationPlanMultiDiff(boolean improvedSearch) {
        ArrayList<Migration> migrationPlan = new ArrayList<Migration>();
        ArrayList<Node> availableNodes = new ArrayList<>(nodes);
        Node maxLoadedNode = getMaxLoadedNode(nodes);
        Node minLoadedNode = getMinLoadedNode(nodes);
        Container toMigrate = maxLoadedNode.getMaxLoaded();

        int loadDelta = maxLoadedNode.getCpuUsage() - minLoadedNode.getCpuUsage();
        int nextLoadDelta = Math.abs((maxLoadedNode.getCpuUsage() - toMigrate.getCpuUsage()) - (minLoadedNode.getCpuUsage() + toMigrate.getCpuUsage()));

        // System.out.println("load delta: " + loadDelta);
        // System.out.println("next load delta: " + nextLoadDelta);

        while (loadDelta > nextLoadDelta && availableNodes.size() > 0) {
            // skip migration if all nodes are equally loaded
            if (minLoadedNode.equals(maxLoadedNode)) break;

            // improved search
            if (improvedSearch) {
                List<Container> migrationCandidates = maxLoadedNode.getContainers();
                migrationCandidates.sort(new Comparator<Container>() {
                    @Override
                    public int compare(Container ct1, Container ct2) {
                        return ct1.getCpuUsage() - ct2.getCpuUsage();
                    }
                });

                // System.out.println(migrationCandidates.stream().map(container -> container.getCpuUsage()).collect(Collectors.toList()));
                int current = maxLoadedNode.getCpuUsage() - minLoadedNode.getCpuUsage();

                // find the best candidate container to migrate
                for (int i = 0; i < migrationCandidates.size(); i++) {
                    Container candidate = migrationCandidates.get(i);
                    int next = Math.abs((maxLoadedNode.getCpuUsage() - candidate.getCpuUsage()) - (minLoadedNode.getCpuUsage() + candidate.getCpuUsage()));
                    if (next > current) {
                        if (i > 0) toMigrate = migrationCandidates.get(i-1);
                        else toMigrate = migrationCandidates.get(i);
                        break;
                    }
                    if (i == migrationCandidates.size() - 1) {
                        toMigrate = migrationCandidates.get(i);
                        break;
                    }
                    current = next;
                }
            }

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

    private ArrayList<Migration> generateMigrationPlanMultiStdDev(boolean improvedSearch) {
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

            // improved search
            if (improvedSearch) {
                List<Container> migrationCandidates = maxLoadedNode.getContainers();
                migrationCandidates.sort(new Comparator<Container>() {
                    @Override
                    public int compare(Container ct1, Container ct2) {
                        return ct1.getCpuUsage() - ct2.getCpuUsage();
                    }
                });

                // System.out.println(migrationCandidates.stream().map(container -> container.getCpuUsage()).collect(Collectors.toList()));

                int loadDelta = maxLoadedNode.getCpuUsage() - minLoadedNode.getCpuUsage();

                // find the best candidate container to migrate
                for (int i = 0; i < migrationCandidates.size(); i++) {
                    Container candidate = migrationCandidates.get(i);
                    int nextLoadDelta = Math.abs((maxLoadedNode.getCpuUsage() - candidate.getCpuUsage()) - (minLoadedNode.getCpuUsage() + candidate.getCpuUsage()));
                    if (nextLoadDelta > loadDelta) {
                        if (i > 0) toMigrate = migrationCandidates.get(i-1);
                        else toMigrate = migrationCandidates.get(i);
                        break;
                    }
                    if (i == migrationCandidates.size() - 1) {
                        toMigrate = migrationCandidates.get(i);
                        break;
                    }
                    loadDelta = nextLoadDelta;
                }
            }

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

    /////////////////////// MAIN LOOP //////////////////////

    public void run() {
        // Timer timer = new Timer();
        // write initial state to csv
        writeMigrationToCsv(null, null);

        for (int i = 0; i < this.blocks; i++) {
            if (config.ONLINE) {
                switch (config.testCase) {
                    case AVERAGE -> generateAverageCaseOnline();
                    case WORST -> generateWorstCaseOnline();
                }
            }

            // loadStdDevPrevBlock = loadStdDev;
            ArrayList<Migration> migrationPlan;
            currentBlock = chain.produceEmptyBlock();
            switch (algorithm) {
                case SINGLE -> migrationPlan = generateMigrationPlanSingle();
                case MULTI_DIFF -> migrationPlan = generateMigrationPlanMultiDiff(false);
                case MULTI_DIFF_IMPROVED -> migrationPlan = generateMigrationPlanMultiDiff(true);
                case MULTI_STDDEV -> migrationPlan = generateMigrationPlanMultiStdDev(false);
                case MULTI_STDDEV_IMPROVED -> migrationPlan = generateMigrationPlanMultiStdDev(true);
                default -> migrationPlan = new ArrayList<>();
            }
            currentBlock.addMigrationPlan(migrationPlan);
            chain.addBlock(currentBlock);

            // timer.schedule(new chain.ProduceBlock(migrationPlan, chain), 0, 1000);
            // loadStdDev = getLoadStdDev();
            // printState();
            // System.out.println("------------ END OF BLOCK ------------");
        }
        // timer.cancel();
    }


    private void writeMigrationToCsv(Migration migration, Container container) {
        if (csvFilePath == null) setCSVpath("analysis/migrations.csv");

        String blockHeight = (currentBlock != null) ? Integer.toString(currentBlock.blockHeight) : "0";
        String totalContainers = Integer.toString(totalContainers());
        String sourceNode = (migration != null && migration.source != null) ? migration.source : "";
        String destinationNode = (migration != null && migration.destination != null) ? migration.destination : "";
        String containerID = (migration != null && migration.container != null) ? migration.container : "";
        String containerCPU = (container != null) ? Integer.toString(container.getCpuUsage()) : "";
        String averageCPU = Float.toString(getAverageLoad());
        String stdDevCPU = Float.toString(getLoadStdDev());
        String minCPU = Integer.toString(getMinLoadedNode(nodes).getCpuUsage());
        String maxCPU = Integer.toString(getMaxLoadedNode(nodes).getCpuUsage());
        String algorithm = this.algorithm.toString();
        String testCase = config.testCase.toString();

        String[] line = {blockHeight, totalContainers, sourceNode, destinationNode, containerID, containerCPU, averageCPU, stdDevCPU, minCPU, maxCPU, algorithm, testCase};
        CsvUtils.writeLine(line, csvFilePath);
    }

    public ArrayList<Container> getAllContainers() {
        ArrayList<Container> out = new ArrayList<>();
        nodes.forEach(node -> {
            out.addAll(node.getContainers());
        });
        return out;
    }

    public Configuration getConfig() {
        return this.config;
    }

    /////////////////////// CONFIGURATION SETTERS //////////////////////

    public void setMigrationAlgorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    public void outputCSV(boolean outputCVS) {
        this.outputCSV = outputCVS;
    }

    public void setCSVpath(String path) {
        this.csvFilePath = path;
        String[] headers = {"block", "totalContainers", "sourceNode", "destinationNode", "containerID", "containerCPU", "averageCPU", "stddevCPU", "minCPU", "maxCPU", "algorithm", "testCase"};
        CsvUtils.initCsv(headers, csvFilePath);
    }

    /////////////////////// PRINTING //////////////////////

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

    public void printNodesCpu() {
        System.out.println(nodes.stream().map(node -> node.getCpuUsage()).collect(Collectors.toList()));
    }

    public void printNodesContainers() {
        System.out.println(nodes.stream().map(node -> node.getContainers().size()).collect(Collectors.toList()));
    }
}
