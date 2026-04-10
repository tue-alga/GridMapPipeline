package common.gridmath;

/**
 *
 * @author Wouter Meulemans
 */
public enum AdjacencyType {
    ROOKS, // share an edge
    BISHOPS, // share a vertex but not an edge
    QUEENS; // share a vertex or an edge
}
