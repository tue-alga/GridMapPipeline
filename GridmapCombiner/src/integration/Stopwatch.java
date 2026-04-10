/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package integration;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author wmeulema
 */
public class Stopwatch {

    private final String name;
    private long total = 0;
    private long start = 0;
    private int count = 0;

    private Stopwatch(String name) {
        this.name = name;
    }

    public Stopwatch start() {
        start = System.currentTimeMillis();
        return this;
    }

    public long stop() {
        long end = System.currentTimeMillis();
        count++;
        total += end - start;
        return end - start;
    }

    public long getTotalTime() {
        return total;
    }

    public int getCount() {
        return count;
    }

    public static List<Stopwatch> watches = new ArrayList();

    public static Stopwatch get(String name) {
        for (Stopwatch w : watches) {
            if (w.name.equals(name)) {
                return w;
            }
        }
        Stopwatch w = new Stopwatch(name);
        watches.add(w);
        return w;
    }

    public static void clear() {
        watches.clear();
    }

    public static void printAndClear() {
        printAll();
        clear();
    }

    public static void printAll() {
        System.out.println("------ TIMINGS -----------------------------");

        int longestname = 4;
        int longesttime = 5;
        int longestcount = 7;
        for (Stopwatch w : watches) {
            longestname = Math.max(longestname, w.name.length());
            longesttime = Math.max(longesttime, Long.toString(w.total).length());
            longestcount = Math.max(longestcount, Integer.toString(w.count).length());
        }

        printLeftAlign("NAME", longestname);
        System.out.print("  ");
        printLeftAlign("TOTAL", longesttime);
        System.out.print("  ");
        printLeftAlign("COUNT", longestcount);
        System.out.print("  ");
        printLeftAlign("AVERAGE", longesttime);
        System.out.println("");

        for (Stopwatch w : watches) {
            printLeftAlign(w.name, longestname);
            System.out.print("  ");
            printRightAlign("" + w.total, longesttime);
            System.out.print("  ");
            printRightAlign("" + w.count, longestcount);
            System.out.print("  ");
            if (w.count == 0) {
                printRightAlign("-", longesttime);
            } else {
                printRightAlign(w.total / w.count + "", longesttime);
            }
            System.out.println("");
        }
        System.out.println("--------------------------------------------");

    }

    private static void printLeftAlign(String s, int w) {
        System.out.print(s);
        w -= s.length();
        while (w > 0) {
            System.out.print(" ");
            w--;
        }
    }

    private static void printRightAlign(String s, int w) {
        w -= s.length();
        while (w > 0) {
            System.out.print(" ");
            w--;
        }
        System.out.print(s);
    }

}
