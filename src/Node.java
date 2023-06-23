import java.sql.Array;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

public class Node {
    public String id;
    private ArrayList<Container> containers;
    private final static int max_containers = 10;
    private final static int max_container_cpu_usage = 50;

    public Node(String id, ArrayList<Container> containers) {
        this.id = id;
        this.containers = containers;
    }

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
            String cid = UUID.randomUUID().toString();
            if (load + cpuUsage <= 100) {
                this.containers.add(new Container(cpuUsage));
            }
            else break;
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

    public void print() {
        // containers.forEach(container -> System.out.print(container.getCpuUsage() + "|"));
        System.out.print(containers.stream().map(container -> container.getCpuUsage()).collect(Collectors.toList()));
        System.out.println(" Load: " + this.getCpuUsage());
    }

    public ArrayList<Container> getContainers() {
        return containers;
    }
}
