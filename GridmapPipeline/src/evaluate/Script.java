package evaluate;

/**
 *
 * @author Wouter Meulemans
 */
public abstract class Script {
    
    public boolean errored = false;
    
    public abstract void printCase();
    
    public abstract void run();
}
