import utils.CsvUtils;

import java.util.ArrayList;

public class Main {
    public static int NETWORK_SIZE = 20;

    public static void main(String[] args) {
        Chain chain = new Chain();
        Network n = new Network(NETWORK_SIZE, chain);
        n.run();
        chain.print();


    }
}
