package evaluate;

import main.Cartogram;

/**
 *
 * @author Wouter Meulemans
 */
public class CartogramScript extends Script {

    private String[] args;

    public CartogramScript(String[] args) {
        this.args = args;
    }

    @Override
    public void printCase() {        
        for (String s : args) {
            if (s.startsWith("-")) {
                if (s != args[0]) {
                    System.out.println("");
                }
                System.out.print("   ");
            }
            System.out.print(" " + s);
        }
        System.out.println("");

    }

    @Override
    public void run() {
        errored = !Cartogram.run(args);
    }
}
