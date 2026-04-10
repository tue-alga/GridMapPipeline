/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package partition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author msondag
 */
public class Utility {

    private static final boolean DEBUG = true;

    private static HashMap<String, Long> timingStart = new HashMap();
    private static HashMap<String, Long> timing = new HashMap();

    public static String executeCommandLine(List<String> commandLineString) {
        try {
            ProcessBuilder pb = new ProcessBuilder(commandLineString);
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
            return builder.toString();
        } catch (IOException ex) {
            Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
        }
        throw new IllegalStateException(commandLineString + " DId not execute properly");
    }

    public static void debug(String debugMessage) {
        if (DEBUG) {
            System.out.println("debugMessage = " + debugMessage);
        }
    }

    public static void debug(boolean printString, String debugMessage) {
        if (printString) {
            System.out.println("debugMessage = " + debugMessage);
        }
    }

    public static void startTimer(String timeName) {
        timingStart.put(timeName, System.currentTimeMillis());
    }

    public static void endTimer(String timeName) {
        //sum the previous value with the current value.
        long time = System.currentTimeMillis() - timingStart.get(timeName);
        timing.put(timeName, timing.getOrDefault(timeName, 0l) + time);
    }

    public static void printTimer() {
        for (String key : timing.keySet()) {
            System.out.println(key + ": " + timing.get(key));
        }
    }

    public static int counter = 0;

    public static int getNextCount() {
        counter++;
        return counter;
    }
}
