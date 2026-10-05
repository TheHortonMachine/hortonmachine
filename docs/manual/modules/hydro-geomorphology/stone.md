# Stone

STONE simulates the three-dimensional trajectories of rockfalls on a digital elevation model (Guzzetti et al., 2002). The module is a port of the GRASS GIS r.stone addon, itself a port of the original STONE code by Fausto Guzzetti and Massimiliano Alvioli.

A rockfall is simulated as a point-like boulder that falls, bounces and rolls on the terrain:

- the DEM is used as a **triangular network**: each square between four cell centers is split into two triangles, on which the motion is computed;
- the boulders start from the **center of the source cells**, with the start velocity, in the direction of steepest descent towards the neighbouring cells;
- a **flying** boulder follows a parabola until it hits the terrain. At the **bounce** it keeps the given percentages of its velocity normal and tangential to the terrain, the normal and tangential **restitution** coefficients;
- after a short and slow bounce (two impacts closer than 3 m, at less than 5 m/s) the boulder starts **rolling**, slowed down by the rolling **friction** coefficient, and flies again where the terrain drops away under it;
- a boulder **stops** when its velocity falls under the stop velocity, when it reaches a stop cell (value -1 in the sources), when it bounces on a cell with a normal restitution of 0 (as fallen into water), or when it leaves the DEM.

The start direction of each boulder, and the coefficients of each bounce and roll, are drawn at random around the values of the maps, with the given distributions and variabilities. So the boulders thrown from the same cell follow different trajectories, and the results have a probabilistic meaning. The random numbers of each source cell come from a fixed seed and the position of the cell: the same input always gives the same result, and the trajectories of a source don't depend on the other sources.

The results are:

- the **counter**, the number of trajectories crossing each cell. With one boulder per source it shows where single random falls from all the sources go; more boulders per source also show the rarer long runouts, and give the counts the meaning of a frequency;
- the **maximum velocity** of the boulders in each cell;
- the **maximum height** of the trajectories over the ground in each cell, useful for example to size protection barriers;
- optionally, the 3D lines of a **sample of the trajectories**, chosen evenly over all the sources, with their maximum velocity and height. The flights are drawn along their exact parabola every meter.

The input maps can be prepared from as little as a DEM with the [StoneInputs](stoneinputs.md) module. Example values of the coefficients for the lithological classes of Italy are in Alvioli et al. (2021), and listed on that page.

## The step

The boulders are moved and their positions recorded about every **step** meters (`pStep`), as in the original model, where the step was fixed to 5 m. The step is limited to the cell size, since larger steps let the boulders jump over cells. The results depend on the step: finer steps give longer runouts, mostly because with coarse steps more boulders are lost when they pass from a triangle to the next. Keep the default of 5 m to compare the results with those of the original model and of the literature.

## Differences from r.stone

- The counter counts all the cells crossed between two recorded points, while r.stone counts only the cells containing a point: with a 5 m step on 10 m cells, about 10% more counts. The cells crossed between two points get the velocity and the height interpolated between them.
- The maximum velocity and height are real numbers, r.stone truncates them to integers.
- The random numbers differ, so the single trajectories differ: the results are equivalent in their statistics, as the results of two runs of r.stone with different random numbers.

```{include} ../generated/Stone.md
```
