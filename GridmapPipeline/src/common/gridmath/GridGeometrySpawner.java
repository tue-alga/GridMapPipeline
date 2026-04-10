package common.gridmath;

import common.gridmath.GridMath.Coordinate;
import common.gridmath.grids.HexagonGeometry;
import common.gridmath.grids.CustomGeometry;
import common.gridmath.grids.NestedSquares;
import common.gridmath.grids.SquareGeometry;
import common.gridmath.grids.StaggeredSquareGeometry;
import common.gridmath.grids.TriangleGeometry;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;
import nl.tue.geometrycore.geometryrendering.styling.Dashing;
import nl.tue.geometrycore.io.ipe.IPEWriter;

/**
 *
 * @author Wouter Meulemans
 */
public class GridGeometrySpawner {

    final String name;
    final Supplier<GridGeometry> supplier;

    public GridGeometrySpawner(Supplier<GridGeometry> supplier) {
        this.name = supplier.get().toString();
        this.supplier = supplier;
    }

    public GridGeometry spawn() {
        return supplier.get();
    }

    @Override
    public String toString() {
        return name;
    }

    public static GridGeometrySpawner[] grids = {
        new GridGeometrySpawner(() -> new SquareGeometry()),
        new GridGeometrySpawner(() -> new HexagonGeometry()),
        new GridGeometrySpawner(() -> TriangleGeometry.rightAngled()),
        new GridGeometrySpawner(() -> TriangleGeometry.equilateral()),
        new GridGeometrySpawner(() -> new StaggeredSquareGeometry()),
        new GridGeometrySpawner(() -> CustomGeometry.escherGeese()),
        new GridGeometrySpawner(() -> new NestedSquares(2)),
        new GridGeometrySpawner(() -> new NestedSquares(3)),
        new GridGeometrySpawner(() -> CustomGeometry.squaresAndTriangles())
    };

    public static GridGeometry inferFromString(String gridname) {
        String[] split = gridname.split("!");

        // derive base
        GridGeometry geom = null;
        for (GridGeometrySpawner sp : grids) {
            if (sp.name.equals(split[0])) {
                geom = sp.spawn();
                break;
            }
        }

        if (geom == null) {
            System.err.println("Warning: unrecognized gridbase " + split[0]);
            return null;
        }

        // apply transforms
        int id = 1;
        while (id < split.length) {
            switch (split[id]) {
                case "r":
                    geom.rotate(Double.parseDouble(split[id + 1]));
                    id += 2;
                    break;
                case "s":
                    geom.scale(Double.parseDouble(split[id + 1]), Double.parseDouble(split[id + 2]));
                    id += 3;
                    break;
                case "h":
                    geom.shear(Double.parseDouble(split[id + 1]), Double.parseDouble(split[id + 2]));
                    id += 3;
                    break;
                default:
                    System.err.println("Warning: unexpected grid transform " + split[id]);
                    id++;
                    break;
            }
        }

        return geom;
    }

    public static void main(String[] args) throws IOException {
        // print the possible base geometries
        IPEWriter write = IPEWriter.fileWriter(new File("gridgeometries.ipe"));
        write.initialize();

        for (GridGeometrySpawner ggs : grids) {
            GridGeometry gg = ggs.spawn();
            GridMath gm = new GridMath(gg);
            write.newPage(gg.getName() + "_base", gg.getName() + "_nbr");

            write.setLayer(gg.getName() + "_nbr");
            write.setStroke(Color.blue, 0.05, Dashing.SOLID);
            for (Coordinate delta : gm.unitVectors()) {
                for (int i = 0; i < gm.getPatternComplexity(); i++) {
                    Coordinate c = gm.origin().plus(i, 0).plus(delta);
                    write.draw(c.getBoundary());
                }
            }

            write.setStroke(Color.black, 0.05, Dashing.SOLID);
            write.setLayer(gg.getName() + "_base");
            for (int i = 0; i < gm.getPatternComplexity(); i++) {
                Coordinate c = gm.origin().plus(i, 0);
                write.draw(c.getBoundary());
            }
        }

        write.close();
    }

}
