/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package integration;

import arrange.model.util.Random;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author wmeulema
 */
public class Main {

    public static void main(String[] args) throws IOException {

        pipelineScripts();
        //cartogramScripts();
    }

    private static void pipelineScripts() throws IOException {

        System.gc();
        Random.restart();

        List<Script> scripts = new ArrayList();

        String dataroot = "../Data/";
        String outroot = "../Output/";

        if (true) {

            String trialroot = outroot + "PartitionTrials/";

            int[] prods = {1, 5, 10, 20, 30};
            double[] dils = {1, 3, 5};
            int reps = 3;

            for (double dil : dils) {
                for (int prod : prods) {
                    String cs = dil + "-" + prod;
                    // Partition time trial
                    scripts.add(new Script(dataroot + "GB_constituencies.wkt",
                            reps,
                            dil, prod, false,
                            false,
                            Stage.PARTITIONED,
                            trialroot + "gb-ENDPOINTS-old-" + cs + ".txt",
                            trialroot + "gb-ENDPOINTS-old-" + cs + ".wkt",
                            trialroot + "gb-ENDPOINTS-old-" + cs + "/"
                    ));
                    scripts.add(new Script(dataroot + "NL_municipalities2017_main.wkt",
                            reps,
                            dil, prod, false,
                            false,
                            Stage.PARTITIONED,
                            trialroot + "nlmain-ENDPOINTS-old-" + cs + ".txt",
                            trialroot + "nlmain-ENDPOINTS-old-" + cs + ".wkt",
                            trialroot + "nlmain-ENDPOINTS-old-" + cs + "/"
                    ));
                }
            }
        }

        if (true) {
            // Cartogram time trial

            int reps = 3;
            String trialroot = outroot + "DeformTrials/";

            boolean[] hexs = {
                false,
                true
            };

            for (boolean hex : hexs) {

                String cs = hex ? "HEXAGON" : "SQUARE";

                scripts.add(new Script(dataroot + "GB_constituencies.wkt",
                        reps,
                        3.0, 15, false,
                        hex,
                        Stage.DEFORMED,
                        trialroot + "gb-old-" + cs + ".txt",
                        trialroot + "gb-old-" + cs + ".wkt",
                        trialroot + "gb-old-" + cs + "/"
                ));
                scripts.add(new Script(dataroot + "UK_constituencies.wkt",
                        reps,
                        3.0, 15, false,
                        hex,
                        Stage.DEFORMED,
                        trialroot + "uk-old-" + cs + ".txt",
                        trialroot + "uk-old-" + cs + ".wkt",
                        trialroot + "uk-old-" + cs + "/"
                ));
                scripts.add(new Script(dataroot + "NL_municipalities2017_main.wkt",
                        reps,
                        3.0, 15, false,
                        hex,
                        Stage.DEFORMED,
                        trialroot + "nlmain-old-" + cs + ".txt",
                        trialroot + "nlmain-old-" + cs + ".wkt",
                        trialroot + "nlmain-old-" + cs + "/"
                ));
                scripts.add(new Script(dataroot + "NL_municipalities2017.wkt",
                        reps,
                        3.0, 15, false,
                        hex,
                        Stage.DEFORMED,
                        trialroot + "nl-old-" + cs + ".txt",
                        trialroot + "nl-old-" + cs + ".wkt",
                        trialroot + "nl-old-" + cs + "/"
                ));
            }
        }
        
        if (true) {
            int reps = 3;
            String trialroot = outroot + "FullPipeline/";

            boolean[] hexs = {
                false,
                true
            };

            for (boolean hex : hexs) {

                String cs = hex ? "HEXAGON" : "SQUARE";

                scripts.add(new Script(dataroot + "UK_constituencies.wkt",
                        reps,
                        3.0, 15, false,
                        hex,
                        Stage.ASSIGNED,
                        trialroot + "uk-old-" + cs + ".txt",
                        trialroot + "uk-old-" + cs + ".wkt",
                        trialroot + "uk-old-" + cs + "/"
                ));
                scripts.add(new Script(dataroot + "NL_municipalities2017.wkt",
                        reps,
                        3.0, 10, false,
                        hex,
                        Stage.ASSIGNED,
                        trialroot + "nl-old-" + cs + ".txt",
                        trialroot + "nl-old-" + cs + ".wkt",
                        trialroot + "nl-old-" + cs + "/"
                ));
            }
        }
        
        System.out.println("---- RUNNING SCRIPTS ----");
        int i = 1;
        int err = 0;
        for (Script script : scripts) {
            System.out.println("Script " + i + " / " + scripts.size());
            script.run();
            if (script.errored) {
                err++;
                System.out.println("  << EXCEPTION THROWN >>");
            }
            Random.restart();
            System.gc();
            i++;
        }
        System.out.println("---- DONE WITH SCRIPTS ----");
        System.out.println("  Errored scripts: " + err);
    }

    private static void cartogramScripts() {

        System.gc();
        Random.restart();

        String dataroot = "../Data/";
        String outroot = "../Output/Cartograms/";

        List<String[]> scripts = new ArrayList();

        scripts.add(new String[]{
            "-input", dataroot + "USA/usa.ipe",
            "-weights", dataroot + "USA/business_without_employees.tsv", "0.0001",
            "-grid", "SQUARE",
            "-output", outroot + "US_businesses-old.ipe",
            "-log", outroot + "US_businesses-old.txt",
            "-pdf", "C:/Program Files/ipe-7.2.24/bin/",
            "-original"
        });

        scripts.add(new String[]{
            "-input", dataroot + "USA/usa-starbucks.ipe",
            "-weights", dataroot + "USA/starbucks.tsv", "1",
            "-grid", "HEXAGON",
            "-output", outroot + "US_starbucks-old.ipe",
            "-log", outroot + "US_starbucks-old.txt",
            "-pdf", "C:/Program Files/ipe-7.2.24/bin/",
            "-dark",
            "-original"
        });

        scripts.add(new String[]{
            "-input", dataroot + "EU/europe-noislands.ipe",
            "-weights", dataroot + "EU/population.tsv", "0.000008",
            "-grid", "HEXAGON",
            "-output", outroot + "EU_population-old.ipe",
            "-log", outroot + "EU_population-old.txt",
            "-pdf", "C:/Program Files/ipe-7.2.24/bin/",
            "-original"
        });        
        
        int num = 1;
        for (String[] script : scripts) {
            System.out.println("Script " + num + " / " + scripts.size());
            Cartogram.run(script);
            System.out.println("  Done");
            System.gc();
            Random.restart();
            num++;
        }
    }
}
