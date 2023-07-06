package test;

public class RunTests {
    public static void main(String[] args) {
        System.out.println("Running tests...");
        DiffVsStddevTest.run();
        SingleVsMultiMigratrionsTest.run();
        NormalVsImprovedTest.run();
        System.out.println("Done!");
    }
}
