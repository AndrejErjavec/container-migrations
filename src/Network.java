import utils.CsvUtils;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


public class Network {
    public int size;
    public Chain chain;
    public Block currentBlock;
    public ArrayList<Node> nodes;
    private static float loadStdDev;
    private static float loadStdDevPrevBlock;
    private static List<Node> snapshot;

    CsvUtils cu = new CsvUtils();
    private static String csvFileName = "analysis/migrations.csv";

    public Network(int size, Chain chain) {
        this.size = size;
        this.chain = chain;
        this.currentBlock = new Block();

        this.nodes = new ArrayList<Node>();
        for (int i = 0; i< this.size; i++) {
            this.nodes.add(new Node());
        }

        snapshot = List.copyOf(nodes);
        loadStdDev = this.getLoadStdDev();
        loadStdDevPrevBlock = Float.MAX_VALUE;

        String[] headers = {"block", "sourceNode", "destinationNode", "containerID", "containerCPU", "averageCPU", "stddevCPU", "minCPU", "maxCPU"};
        cu.initCsv(headers, csvFileName);
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
        // snapshot = List.copyOf(nodes);
        snapshot = new ArrayList<>();
        // snapshot.clear();
        nodes.forEach(node -> snapshot.add(node.copy()));
    }

    private void restorePrevoiusState() {
        nodes.clear();
        snapshot.forEach(node -> {
            Node n = new Node(node.id, new ArrayList<Container>(node.getContainers()));
            nodes.add(n);
        });
    }

    /** existing migration plan algorithm

    public void generateMigrationPlan() {
        Node maxLoadedNode = getMaxLoadedNode();
        Node minLoadedNode = getMinLoadedNode();
        Container ctToMigrate = maxLoadedNode.getMaxLoaded();

        int loadDelta = maxLoadedNode.getCpuUsage() - minLoadedNode.getCpuUsage();
        int nextLoadDelta = Math.abs(maxLoadedNode.getCpuUsage() - ctToMigrate.getCpuUsage()) - (minLoadedNode.getCpuUsage() + ctToMigrate.getCpuUsage());
        if (loadDelta > nextLoadDelta) {
            // simulateMigration(ctToMigrate, maxLoadedNode, minLoadedNode);
            migrationPlan.add(new Migration(ctToMigrate.id, maxLoadedNode.id, minLoadedNode.id));
        }
    }

    */

    private ArrayList<Migration> generateMigrationPlan() {
        ArrayList<Migration> migrationPlan = new ArrayList<Migration>();
        ArrayList<Node> availableNodes = new ArrayList<>();
        float dev = getLoadStdDev();
        float devPrevious = getLoadStdDev();

        nodes.forEach(node -> {
            availableNodes.add(node);
        });

        // printState();

        while (dev <= devPrevious && availableNodes.size() > 0) {
            Node maxLoadedNode = getMaxLoadedNode(availableNodes);
            Node minLoadedNode = getMinLoadedNode(availableNodes);

            List<Container> migrationCandidates = maxLoadedNode.getContainers();
            migrationCandidates.sort(new Comparator<Container>() {
                @Override
                public int compare(Container ct1, Container ct2) {
                    return ct1.getCpuUsage() - ct2.getCpuUsage();
                }
            });

            // System.out.println(migrationCandidates.stream().map(container -> container.getCpuUsage()).collect(Collectors.toList()));

            int loadDelta = maxLoadedNode.getCpuUsage() - minLoadedNode.getCpuUsage();
            int prevDiff = Integer.MAX_VALUE;

            // find the best candidate container to migrate
            Container toMigrate = null;
            Migration migration = null;
            for (int i = 0; i < migrationCandidates.size(); i++) {
                Container candidate = migrationCandidates.get(i);
                int nextLoadDelta = Math.abs(maxLoadedNode.getCpuUsage() - candidate.getCpuUsage()) - (minLoadedNode.getCpuUsage() + candidate.getCpuUsage());
                int diff = Math.abs(loadDelta - nextLoadDelta);

                if (diff < prevDiff) {
                    prevDiff = diff;
                }
                // always true - min and max nodes taken from available nodes
                else if (availableNodes.contains(maxLoadedNode) && availableNodes.contains(minLoadedNode)) {
                    takeSnapshot(nodes);
                    // perform migration
                    toMigrate = migrationCandidates.get(i-1);
                    migration = new Migration(toMigrate.id, maxLoadedNode.id, minLoadedNode.id);
                    migrationPlan.add(migration);
                    simulateMigration(toMigrate, maxLoadedNode, minLoadedNode);
                    availableNodes.remove(maxLoadedNode);
                    availableNodes.remove(minLoadedNode);

                    break;
                }
                else {
                    System.out.println("Skipped migration - already performed");
                    break;
                }
            }

            devPrevious = dev;
            dev = getLoadStdDev();
            if (dev <= devPrevious) {
                System.out.println("Std dev: " + dev);
                writeMigrationsToCsv(migration, toMigrate);
            }
            else {
                System.out.println("Std dev extra: " + dev);
            }
        }
        System.out.println("Standard deviation before restore: " + getLoadStdDev());
        restorePrevoiusState();
        System.out.println("Standard deviation after restore: " + getLoadStdDev());
        System.out.println("------- END OF BLOCK -------");
        migrationPlan.remove(migrationPlan.size()-1);
        return migrationPlan;
    }

    public void run() {
        while (loadStdDev < loadStdDevPrevBlock) {
            loadStdDevPrevBlock = loadStdDev;
            ArrayList<Migration> migrationPlan = generateMigrationPlan();
            if (migrationPlan.size() > 0) {
                currentBlock.addMigrationPlan(migrationPlan);
                // Block newBlock = new Block(migrationPlan);
                chain.addBlock(currentBlock);
                currentBlock = new Block();
            }
            loadStdDev = getLoadStdDev();
        }
    }

    public void executeMigrationPlan() {
        // ...
    }

    private void writeMigrationsToCsv(Migration migration, Container container) {
        // block, sourceNode, destinationNode, containerID, containerCPU, averageCPU, stddevCPU, minCPU, maxCPU
        String blockId = currentBlock.id;
        String sourceNode = migration.source;
        String destinationNode = migration.destination;
        String containerID = migration.container;
        String containerCPU = Integer.toString(container.getCpuUsage());
        String averageCPU = Float.toString(getAverageLoad());
        String stdDevCPU = Float.toString(getLoadStdDev());
        String minCPU = Integer.toString(getMinLoadedNode(nodes).getCpuUsage());
        String maxCPU = Integer.toString(getMaxLoadedNode(nodes).getCpuUsage());

        String[] line = {blockId, sourceNode, destinationNode, containerID, containerCPU, averageCPU, stdDevCPU, minCPU, maxCPU};
        cu.writeLine(line, csvFileName);
    }

    public void printState() {
        for (int i = 0; i < nodes.size(); i++) {
            System.out.print("Node " + i + ": ");
            nodes.get(i).print();
        }
        System.out.println("Std dev: " + getLoadStdDev());
        System.out.println("--------------------------------");
    }
}
