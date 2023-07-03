import utils.CsvUtils;
import utils.RandomUtils;

import java.util.*;
import java.util.stream.Collectors;

public class Network {
    public int size;
    public int containers;
    public int blocks;
    public Chain chain;
    public Block currentBlock;
    public Block prevBlock;
    public ArrayList<Node> nodes;
    private float loadStdDev;
    private float loadStdDevPrevBlock;
    private List<Node> snapshot;

    // ------- configuration -------
    private boolean multiMigrationsPerBlock = true;
    private boolean outputCSV = false;

    private String csvFilePath;

    public Network(int size, int blocks, Chain chain, int seed) {
        this.size = size;
        this.blocks = blocks;
        this.chain = chain;
        this.currentBlock = new Block(1);
        this.prevBlock = null;

        this.nodes = new ArrayList<Node>();
        for (int i = 0; i < this.size; i++) {
            this.nodes.add(new Node(seed + i));
        }

        snapshot = List.copyOf(nodes);
        loadStdDev = this.getLoadStdDev();
        loadStdDevPrevBlock = Float.MAX_VALUE;
    }

    public Network(int size, int containers, int blocks, Chain chain, int seed) {
        this.size = size;
        this.containers = containers;
        this.blocks = blocks;
        this.chain = chain;
        this.currentBlock = new Block(1);
        this.prevBlock = null;

        this.nodes = new ArrayList<Node>();

        int[] containerDistribution = RandomUtils.randomUniformDistribution(size, containers, seed);
        for (int i = 0; i < size; i++) {
            Node node = new Node(containerDistribution[i], seed + i);
            this.nodes.add(node);
        }

        snapshot = List.copyOf(nodes);
        loadStdDev = this.getLoadStdDev();
        loadStdDevPrevBlock = Float.MAX_VALUE;
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

    /**
     * exsisting migration plan algorithm
     * single migration per block
     */
    private ArrayList<Migration> generateMigrationPlanOld() {
        Node maxLoadedNode = getMaxLoadedNode(nodes);
        Node minLoadedNode = getMinLoadedNode(nodes);
        Container toMigrate = maxLoadedNode.getMaxLoaded();

        int loadDelta = maxLoadedNode.getCpuUsage() - minLoadedNode.getCpuUsage();
        int nextLoadDelta = Math.abs((maxLoadedNode.getCpuUsage() - toMigrate.getCpuUsage()) - (minLoadedNode.getCpuUsage() + toMigrate.getCpuUsage()));
        if (loadDelta > nextLoadDelta) {
            simulateMigration(toMigrate, maxLoadedNode, minLoadedNode);
            ArrayList migrations = new ArrayList<>();
            Migration migration = new Migration(toMigrate.id, maxLoadedNode.id, minLoadedNode.id);
            migrations.add(migration);
            if(outputCSV) { writeMigrationsToCsv(migration, toMigrate); }
            return migrations;
        }
        return new ArrayList<>();
    }

    /**
     * new migration plan algorithm
     * multiple migrations per block
     */
    private ArrayList<Migration> generateMigrationPlan() {
        ArrayList<Migration> migrationPlan = new ArrayList<Migration>();
        ArrayList<Node> availableNodes = new ArrayList<>();
        float dev = getLoadStdDev();
        float devPrevious = dev;

        nodes.forEach(node -> {
            availableNodes.add(node);
        });

        // System.out.println("INITIAL STATE");
        // printState();

        while (dev <= devPrevious && availableNodes.size() > 0) {
            Node maxLoadedNode = getMaxLoadedNode(availableNodes);
            Node minLoadedNode = getMinLoadedNode(availableNodes);

            // skip migration if all nodes are equally loaded
            if(minLoadedNode.equals(maxLoadedNode)) break;

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
            Container toMigrate = null;
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

            // perform migration
            simulateMigration(toMigrate, maxLoadedNode, minLoadedNode);
            availableNodes.remove(maxLoadedNode);
            availableNodes.remove(minLoadedNode);

            devPrevious = dev;
            dev = getLoadStdDev();
            if (dev <= devPrevious) {
                // System.out.println("Std dev: " + dev);
                takeSnapshot(nodes);
                Migration migration = new Migration(toMigrate.id, maxLoadedNode.id, minLoadedNode.id);
                migrationPlan.add(migration);
                if (outputCSV) { writeMigrationsToCsv(migration, toMigrate); }
            }
            // printState();
        }

        if (dev > devPrevious) {
            // System.out.println("Standard deviation before restore: " + getLoadStdDev());
            restorePreviousState();
            // System.out.println("Standard deviation after restore: " + getLoadStdDev());
            if (migrationPlan.size() > 0) migrationPlan.remove(migrationPlan.size()-1);
        }

        // System.out.println("FINAL STATE");
        // printState();
        return migrationPlan;
    }

    public void run() {
        // while (loadStdDev < loadStdDevPrevBlock) {
        // printState();
        for (int i = 0; i < this.blocks; i++) {
            loadStdDevPrevBlock = loadStdDev;
            ArrayList<Migration> migrationPlan;
            if (multiMigrationsPerBlock) { migrationPlan = generateMigrationPlan(); }
            else { migrationPlan = generateMigrationPlanOld(); }
            if (migrationPlan.size() > 0) {
                produceBlock(migrationPlan);
            }
            loadStdDev = getLoadStdDev();
            // printState();
            // System.out.println("------------ END OF BLOCK ------------");
        }
    }

    private void produceBlock(ArrayList<Migration> migrationPlan) {
        currentBlock.addMigrationPlan(migrationPlan);
        chain.addBlock(currentBlock);
        prevBlock = currentBlock;
        currentBlock = new Block(currentBlock.blockHeight + 1);
    }

    public void executeMigrationPlan() {
        // ...
    }

    private void writeMigrationsToCsv(Migration migration, Container container) {
        if (csvFilePath == null) csvFilePath = "migrations.csv";

        // block, sourceNode, destinationNode, containerID, containerCPU, averageCPU, stddevCPU, minCPU, maxCPU
        String blockHeight = Integer.toString(currentBlock.blockHeight);
        // String blockId = currentBlock.id;
        String sourceNode = migration.source;
        String destinationNode = migration.destination;
        String containerID = migration.container;
        String containerCPU = Integer.toString(container.getCpuUsage());
        String averageCPU = Float.toString(getAverageLoad());
        String stdDevCPU = Float.toString(getLoadStdDev());
        String minCPU = Integer.toString(getMinLoadedNode(nodes).getCpuUsage());
        String maxCPU = Integer.toString(getMaxLoadedNode(nodes).getCpuUsage());

        String[] line = {blockHeight, sourceNode, destinationNode, containerID, containerCPU, averageCPU, stdDevCPU, minCPU, maxCPU};
        CsvUtils.writeLine(line, csvFilePath);
    }

    public void printState() {
        for (int i = 0; i < nodes.size(); i++) {
            System.out.print("Node " + i + ": ");
            nodes.get(i).print();
        }
        System.out.println("Std dev: " + getLoadStdDev());
        System.out.println("-------");
    }

    public void printStdDev() {
        System.out.println("Std dev: " + getLoadStdDev());
    }

    //  ------- configuration setters -------
    public void multiMigrationsPerBlock(boolean multiMigrationsPerBlock) {
        this.multiMigrationsPerBlock = multiMigrationsPerBlock;
    }

    public void outputCSV(boolean outputCVS) {
        this.outputCSV = outputCVS;
    }

    public void setCSVpath(String path) {
        this.csvFilePath = path;
        String[] headers = {"block", "sourceNode", "destinationNode", "containerID", "containerCPU", "averageCPU", "stddevCPU", "minCPU", "maxCPU"};
        CsvUtils.initCsv(headers, csvFilePath);
    }
}
