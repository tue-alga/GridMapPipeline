/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package integration;

/**
 *
 * @author wmeulema
 */
public class CmdLine {

    public static class InvalidArgumentsException extends Exception {

        public InvalidArgumentsException(String message) {
            super(message);
        }

    }

    public static boolean hasSwitch(String arg, String[] args) {
        return findArgument(arg, args) >= 0;
    }

    public static int findArgument(String arg, String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(arg)) {
                return i;
            }
        }
        return -1;
    }

    public static String findStringArgument(String arg, String[] args) throws InvalidArgumentsException {
        return findStringArgument(arg, 1, args);
    }

    public static String findStringArgument(String arg, String[] args, String defaultvalue) {
        return findStringArgument(arg, 1, args, defaultvalue);
    }

    public static String findStringArgument(String arg, int num, String[] args) throws InvalidArgumentsException {
        int i = findArgument(arg, args);
        if (i < 0) {
            throw new InvalidArgumentsException("Did not find required argument: " + arg);
        } else if (i + num >= args.length) {
            throw new InvalidArgumentsException("Found argument " + arg + " but not its value");
        } else {
            return args[i + num];
        }
    }

    public static String findStringArgument(String arg, int num, String[] args, String defaultvalue) {
        int i = findArgument(arg, args);
        if (i < 0) {
            return defaultvalue;
        } else if (i + num >= args.length) {
            return defaultvalue;
        } else {
            return args[i + num];
        }
    }

    public static int findIntegerArgument(String arg, String[] args) throws InvalidArgumentsException {
        return findIntegerArgument(arg, 1, args);
    }

    public static int findIntegerArgument(String arg, int num, String[] args) throws InvalidArgumentsException {
        String val = findStringArgument(arg, num, args);
        return Integer.parseInt(val);
    }

    public static double findDoubleArgument(String arg, String[] args) throws InvalidArgumentsException {
        return findDoubleArgument(arg, 1, args);
    }

    public static double findDoubleArgument(String arg, int num, String[] args) throws InvalidArgumentsException {
        String val = findStringArgument(arg, num, args);
        return Double.parseDouble(val);
    }

    public static double findDoubleArgument(String arg, int num, String[] args, double defaultvalue) {
        String val = findStringArgument(arg, num, args, null);
        if (val == null) {
            return defaultvalue;
        }
        return Double.parseDouble(val);
    }

}
