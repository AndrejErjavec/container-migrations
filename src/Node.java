import utils.RandomUtils;

import java.sql.Array;
import java.util.*;
import java.util.stream.Collectors;

public class Node {
    public String id;
    private List<Container> containers;

    // for copying node
    public Node(String id, List<Container> containers) {
        this.id = id;
        this.containers = containers;
    }

    // empty node
    public Node() {
        this.id = UUID.randomUUID().toString();
        this.containers = new ArrayList<Container>();
    }

    public int getCpuUsage() {
        int load = 0;
        for (int i = 0; i < this.containers.size(); i++) {
            load += containers.get(i).getCpuUsage();
        }
        return load;
    }

    public Container getMaxLoaded() {
        if (containers.size() == 0) return null;
        Container maxLoaded = containers.get(0);
        for (int i = 0; i < containers.size(); i++) {
            if (containers.get(i).getCpuUsage() > maxLoaded.getCpuUsage()) {
                maxLoaded = containers.get(i);
            }
        }
        return maxLoaded;
    }

    public void removeContainer(Container container) {
        this.containers.remove(container);
    }

    public void addContainer(Container container) {
        this.containers.add(container);
    }

    // create node with immutable list of containers (for snapshots)
    public Node copy() {
     return new Node(this.id, List.copyOf(this.containers));
    }

    // immutable containers back to mutable (for snapshots)
    public Node restore() { return new Node(this.id, new ArrayList<Container>(this.getContainers())); }

    public void print() {
        // containers.forEach(container -> System.out.print(container.getCpuUsage() + "|"));
        System.out.print(containers.stream().map(container -> container.getCpuUsage()).collect(Collectors.toList()));
        System.out.println(" Load: " + this.getCpuUsage());
    }

    public List<Container> getContainers() {
        return containers;
    }
}
