---
orphan: true
---

# Rockfall Viewer

The Rockfall Viewer runs the [Stone](../modules/hydro-geomorphology/stone.md) rockfall model and shows its results in 3D, on the terrain of the DEM. Boulders can also be thrown from any cell clicked on the terrain, to see all their trajectories.

## The main window

:::{figure} ../images/apps/rockfall/stone_simulation.png
:alt: The Rockfall Viewer after a simulation
:width: 100%
:align: center

The counter of a simulation draped on the terrain, with its legend at the bottom of the map.
:::

On the left are the input maps, the parameters and the layers; on the right the 3D view, navigated as in the [Simple NWW Viewer](nwwviewer.md).

Input maps
: the DEM, the sources, the normal and tangential restitution and the friction maps, described in the [Stone](../modules/hydro-geomorphology/stone.md) module. When the DEM is chosen with the **...** button, the terrain is loaded and the view moves to it, and the other maps are searched in its folder with the standard names `sources`, `nrest`, `trest` and `friction`, also with the prefix of the DEM name: for `area1_dem.tif` the files `area1_sources.tif` and so on are tried first.

Prepare from DEM...
: creates the missing maps from the DEM, see [Preparing the maps](#preparing-the-maps).

Parameters
: the **Start velocity** and **Stop velocity** of the boulders, the **Step** of the trajectories (5 m by default, the results depend on it, see the [Stone](../modules/hydro-geomorphology/stone.md#the-step) module) and the number of **Trajectories to save** with the results.

Terrain around the DEM
: the terrain shown outside of the DEM: flat at the lowest elevation of the DEM, or the Copernicus DEM, downloaded for the areas viewed. The simulation uses only the DEM: the boulders stop where they leave it.

## Running a simulation

**Run** simulates the boulders of all the sources, with a progress dialog, and adds the results as layers, which can be switched on and off in the **Layers** list:

- **Counter**, the number of trajectories crossing each cell, on a logarithmic color scale;
- **Max velocity**, the maximum velocity of the boulders in each cell, off by default;
- **Max height**, the maximum height of the trajectories over the ground in each cell, off by default.

The legend of each of them is shown at the bottom of the map while the layer is on. The **Sources** layer shows the source cells in red and the stop cells in blue, the **DEM** layer colors the terrain by elevation.

**Save results...** writes the results of the last simulation in a folder: `rockfall_counter.tif`, `rockfall_maxvel.tif`, `rockfall_maxdz.tif` and the 3D lines of the saved trajectories in `rockfall_trajectories.gpkg`.

## Boulders from a clicked cell

:::{figure} ../images/apps/rockfall/picker_simulation_trajectories.png
:alt: The trajectories of the boulders thrown from a clicked cell
:width: 100%
:align: center

100 boulders thrown from a clicked cell, colored by their velocity.
:::

With **Pick start points** pressed, a yellow sphere follows the terrain under the mouse, and a click throws **Boulders per click** boulders from the clicked cell. The simulation is run for that cell only, with the maps and the parameters of the panel, and all the trajectories are drawn in 3D, colored by velocity, in the **Picked trajectories** layer. A white point marks the center of the cell, where the boulders start; the status line reports the time of the simulation and the longest runout.

The boulders differ in their start direction and in the coefficients of each bounce and roll, so they show what a boulder falling from that cell can do: how far it goes, how fast and how high it bounces. The same cell always gives the same trajectories. They can also go farther than the counter of the simulation, which with one boulder per source shows only the likely paths.

While picking, a click doesn't move the view, so that several cells can be tried from the same point of view; drag to move it. Releasing **Pick start points** removes the picked trajectories.

## Preparing the maps

**Prepare from DEM...** creates the input maps with the [StoneInputs](../modules/hydro-geomorphology/stoneinputs.md) module, asking:

- whether to **create the sources** from the slope, with the slope threshold, the boulders per source and optional stop areas: checked if no sources map is set;
- whether to **create friction and restitutions**, from an optional lithology map and table, with the default lithological class where the lithology is missing: checked if any of these maps is not set.

The maps are written next to the DEM with the standard names, asking before overwriting existing files, and are set in the panel, ready to run.
