# The HortonMachine manual

The HortonMachine is an open source geospatial library, written in Java, focused on hydro-geomorphological analysis and environmental modelling.

Its main areas are:

- **geomorphology**: depitting, drainage directions, contributing areas, network and watershed extraction, slope, curvatures, and hydrologic and geomorphologic indexes;
- **hydrology and hazards**: meteorological interpolation (kriging and Jami), hydraulic and peak flow models, debris flow triggering, propagation and deposition, hillslope stability;
- **lidar**: the LESTO tools (LiDAR Empowered Science Toolbox Open source);
- **raster and vector processing**, spatial databases, and access to remote data services like WFS, WCS and STAC.

## A bit of history

Development started in 2002 at the Department of Civil and Environmental Engineering of the University of Trento, with the first HortonMachine tools written for GRASS. In 2003 the JGrass project was born, a collaboration between Icens Kingston and the University of Trento, and from 2005 HydroloGIS took over development and coordination.

Over the years the tools moved through several desktop GIS platforms: uDig from 2007, gvSIG from 2015, while OpenMI and then OMS became the modelling frameworks behind the modules. Along the way came the support for PostGIS, NetCDF and time series, the network tools Epanet and TrentoP, and in 2014 the LESTO lidar tools, developed with the Free University of Bozen-Bolzano. Since 2017 the HortonMachine is also used as the geospatial and hydro-geomorphological engine of k.LAB.

## Using the HortonMachine

The library can be used in three ways:

- through its **desktop applications**, small standalone programs built around a single task, like browsing a STAC catalog or viewing a spatial database;
- through the **Spatial Toolbox**, a graphical launcher for the processing modules;
- as a **Java library** in your own code, from the Maven artifacts published on Maven Central.

This manual covers the first two. It is a work in progress: the chapters are added one application at a time.

## License

The HortonMachine source code is licensed under the [GNU General Public License, Version 3](https://github.com/TheHortonMachine/hortonmachine/blob/master/LICENSE). The manual is licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).

## Need help? Found bugs?

Report bugs and ask for features in the [issue tracker](https://github.com/TheHortonMachine/hortonmachine/issues).

This manual lives alongside the source code, under `docs/manual`, so documentation changes can travel together with the feature changes that motivate them. Contributions are welcome as pull requests.

```{toctree}
:maxdepth: 2
:caption: Getting started

installation
```

```{toctree}
:maxdepth: 2
:caption: Applications

apps/index
apps/spatialtoolbox
apps/geoscript
apps/dbviewer
apps/stacbrowser
apps/sld
apps/wms2geotiff
apps/geopaparazzi
apps/gforms
apps/lasviewer
apps/mapcalc
apps/nwwviewer
apps/quickfolder
apps/sshutils
apps/settings
```
