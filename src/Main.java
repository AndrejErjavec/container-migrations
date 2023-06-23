public class Main {
    public static int NETWORK_SIZE = 5;

    public static void main(String[] args) {

        Network n = new Network(NETWORK_SIZE);
        n.generateMigrationPlanImproved();
        n.printMigrationPlan();
    }
}
