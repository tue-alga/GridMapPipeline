/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assign;

import java.util.List;
import com.quantego.clp.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Point set matching algorithm using a linear program.
 *
 * @author msondag
 */
public class LinearProgram {

    //Holds the cells and sites we are going to match
    Component component;
    CLP model;

    List<CLPVariable> variables = new ArrayList();
    //all variables that correspond to the site
    HashMap<Site, List<CLPVariable>> siteVariables = new HashMap();
    //all variables that hold the coordinate
    HashMap<Cell, List<CLPVariable>> cellVariables = new HashMap();

    //holds for each flow variable between a cell and a site, to which site it maps
    HashMap<CLPVariable, Site> siteMapping = new HashMap();
    //holds for each flow variable between a cell and a site, to which cite it maps
    HashMap<CLPVariable, Cell> cellMapping = new HashMap();

    /**
     * Sets up a linear program for the given component.
     *
     * @param component
     */
    public LinearProgram(Component component) {
        this.component = component;
        setupLP();
    }

    /**
     * Initializes all the variables
     */
    private void setupLP() {
        model = new CLP();
        createVariables(component.sites, component.cells);
        addSiteConstraint();
        addCellConstraint();
        addOptimization();
    }

    /**
     * Creates variables for the cells and sites.
     *
     * @param sites
     * @param gridCells
     */
    private void createVariables(List<Site> sites, List<Cell> gridCells) {
        //Each site can be assigned to a cell in the grid.
        for (Site s : sites) {
            for (Cell c : gridCells) {

                CLPVariable flow = model.addVariable();
                //flow between 0 and 1
                flow.bounds(0.0, 1.0);
                //name it and add it
                flow.name(s.label + ";" + c);
                variables.add(flow);

                //keep track of it
                List cVarList = cellVariables.getOrDefault(c, new ArrayList());
                cVarList.add(flow);
                cellVariables.put(c, cVarList);

                List sVarList = siteVariables.getOrDefault(s, new ArrayList());
                sVarList.add(flow);
                siteVariables.put(s, sVarList);

                siteMapping.put(flow, s);
                cellMapping.put(flow, c);
            }
        }
    }

    /**
     * Creates the constraint that every site maps to exactly 1 cell.
     */
    private void addSiteConstraint() {
        //every site maps to a total (exactly) one cell.
        for (Collection<CLPVariable> variableList : siteVariables.values()) {
            HashMap<CLPVariable, Double> lhs = new HashMap<>();
            for (CLPVariable v : variableList) {
                lhs.put(v, 1.0);
            }
            model.addConstraint(lhs, CLPConstraint.TYPE.EQ, 1);
        }
    }

    /**
     * Creates the constraint that every cell has at most one site mapped to it.
     */
    private void addCellConstraint() {
        //every cell has at most one site mapped to it.
        for (Collection<CLPVariable> variableList : cellVariables.values()) {
            HashMap<CLPVariable, Double> lhs = new HashMap<>();
            for (CLPVariable v : variableList) {
                lhs.put(v, 1.0);
            }
            model.addConstraint(lhs, CLPConstraint.TYPE.LEQ, 1);
        }
    }

    /**
     * Adds the optimization term: Minimize sum of squared distances.
     */
    private void addOptimization() {
        //cost for a site s to be assigned to cell c is the squared squaredDistance between their centroids.
        HashMap<CLPVariable, Double> objective = new HashMap<>();
        for (CLPVariable v : variables) {
            Site site = getSite(v);
            Cell c = getCell(v);

            //cell can be a square or hexagon, need to calculate the squaredDistance
            double squaredDistance = c.squaredDistance(site.c);

            objective.put(v, squaredDistance);
        }

        //minimize the sum of squared distances
        model.addObjective(objective, 0.0);
    }

    /**
     * Solve the linear program and return the cost.
     *
     * @return
     */
    public double solveLP() {
        model.minimize();
        for (CLPVariable v : variables) {
            if (model.getSolution(v) == 1) {
                //there is a mapping from the site to the cell.
                Site site = getSite(v);
                Cell cell = getCell(v);
                site.assigned = cell.mosaicCell;
                cell.label = site.label;
                cell.color = site.color;
                cell.province = site.province;

            } else if (model.getSolution(v) > 0) {
                System.out.println("Non-integer solutions");
            }
        }
        return model.getObjectiveValue();
    }

    /**
     * Returns the site associated to a given variable,
     *
     * @param v
     * @return
     */
    private Site getSite(CLPVariable v) {
        return siteMapping.get(v);
    }

    /**
     * Returns the cell associated to a given variable.
     *
     * @param v
     * @return
     */
    private Cell getCell(CLPVariable v) {
        return cellMapping.get(v);
    }

}
