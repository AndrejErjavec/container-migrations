import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

public class Network {
    public int size;
    public Chain chain;
    public ArrayList<Node> nodes;
    private static float loadStdDev;
    private static float loadStdDevPrevBlock;

    public Network(int size, Chain chain) {
        this.size = size;
        this.nodes = new ArrayList<Node>();
        this.chain = chain;
        // this.migrationPlan = new ArrayList<Migration>();

        for (int i = 0; i< this.size; i++) {
            this.nodes.add(new Node());
        }

        loadStdDev = this.getLoadStdDev();
        loadStdDevPrevBlock = this.getLoadStdDev();
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
        float devPrev = getLoadStdDev();

        nodes.forEach(node -> availableNodes.add(node));

        // printState();

        while (dev <= devPrev && availableNodes.size() > 0) {
            Node maxLoadedNode = getMaxLoadedNode(availableNodes);
            Node minLoadedNode = getMinLoadedNode(availableNodes);

            ArrayList<Container> migrationCandidates = maxLoadedNode.getContainers();
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
            for (int i = 0; i < migrationCandidates.size(); i++) {
                Container candidate = migrationCandidates.get(i);
                int nextLoadDelta = Math.abs(maxLoadedNode.getCpuUsage() - candidate.getCpuUsage()) - (minLoadedNode.getCpuUsage() + candidate.getCpuUsage());
                int diff = Math.abs(loadDelta - nextLoadDelta);

                if (diff < prevDiff) {
                    prevDiff = diff;
                }
                // always true - min and max nodes taken from available nodes
                else if (availableNodes.contains(maxLoadedNode) && availableNodes.contains(minLoadedNode)) {
                    Container toMigrate = migrationCandidates.get(i-1);
                    migrationPlan.add(new Migration(toMigrate.id, maxLoadedNode.id, minLoadedNode.id));
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

            devPrev = dev;
            dev = getLoadStdDev();
            if (dev <= devPrev) {
                // printState();
                System.out.println("Std dev: " + getLoadStdDev());
            }
        }
        System.out.println("------- END OF BLOCK -------");
        migrationPlan.remove(migrationPlan.size()-1);
        return migrationPlan;
    }

    public void run() {
        while (loadStdDev <= loadStdDevPrevBlock) {
            loadStdDevPrevBlock = loadStdDev;
            ArrayList<Migration> migrationPlan = generateMigrationPlan();
            chain.addBlock(new Block(migrationPlan));
            loadStdDev = getLoadStdDev();
        }
    }

    public void executeMigrationPlan() {
        // ...
    }

    public void printState() {
        for (int i = 0; i < nodes.size(); i++) {
            System.out.print("Node " + i + ": ");
            nodes.get(i).print();
        }
        System.out.println("Std dev: " + getLoadStdDev());
        System.out.println("--------------------------------");
    }

    /*
    public void printMigrationPlan() {
        migrationPlan.forEach(migration -> {
            migration.print();
        });
    }
     */
}
