package combine;

import combine.combinations.AgglomerativeCombination;
import combine.combinations.AgglomerativePartitionCombination;
import combine.combinations.AgnosticCombination;
import combine.combinations.AssignedCombination;
import common.SiteMap;
/**
 *
 * @author Wouter Meulemans
 */
public abstract class Combination {
    private final String name;

    public Combination(String name) {
        this.name = name;
    }

    public abstract void run(SiteMap map);

    @Override
    public String toString() {
        return name;
    }
    
    public abstract boolean requiresAssigned();
    
    public static Combination[] methods = {
        new AgnosticCombination(),
        new AssignedCombination(),
        new AgglomerativeCombination(),
        new AgglomerativePartitionCombination()
    };
}
