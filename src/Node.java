import java.sql.Array;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Node {
    private ArrayList<Container> containers;
    private final static int max_containers = 10;
    private final static int max_container_cpu_usage = 50;

    public Node(ArrayList<Container> containers) {
        this.containers = containers;
    }

    public Node() {
        this.containers = new ArrayList<Container>();

        long n = Math.round(Math.random() * max_containers);
        for (int i = 0; i < n; i++) {
            int load = 0;
            for (int j = 0; j < this.containers.size(); j++) {
                load += containers.get(j).getCpuUsage();
            }
            int cpu_usage = (int)Math.round(Math.random() * max_container_cpu_usage);
            if (load + cpu_usage <= 100) {
                this.containers.add(new Container(cpu_usage));
            }
            else break;
        }
    }

    public int getLoad() {
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

    public void print() {
        // containers.forEach(container -> System.out.print(container.getCpuUsage() + "|"));
        System.out.print(containers.stream().map(container -> container.getCpuUsage()).collect(Collectors.toList()));
        System.out.println(" Load: " + this.getLoad());
    }

    public ArrayList<Container> getContainers() {
        return containers;
    }
}
