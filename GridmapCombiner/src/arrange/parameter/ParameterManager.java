package arrange.parameter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class ParameterManager {

    private static HashMap<String, CommandLineArgument> argumentsMap = initializeArgumentsMap();

    private ParameterManager() {
    }

    public static void parseArguments(String[] args) {
        List<String> argumentList = Arrays.asList(args);
        //print arguments
//        for (String s : argumentList) {
//            System.out.print(s + " ");
//        }
//        System.out.println("");
        ListIterator<String> it = argumentList.listIterator();
        while (it.hasNext()) {
            String arg = it.next();
            try {
                CommandLineArgument argument = argumentsMap.get(arg);
                if (argument == null) {
                    throw new UnknownArgumentException(arg);
                }
                argument.parse(it);
            } catch (ArgumentException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }
        // System.out.println("2");
    }

    private static HashMap<String, CommandLineArgument> initializeArgumentsMap() {
        argumentsMap = new HashMap<>(10);
        for (Class<?> c : ParameterManager.class.getClasses()) {
            try {
                Class.forName(c.getName());
            } catch (ClassNotFoundException e) {
                System.err.println("Bizarre error, abort, abort!");
                System.exit(1);
            }
        }
        return argumentsMap;
    }

    private static void printArgumentDescriptions() {
        for (Class<?> c : ParameterManager.class.getClasses()) {
            try {
                String groupDescription = (String) c.getDeclaredMethod("getDescription").invoke(null);
                System.out.println(groupDescription);
                for (Field f : c.getDeclaredFields()) {
                    f.setAccessible(true);
                    if (f.get(null) instanceof CommandLineArgument) {
                        CommandLineArgument cmdArg = (CommandLineArgument) f.get(null);
                        String[] args = cmdArg.getArguments();
                        String description = cmdArg.getDescription();
                        System.out.print("   ");
                        for (String s : args) {
                            System.out.print(s + " ");
                        }
                        System.out.println();
                        System.out.println("      " + description);
                    }
                }
                System.out.println();
            } catch (IllegalArgumentException | IllegalAccessException | SecurityException | NoSuchMethodException | InvocationTargetException ex) {
                Logger.getLogger(ParameterManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static class Application {

        // Description
        private static final String groupDescription = "General parameters to control the application";
        // Arguments
        private static final CommandLineArgument app_print_help
                                                 = new CommandLineArgument(new String[]{"-app-print-help", "-help", "-h"}, "Prints this information") {
            @Override
            public void parse(ListIterator<String> it) throws ArgumentException {
                printArgumentDescriptions();
                System.exit(0);
            }
        };
        private static final CommandLineArgument map_file_name
                                                 = new CommandLineArgument("-map", "Loads an input map file") {
            @Override
            public void parse(ListIterator<String> it) throws ArgumentException {
                Application.mapFileName = parseString(it);
            }
        };
        private static final CommandLineArgument data_file_name
                                                 = new CommandLineArgument("-data", "Loads an input data file") {
            @Override
            public void parse(ListIterator<String> it) throws ArgumentException {
                Application.dataFileName = parseString(it);
            }
        };
        private static final CommandLineArgument mosaic_file_name
                                                 = new CommandLineArgument("-mosaic", "Loads an initial mosaic drawing") {
            @Override
            public void parse(ListIterator<String> it) throws ArgumentException {
                Application.mosaicFileName = parseString(it);
            }
        };
        private static final CommandLineArgument color_file_name
                                                 = new CommandLineArgument("-color", "Loads an input color file") {
            @Override
            public void parse(ListIterator<String> it) throws ArgumentException {
                Application.colorFileName = parseString(it);
            }
        };
        private static final CommandLineArgument color_map
                                                 = new CommandLineArgument("-autocolor", "Automatically color given map") {
            @Override
            public void parse(ListIterator<String> it) throws ArgumentException {
                Application.colorMap = true;
            }
        };
        private static final CommandLineArgument ipe_file_name
                                                 = new CommandLineArgument("-ipe", "Exports cartogram to ipe file") {
            @Override
            public void parse(ListIterator<String> it) throws ArgumentException {
                Application.ipeFileName = parseString(it);
            }
        };

        private static final CommandLineArgument stats_file_name
                                                 = new CommandLineArgument("-stats", "Prints stats line to file") {
            @Override
            public void parse(ListIterator<String> it) throws ArgumentException {
                Application.statsFileName = parseString(it);
            }
        };
        private static final CommandLineArgument mosaic_resolution
                                                 = new CommandLineArgument("-resolution", "Average number of tiles per region") {
            @Override
            public void parse(ListIterator<String> it) throws ArgumentException {
                Application.mosaicResolution = parseDouble(it);
            }
        };
        private static final CommandLineArgument unit_data
                                                 = new CommandLineArgument("-unit", "Numerical data represented by one tile (only active if -resolution is not set)") {
            @Override
            public void parse(ListIterator<String> it) throws ArgumentException {
                Application.unitData = parseDouble(it);
            }
        };
        private static final CommandLineArgument vornoi_enabled
                                                 = new CommandLineArgument("-vornoi", "Paramater that uses a vornoi tesselation on the input kml data file)") {
            @Override
            public void parse(ListIterator<String> it) throws ArgumentException {
                Application.vornoiEnabled = true;
            }
        };

        private static final CommandLineArgument hexagonal_grid
                                                 = new CommandLineArgument("-hexagonal", "Use a hexagonal grid") {
            @Override
            public void parse(ListIterator<String> it) throws ArgumentException {
                Application.gridType = GridType.HEXAGONAL;
            }
        };
        private static final CommandLineArgument square_grid
                                                 = new CommandLineArgument("-square", "Use a square grid") {
            @Override
            public void parse(ListIterator<String> it) throws ArgumentException {
                Application.gridType = GridType.SQUARE;
            }
        };

        private static final CommandLineArgument exact_tile_amount
                                                 = new CommandLineArgument("-exact", "Each region will have the exact amount of tiles, adjacencies might be incorrect and holes might be introduced.") {
            @Override
            public void parse(ListIterator<String> it) throws ArgumentException {
                Application.exactTileAmount = true;
            }
        };

        // Parameters
        private static String mapFileName = null;
        private static String dataFileName = null;
        private static String mosaicFileName = null;
        private static String colorFileName = null;
        private static boolean colorMap = false;
        private static String ipeFileName = null;
        private static String statsFileName = null;
        private static Double mosaicResolution = null;
        private static Double unitData = null;
        private static GridType gridType = null;
        private static boolean vornoiEnabled = false;
        private static boolean exactTileAmount = false;

        public static String getDescription() {
            return groupDescription;
        }

        public static String getMapFileName() {
            return mapFileName;
        }

        public static String getDataFileName() {
            return dataFileName;
        }

        public static String getMosaicFileName() {
            return mosaicFileName;
        }

        public static String getColorFileName() {
            return colorFileName;
        }

        public static boolean getColorMap() {
            return colorMap;
        }

        public static boolean getVornoiEnabled() {
            return vornoiEnabled;
        }

        public static String getIpeFileName() {
            return ipeFileName;
        }

        public static String getStatsFileName() {
            return statsFileName;
        }

        public static Double getMosaicResolution() {
            return mosaicResolution;
        }

        public static Double getUnitData() {
            return unitData;
        }

        public static GridType getGridType() {
            return gridType;
        }

        public static boolean getExactTiles() {
            return exactTileAmount;
        }

        public enum GridType {

            HEXAGONAL, SQUARE;
        }

        private Application() {
        }
    }

    private static abstract class CommandLineArgument {

        private final String[] argument;
        private final String description;

        @SuppressWarnings("LeakingThisInConstructor")
        public CommandLineArgument(String argument, String description) {
            this.argument = new String[]{argument};
            this.description = description;
            ParameterManager.argumentsMap.put(argument, this);
        }

        @SuppressWarnings("LeakingThisInConstructor")
        public CommandLineArgument(String[] argumentArray, String description) {
            this.argument = argumentArray;
            this.description = description;
            for (String arg : argumentArray) {
                ParameterManager.argumentsMap.put(arg, this);
            }
        }

        public final String[] getArguments() {
            return argument;
        }

        public final String getDescription() {
            return description;
        }

        public abstract void parse(ListIterator<String> it) throws ArgumentException;
    }

    private static int parseInt(ListIterator<String> it) throws ArgumentException {
        int i = 0;
        try {
            i = Integer.parseInt(it.next());
        } catch (NumberFormatException e) {
            it.previous();
            throw new ArgumentTypeException("integer", it.previous());
        } catch (NoSuchElementException e) {
            throw new MissingArgumentException("integer", it.previous());
        }
        return i;
    }

    private static double parseDouble(ListIterator<String> it) throws ArgumentException {
        double d = 0.0;
        try {
            d = Double.parseDouble(it.next());
        } catch (NumberFormatException e) {
            it.previous();
            throw new ArgumentTypeException("double", it.previous());
        } catch (NoSuchElementException e) {
            it.previous();
            throw new MissingArgumentException("double", it.previous());
        }
        return d;
    }

    private static String parseString(ListIterator<String> it) throws ArgumentException {
        String s;
        try {
            s = it.next();
            if (s.startsWith("-")) {
                it.previous();
                throw new ArgumentTypeException("string", it.previous());
            }
        } catch (NoSuchElementException e) {
            throw new MissingArgumentException("string", it.previous());
        }
        return s;
    }
}
