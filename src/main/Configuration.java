package main;

import main.types.TestCase;

public class Configuration {
    public int NETWORK_SIZE = 256;
    public int CONTAINERS = 1000;
    public int BLOCKS = 1000;
    public int BLOCK_DURATION = 200; // ms
    public int MIN_CONTAINER_CPU = 10;
    public int MAX_CONTAINER_CPU = 90;
    public TestCase testCase = TestCase.AVERAGE;
    public boolean ONLINE = false;
    public int CONTAINERS_PER_BLOCK = 10;
}
