package arrange.util;

/**
 * Wrapper for java.util.Random to give better control over the seed used in the
 * random number generator.
 *
 * @author Rafael Cano
 */
public class Random {

    /**
     * These variables should be set manually to determine whether the behavior
     * of the random number generator is the same in all runs. To get
     * non-deterministic behavior, 'random' must be initialized with the
     * no-argument constructor.
     */
    private static final long SEED = 1;
    private static final java.util.Random random = new java.util.Random(SEED);

    private Random() {
    }

    public static void restart() {
        random.setSeed(SEED);
    }

    public static long getSeed() {
        return SEED;
    }

    public static boolean nextBoolean() {
        return random.nextBoolean();
    }

    public static void nextBytes(byte[] bytes) {
        random.nextBytes(bytes);
    }

    public static double nextDouble() {
        return random.nextDouble();
    }

    public static float nextFloat() {
        return random.nextFloat();
    }

    public static double nextGaussian() {
        return random.nextGaussian();
    }

    public static int nextInt() {
        return random.nextInt();
    }

    public static int nextInt(int n) {
        return random.nextInt(n);
    }

    public static long nextLong() {
        return random.nextLong();
    }
}