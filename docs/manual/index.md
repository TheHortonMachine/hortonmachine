# The HortonMachine - an open source geospatial library focused on hydro-geomorphological analysis and environmental modelling

The HortonMachine is an open source geospatial library, written in Java, focused on hydro-geomorphological analysis and environmental modelling.

Its main areas are:

- **geomorphology**: depitting, drainage directions, contributing areas, network and watershed extraction, slope, curvatures, and hydrologic and geomorphologic indexes;
- **hydrology and hazards**: meteorological interpolation (kriging and Jami), hydraulic and peak flow models, debris flow triggering, propagation and deposition, hillslope stability;
- **lidar**: the LESTO tools (LiDAR Empowered Science Toolbox Open source);
- **raster and vector processing**, spatial databases, and access to remote data services like WFS, WCS and STAC.

## A bit of history

Development started in 2002 at the Department of Civil and Environmental Engineering of the University of Trento, with the first HortonMachine tools written for GRASS. In 2003 the JGrass project was born, a collaboration between Icens Kingston and the University of Trento, started by our late friend [John Preston](https://www.researchgate.net/profile/J-Preston) of Kingston, Jamaica, together with professor Riccardo Rigon and Andrea Antonello. The joke at the time was that the J in JGrass didn't stand for Java, but for Jamaican. From 2005 HydroloGIS took over development and coordination.

Over the years the tools moved through several desktop GIS platforms: uDig from 2007, gvSIG from 2015, while OpenMI and then OMS became the modelling frameworks behind the modules. Along the way came the support for PostGIS, NetCDF and time series, the network tools Epanet and TrentoP, and in 2014 the LESTO lidar tools, developed with the Free University of Bozen-Bolzano and started together with the late [Giustino Tonon](https://www.researchgate.net/profile/Giustino-Tonon), a kind and generous mentor.

Since 2017 the HortonMachine runs under the hood of [k.LAB](https://aries.integratedmodelling.org/), the semantic modelling platform of the ARIES project (ARtificial Intelligence for Environment & Sustainability), where it powers the geospatial and hydro-geomorphological computations. Through k.LAB it ends under the hood of **ARIES for SEEA**, the United Nations initiative that supports countries in compiling natural capital accounts under the System of Environmental-Economic Accounting.

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
```

```{toctree}
:maxdepth: 2
:caption: Modules

modules/index
```
