# A Pipeline for Grid Maps

This repository contains the code and data for generating and evaluating grid maps for complex geographic data.

*/GridMapPipeline/* contains the newly proposed restructured pipeline (Meulemans, Computer Graphics Forum 2026, forthcoming). 
- It uses the */CmdMedialAxis/* program, for which a precompiled Windows version is available at */precompiled/CmdMedialAxis.exe*.
- For exploratory purposes, the *main.GUI* class is best used.
- Computing mosaic cartograms can be done via the *main.Cartogram* class. 
- A command-line version of the pipeline is also available through *main.Pipeline*, though not fully fledged for all settings. 
- The *evaluation* package describes all scripts that were used to generate the figures and tables in the paper. Specifically, running *evalution.Main* first computes all grid maps and cartograms (placed into /output/) and then computes figures and logs for all results (placed into /evaluation/).

*/GridMapCombiner/* contains a unified version of the Java code of the original pipeline by Meulemans, Sondag and Speckmann (https://doi.org/10.1109/TVCG.2020.3028953), including minor bug fixes.
- It uses the */MedialAxis/* program, for which a precompiled Windows version is available at */precompiled/sdg-voronoi-edges.exe*.
- This version also contains an *integration* package that aligns it with the setup for the new pipeline. Specifically, running *integration.Main* runs all scripts used in the evaluation of the new pipeline.

Both versions use GeometryCore (v1.4.0 or later), available at https://github.com/tue-alga/GeometryCore.