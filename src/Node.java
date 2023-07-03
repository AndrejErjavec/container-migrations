import utils.RandomUtils;

import java.sql.Array;
import java.util.*;
import java.util.stream.Collectors;

public class Node {
    public String id;
    private List<Container> containers;
    private static final int max_containers = 10;
    private static final int max_container_cpu_usage = 50;
    private static final int max_cpu_usage = 100;

    // for copying node
    public Node(String id, List<Container> containers) {
        this.id = id;
        this.containers = containers;
    }

    // random node
    public Node() {
        this.id = UUID.randomUUID().toString();
        this.containers = new ArrayList<Container>();

        long n = Math.round(Math.random() * max_containers);
        for (int i = 0; i < n; i++) {
            int load = 0;
            for (int j = 0; j < this.containers.size(); j++) {
                load += containers.get(j).getCpuUsage();
            }
            int cpuUsage = (int)Math.round(Math.random() * max_container_cpu_usage);
            if (load + cpuUsage <= 100) {
                this.containers.add(new Container(cpuUsage));
            }
            else break;
        }
    }

    // seeded node
    public Node(int seed) {
        this.id = UUID.randomUUID().toString();
        this.containers = new ArrayList<Container>();

        long n = new Random(seed).nextInt(max_containers);
        for (int i = 0; i < n; i++) {
            int load = getCpuUsage();
            int cpuUsage = new Random(seed + i).nextInt(max_container_cpu_usage);
            if (load + cpuUsage <= 100) {
                this.containers.add(new Container(cpuUsage));
            }
            else break;
        }
    }

    // seeded node with specified number of containers
    public Node(int containers, int seed) {
        this.id = UUID.randomUUID().toString();
        this.containers = new ArrayList<Container>();

        int cpuUsage = new Random(seed).nextInt(max_cpu_usage);
        if (containers > 0) {
            int[] cpuDistribution = RandomUtils.randomDistribution(containers, cpuUsage, seed);
            for (int i = 0; i < containers; i++) {
                this.containers.add(new Container(cpuDistribution[i]));
            }
        }
        else {
            this.containers.add(new Container(0));
        }
    }

    public int getCpuUsage() {
        int load = 0;
        for (int i = 0; i < this.containers.size(); i++) {
            load += containers.get(i).getCpuUsage();
        }
        return load;
    }

    public Container getMaxLoaded() {
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
