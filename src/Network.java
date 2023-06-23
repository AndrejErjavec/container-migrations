import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

public class Network {
    public int size;
    public ArrayList<Node> nodes;
    public float averageLoad;
    public float loadStdDev;
    public ArrayList<Migration> migrationPlan;

    public Network(int size) {
        this.size = size;
        this.nodes = new ArrayList<Node>();
        this.migrationPlan = new ArrayList<Migration>();

        for (int i = 0; i< this.size; i++) {
            this.nodes.add(new Node());
        }

        this.averageLoad = this.getAverageLoad();
        this.loadStdDev = this.getLoadStdDev();
    }

    private Node getMaxLoadedNode() {
        Node maxLoaded = nodes.get(0);
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getCpuUsage() > maxLoaded.getCpuUsage()) {
                maxLoaded = nodes.get(i);
            }
        }
        return maxLoaded;
    }

    private Node getMinLoadedNode() {
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

    // existing migration plan algorithm
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

    public void generateMigrationPlanImproved() {
        printState();
        float currentDev = getLoadStdDev();
        for (int n = 0; n < 50; n++) {
            Node maxLoadedNode = getMaxLoadedNode();
            Node minLoadedNode = getMinLoadedNode();

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

            // find best candidate container to migrate
            for (int i = 0; i < migrationCandidates.size(); i++) {
                Container candidate = migrationCandidates.get(i);
                int nextLoadDelta = Math.abs(maxLoadedNode.getCpuUsage() - candidate.getCpuUsage()) - (minLoadedNode.getCpuUsage() + candidate.getCpuUsage());
                int diff = Math.abs(loadDelta - nextLoadDelta);
                if (diff < prevDiff) {
                    prevDiff = diff;
                }
                else {
                    Container toMigrate = migrationCandidates.get(i-1);
                    migrationPlan.add(new Migration(toMigrate.id, maxLoadedNode.id, minLoadedNode.id));
                    simulateMigration(toMigrate, maxLoadedNode, minLoadedNode);
                    break;
                }
            }
            float nextDev = getLoadStdDev();
            if (nextDev > currentDev) {
                migrationPlan.remove(migrationPlan.size()-1);
                return;
            }
            else {
                printState();
                currentDev = nextDev;
            }
            // System.out.println("Std dev: " + getLoadStdDev());
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

    public void printMigrationPlan() {
        migrationPlan.forEach(migration -> {
            migration.print();
        });
    }
}
