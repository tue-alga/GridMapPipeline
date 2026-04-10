package common;

/**
 *
 * @author Wouter Meulemans
 */
public class Stage {

    public static final byte EMPTY = 0;
    public static final byte INPUT = 1;
    public static final byte PARTITIONED = 2;
    public static final byte DEFORMED = 4;
    public static final byte ASSIGNED = 8;

    public static final byte ALL_STAGES = INPUT | PARTITIONED | DEFORMED | ASSIGNED;
    
    public static byte previous(byte stage) {
        return (byte) (stage / 2);
    }

    public static byte next(byte stage) {
        return (byte) (stage * 2);
    }
}
