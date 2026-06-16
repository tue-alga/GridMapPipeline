# Gridmap

This repository contains code and example data for generating and evaluating gridmaps for complex geographic areas, based on 

A Simple Grid-Maps Pipeline: Restructured, Accelerated and Upgraded (W. Meulemans, Computer Graphics Forum, 2026). <https://doi.org/10.1111/cgf.70467>

Notes:
1. For the evaluation code as used in the paper, see version 2.0.0 at <https://doi.org/10.5281/zenodo.19497047>. Note that this also contains a precompiled version of CmdMedialAxis for Windows, which is compatible with the current version of the program.
2. For converting IPE files to PDF files, IPE (https://ipe.otfried.org/) is assumed to be installed at *C:/Program Files/ipe-7.2.24/bin/*. For another path to IPE, the code must be adapted and recompiled. However, for most standard use-cases, this conversion is likely not required.
3. GeometryCore, available at https://github.com/tue-alga/GeometryCore, is required. For SVG export to work correctly, you will need the head version of 16/06/2026 or later.

## Basic use of the program

Basic use of the program follows the workflow below to generate a grid map:

1. In the "General" tab: click "Load all" to load a ".wkt" file describing the input.
2. In the "Partition" tab: configure productivity and dilation as desired, then click "Execute".
3. In the "Deform" tab: configure the desired grid, by selecting a grid type from the dropdown menu; optionally, configure an affine transformation of the grid though shear, scale and rotate. Then click "Execute".
4. In the "Assign" tab: click "Execute".
5. In the "Combine" tab: optionally select a combination method, then click "Execute".
6. Return to the "General" tab: select the desired output formats, then click "Export result". 

Note that, saving a WKT file (either through "Save all" or "Export results"), stores the full result and can be used to load the exact state into the program at a later point in time.

## Input format (WKT)

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

