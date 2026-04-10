package arrange.main;

import java.util.Locale;

import arrange.gui.MainGUI;
import arrange.parameter.ParameterManager;
import integration.SiteMap;
import integration.Stage;
import java.io.File;

public class Hexastuff {

    public static void main(final String[] args) {
       // System.out.println("start");
        Locale.setDefault(new Locale("en", "US"));

        MainGUI gui = new MainGUI();
        if (args.length == 0) {
            gui.run(false);
        } else {
          //  System.out.println("1");
            ParameterManager.parseArguments(args);
            gui.run(true);
        }
    }
    
    public static void main(SiteMap sitemap, boolean hex, boolean exact, File outdir) {
       // System.out.println("start");
        Locale.setDefault(new Locale("en", "US"));

        MainGUI gui = new MainGUI();
        gui.run(sitemap, hex, exact, outdir);
        
        sitemap.stage = Stage.DEFORMED;
    }
}
