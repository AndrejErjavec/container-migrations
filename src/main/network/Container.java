package main.network;

import java.util.UUID;

public class Container {
    private int cpuUsage;
    public String id;

    public Container (int cpuUsage) {
        this.id = UUID.randomUUID().toString();
        this.cpuUsage = cpuUsage;
    }

    public int getCpuUsage() {
        return this.cpuUsage;
    }
}
