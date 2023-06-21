import java.util.ArrayList;

public class Network {
    public int size;
    public ArrayList<Node> nodes;

    public Network(int size) {
        this.size = size;
        this.nodes = new ArrayList<Node>();

        for (int i = 0; i< this.size; i++) {
            this.nodes.add(new Node());
        }
    }

    public Node getMaxLoadedNode() {
        Node maxLoaded = nodes.get(0);
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getLoad() > maxLoaded.getLoad()) {
                maxLoaded = nodes.get(i);
            }
        }
        return maxLoaded;
    }

    public Node getMinLoadedNode() {
        Node minLoaded = nodes.get(0);
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getLoad() < minLoaded.getLoad()) {
                minLoaded = nodes.get(i);
            }
        }
        return minLoaded;
    }

    public void migrate(Container ct, Node src, Node dst) {
        src.removeContainer(ct);
        dst.addContainer(ct);
        System.out.println("Moved container with CPU usage " + ct.getCpuUsage() + " from node " + nodes.indexOf(src) + " to node " + nodes.indexOf(dst));
    }

    public void migrationPlan1() {
        Node maxLoadedNode = getMaxLoadedNode();
        Node minLoadedNode = getMinLoadedNode();
        Container maxLoadedContainer = maxLoadedNode.getMaxLoaded();

        migrate(maxLoadedContainer, maxLoadedNode, minLoadedNode);
    }

    public void print() {
        for (int i = 0; i < nodes.size(); i++) {
            System.out.print("Node " + i + ": ");
            nodes.get(i).print();
        }
    }
}
