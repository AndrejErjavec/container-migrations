package utils;

import java.util.Random;

public class RandomUtils {
    public static int[] randomDistribution(int bins, int sum, int seed) {
        int[] numbers = new int[bins];
        Random r = new Random(seed);

        if (sum == 0) {
            return new int[bins];
        }

        for (int i = 0; i < bins - 1; i++) {
            int num = r.nextInt(sum);
            numbers[i] = num;
            sum -= num;
        }

        numbers[bins - 1] = sum;

        return numbers;
    }

    public static int[] randomUniformDistribution(int bins, int sum, int seed) {
        int[] numbers = new int[bins];
        Random random = new Random(seed);

        int total = 0;
        for (int i = 0; i < bins - 1; i++) {
            numbers[i] = random.nextInt(0, (sum / bins)*2);
            total += numbers[i];
        }
        numbers[bins - 1] = sum - total;

        return numbers;
    }
}
