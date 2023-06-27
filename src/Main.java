import com.sun.source.tree.ArrayAccessTree;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static int NETWORK_SIZE = 20;

    public static void main(String[] args) {
        Chain chain = new Chain();
        Network n = new Network(NETWORK_SIZE, chain);
        n.run();
        chain.print();

        /*

        ArrayList<Node> nodes = new ArrayList<Node>();
        for (int i = 0; i < 5; i++) {
            nodes.add(new Node());
        }


        List<Node> copy = new ArrayList<>();
        nodes.forEach(node -> copy.add(node.copy()));

        System.out.println(nodes.get(0).getContainers());
        System.out.println(copy.get(0).getContainers());

        Node first = nodes.get(0);
        Container ct = first.getContainers().get(0);
        first.removeContainer(ct);

        // System.out.println("Nodes: " + nodes);
        // System.out.println("Copy: " + copy);

        System.out.println(nodes.get(0).getContainers());
        System.out.println(copy.get(0).getContainers());

         */

        /*

        ArrayList<String> l1 = new ArrayList<>();
        l1.add("a");
        l1.add("b");
        l1.add("c");

        List<String> immutable = List.copyOf(l1);

        ArrayList<String> mutable = new ArrayList<>(immutable);
        mutable.add("d");
        System.out.println(mutable);

         */
    }
}
