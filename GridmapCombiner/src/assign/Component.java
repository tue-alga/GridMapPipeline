/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assign;

import java.util.List;

/**
 *
 * @author msondag
 */
public class Component {
    
    public List<Site> sites;
    List<Cell> cells;

    public Component(List<Site> sites, List<Cell> cells) {
        this.sites = sites;
        this.cells = cells;
    }
    
    
}
