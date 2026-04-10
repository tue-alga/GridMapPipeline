/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package integration;

import arrange.main.Hexastuff;
import arrange.model.util.Random;
import assign.GridMapLP;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import partition.GridMapPartioner;

/**
 *
 * @author wmeulema
 */
public class Script {
    private final String input;
    private final String stdout;
    private final String wktoutput;
    private final String outdir;
    private final byte stopAfter;
    private final double dilation;
    private final int productivity;
    private final boolean pretest;
    private final boolean hex;
    private int repetitions;
    public boolean errored = false;

    public Script(String input, int repetitions, double dilation, int productivity, boolean pretest, boolean hex, byte stopAfter, String stdout, String wktoutput, String outdir) {
        this.input = input;
        this.wktoutput = wktoutput;
        this.outdir = outdir;
        this.stdout = stdout;
        this.repetitions = repetitions;
        this.stopAfter = stopAfter;
        this.dilation = dilation;
        this.productivity = productivity;
        this.hex = hex;
        this.pretest = pretest;
    }

    public SiteMap run() throws IOException {

        PrintStream oldps = System.out;
        if (stdout != null) {
            try {
                File f = new File(stdout);
                f.getParentFile().mkdirs();
                System.setOut(new PrintStream(f));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Script.class.getName()).log(Level.SEVERE, null, ex);
                System.setOut(oldps);
            }
        }

        File dir = new File(outdir);
        dir.mkdirs();
        SiteMap map = null;
        try {
            do {
                map = WKT.read(new File(input));

                // NB: dilation is interpreted inversely in the old pipeline
                GridMapPartioner.main(map, 1.0 / dilation, productivity, pretest, dir);
                if (stopAfter > Stage.PARTITIONED) {
                    Hexastuff.main(map, hex, true, dir);
                    if (stopAfter > Stage.DEFORMED) {
                        GridMapLP.main(map, hex, dir);
                    }
                }
                repetitions--;
            } while (repetitions > 0);

            File f = new File(wktoutput);
            f.getParentFile().mkdirs();
            WKT.write(f, map);

            Stopwatch.printAndClear();

        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace(System.out);
            errored = true;
        }

        if (stdout != null) {
            System.setOut(oldps);
        }

        return map;
    }

}
