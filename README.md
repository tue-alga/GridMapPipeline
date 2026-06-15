# Gridmap
Contains the code and data for generating and evaluating gridmaps for complex data.

*/GridMapPipeline/* contains the newly proposed restructured pipeline (Meulemans, Computer Graphics Forum 2026, forthcoming). 
- It uses the */CmdMedialAxis/* program, for which a precompiled Windows version is available at */precompiled/CmdMedialAxis.exe*.
- For exploratory purposes, the *main.GUI* class is best used.
- Computing mosaic cartograms can be done via the *main.Cartogram* class. 
- A command-line version of the pipeline is also available through *main.Pipeline*, though not fully fledged for all settings. 
- The *evaluation* package describes all scripts that were used to generate the figures and tables in the paper. Specifically, running *evalution.Main* first computes all grid maps and cartograms (placed into /output/) and then computes figures and logs for all results (placed into /evaluation/).

*/GridMapCombiner/* contains a unified version of the Java code of the original pipeline by Meulemans, Sondag and Speckmann (https://doi.org/10.1109/TVCG.2020.3028953), including minor bug fixes.
- It uses the */MedialAxis/* program, for which a precompiled Windows version is available at */precompiled/sdg-voronoi-edges.exe*.
- This version also contains an *integration* package that aligns it with the setup for the new pipeline. Specifically, running *integration.Main* runs all scripts used in the evaluation of the new pipeline.

To run all experiments that are reported, precompiled versions (for Windows) are available in */precompiled/* and two batch scripts that run all scripts (*run-scripts.bat*, i.e., run all algorithms) and produce all outcomes (*run-outcomes.bat*, i.e., produce all tables and figures).
Note that these assume that IPE (https://ipe.otfried.org/) is installed at *C:/Program Files/ipe-7.2.24/bin/*. For another path to IPE, the code must be reconfigured and recompiled.

Both versions use GeometryCore, available at https://github.com/tue-alga/GeometryCore, and require version 1.4.0 (or later).

## Input format

The program uses a custom WKT ("well-known-text") format to read input and write (intermediate) outputs. Here is a brief sketch of this format; see example data files and/or io.WKT.java for details.

A full file looks roughly as the sketch below. 

```
STAGE 1
OUTLINES <K>
OUT <INDEX = 0>	<LABEL>	<X> <Y>	<R> <G> <B>
<X1> <Y1>	<X2> <Y2>	...	<Xn> <Yn>
SITECOUNT <S0>
SITE <OUTLINEINDEX = 0> <INDEX = 0>	<LABEL>	<X> <Y>	<R> <G> <B>	sites
... repeat S0 sites ...
SITE <OUTLINEINDEX = 0> <INDEX = S0-1>	<LABEL>	<X> <Y>	<R> <G> <B>	sites
... repeat K outlines ...
OUT <INDEX = {K-1}>	<LABEL>	<X> <Y>	<R> <G> <B>
<X1> <Y1>	<X2> <Y2>	...	<Xn> <Yn>
SITECOUNT <S{K-1}>
SITE <OUTLINEINDEX = K-1> <INDEX = 0>	<LABEL>	<X> <Y>	<R> <G> <B>	sites
... repeat S sites ...
SITE <OUTLINEINDEX = K-1> <INDEX = S{K-1}-1>	<LABEL>	<X> <Y>	<R> <G> <B>	sites
```

In the above, all <...> tags indicate values to be filled in appropriately, without the angular brackets. Note that the first line is wholly fixed; we treat the other parts in more detail below.
 
```
OUTLINES <K>
```

This second line indicates the number of outlines (polygons, with containing sites) the file describes.

```
OUT <INDEX>	<LABEL>	<X> <Y>	<R> <G> <B>
<X1> <Y1>	<X2> <Y2>	...	<Xn> <Yn>
SITECOUNT <S>
```

These three lines give a description of an outline. The first line lists an index, label, X&Y coordinate for this label, and an RGB color. Its second line describes the polygon as a sequence of X&Y coordinates, without repeating the first coordinate.
The third line indicates the number of sites contained within this outline.
What then follows is a sequence of lines, one for each site, to describe these sites contained within the outline.

```
SITE <OUTLINEINDEX> <INDEX>	<LABEL>	<X> <Y>	<R> <G> <B>	sites
```

This line represents a site: it repeats the outline index and has its own index, label, X&Y coordinate and RGB color.
Note that the last word on the line is "sites", typically fixed. This can be used to differentiate layers when using IPE outputs, but is only for advanced use.

After all sites of the outline have been listed, the next outline is described (if there is one).

Note that the sites are grouped with the outline that contains them and the polygons of these outlines are assumed to be interior-disjoint. They may share boundaries, for example, to represent a known subdivision.

Indices must be numbered incrementally, starting from 0. Coordinates can be specified via double precision. Each component of a color must be an integer in the range 0 to 255 (inclusive).
Labels can be any non-empty string that does not contain a tab; however, special characters may not render well.

Further, note that this format uses a mixture of tabs and spaces to separate the various values. Below, the same format is repeated but with [space] and [tab] used to indicate the separators explicitly.

```
STAGE[space]1
OUTLINES[space]<K>
OUT[space]<INDEX = 0>[tab]<LABEL>[tab]<X>[space]<Y>[tab]<R>[space]<G>[space]<B>
<X1>[space]<Y1>[tab]<X2>[space]<Y2>[tab]...[tab]<Xn>[space]<Yn>
SITECOUNT[space]<S0>
SITE[space]<OUTLINEINDEX = 0>[space]<INDEX = 0>[tab]<LABEL>[tab]<X>[space]<Y>[tab]<R>[space]<G>[space]<B>[tab]sites
... repeat S0 sites ...
SITE[space]<OUTLINEINDEX = 0>[space]<INDEX = S0-1>[tab]<LABEL>[tab]<X>[space]<Y>[tab]<R>[space]<G>[space]<B>[tab]sites
... repeat K outlines ...
OUT[space]<INDEX = K-1>[tab]<LABEL>[tab]<X>[space]<Y>[tab]<R>[space]<G>[space]<B>
<X1>[space]<Y1>[tab]<X2>[space]<Y2>[tab]...[tab]<Xn>[space]<Yn>
SITECOUNT[space]<S{K-1}>
SITE[space]<OUTLINEINDEX = K-1>[space]<INDEX = 0>[tab]<LABEL>[tab]<X>[space]<Y>[tab]<R>[space]<G>[space]<B>[tab]sites
... repeat S sites ...
SITE[space]<OUTLINEINDEX = K-1>[space]<INDEX = S{K-1}-1>[tab]<LABEL>[tab]<X>[space]<Y>[tab]<R>[space]<G>[space]<B>[tab]sites
```

