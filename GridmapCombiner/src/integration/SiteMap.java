/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package integration;

import arrange.model.Cartogram.MosaicCartogram.Coordinate;
import arrange.parameter.ParameterManager.Application.GridType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;

/**
 *
 * @author wmeulema
 */
public class SiteMap {

    public List<Outline> outlines = new ArrayList();
    public GridType gridType = null;
    public byte stage = Stage.EMPTY;

    public void revertToStage(byte stage) {
        while (this.stage > stage) {
            switch (this.stage) {
                case Stage.INPUT:
                    outlines.clear();
                    break;
                case Stage.PARTITIONED:
                    for (Outline o : outlines) {
                        o.partitions.clear();
                    }
                    for (Site s : sites()) {
                        s.setPartition(null);
                    }
                    break;
                case Stage.DEFORMED:
                    for (Partition p : partitions()) {
                        p.mosaic = null;
                    }
                    gridType = null;
                    break;
                case Stage.ASSIGNED:
                    for (Site s : sites()) {
                        s.setCell(null);
                    }
                    break;
            }

            this.stage = Stage.previous(this.stage);
        }
    }
    
    public Outline findOutline(Vector v) {
        for (Outline p : outlines) {
            if (p.contains(v)) {
                return p;
            }
        }
        return null;
    }

    public Iterable<Site> sites() {
        return () -> new Iterator<Site>() {
            Iterator<Outline> outline_it = outlines.iterator();
            Outline curr = outline_it.hasNext() ? outline_it.next() : null;
            int site = 0;

            @Override
            public boolean hasNext() {
                // search for next
                while (curr != null) {
                    if (site < curr.sites.size()) {
                        return true;
                    }
                    site = 0;
                    if (outline_it.hasNext()) {
                        curr = outline_it.next();
                    } else {
                        return false;
                    }
                }
                return false;
            }

            @Override
            public Site next() {
                Site s = curr.sites.get(site);
                site++;
                return s;
            }
        };
    }

    public Iterable<Partition> partitions() {
        return () -> new Iterator<Partition>() {
            Iterator<Outline> outline_it = outlines.iterator();
            Outline curr = outline_it.hasNext() ? outline_it.next() : null;
            int partition = 0;

            @Override
            public boolean hasNext() {
                // search for next
                while (curr != null) {
                    if (partition < curr.partitions.size()) {
                        return true;
                    }
                    partition = 0;
                    if (outline_it.hasNext()) {
                        curr = outline_it.next();
                    } else {
                        return false;
                    }
                }
                return false;
            }

            @Override
            public Partition next() {
                Partition p = curr.partitions.get(partition);
                partition++;
                return p;
            }
        };
    }

    public Iterable<Coordinate> cells() {
        return () -> new Iterator<Coordinate>() {
            Iterator<Partition> partition_it = partitions().iterator();
            Partition curr = partition_it.hasNext() ? partition_it.next() : null;
            Iterator<Coordinate> coord_it = curr == null ? null : curr.mosaic.iterator();

            @Override
            public boolean hasNext() {
                // search for next
                while (curr != null) {
                    if (coord_it.hasNext()) {
                        return true;
                    }
                    if (partition_it.hasNext()) {
                        curr = partition_it.next();
                        coord_it = curr.mosaic.iterator();
                    } else {
                        return false;
                    }
                }
                return false;
            }

            @Override
            public Coordinate next() {
                return coord_it.next();
            }
        };
    }
}
