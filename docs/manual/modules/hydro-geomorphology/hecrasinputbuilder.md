# HecrasInputBuilder

HecrasInputBuilder writes the sections of [RiverSectionsExtractor](riversectionsextractor.md) as a geometry file for the GIS import of HEC-RAS, the 1D and 2D hydraulic model of the US Army Corps of Engineers. The file follows the spatial data format of the import (the `.sdf` files of HEC-GeoRAS): a single reach, with its centerline and its cross sections, in metric units.

The river stations of the sections are numbers that grow upstream, and the reach lengths are the distances to the next section along the river and along the ends of the sections. The whole section is the main channel, with a Manning coefficient of 0.0333, the Gauckler-Strickler coefficient of 30. The banks and the roughness are meant to be refined in HEC-RAS after the import.

:::{warning}
Each section gets two levees at its ends, with the length of the section as their elevation: in a mountain valley they are far below the terrain and have no effect, but where the terrain is lower than the length of the sections in meters they act as walls. Remove or correct them in HEC-RAS.
:::

## Example

The example writes the sections of the example of [RiverSectionsExtractor](riversectionsextractor.md), on the main valley of the sample area:

```{literalinclude} ../examples/hm/hecrasinputbuilder.groovy
:language: groovy
```

The beginning of the file, with the reach from its upstream end, and the first cross section:

```text
# Header must contain a record to identify the unit system used in the imported data set
BEGIN HEADER:
# Number of reaches
NUMBER OF REACHES: 1
# Number of cross sections
NUMBER OF CROSS-SECTIONS:
35
# Unit system used
UNITS: METRIC
END HEADER:

BEGIN STREAM NETWORK:
# List of all endpoint of the multiline that represents the river
ENDPOINT:	1639985.0,5113775.0,1955.1600341796875,	1
ENDPOINT:	1638705.0,5112805.0,1375.737060546875,	2
# Description of the river reach
REACH:
STREAM ID: flanginec
REACH ID: headwaters
# Upsteam endpoint
FROM POINT: 1
# Downsteam endpoint
TO POINT: 2

# Coordinates and floating point station value to draw the river network:

CENTERLINE:
1639985.0,	5113775.0,	1955.1600341796875,	209
1639979.0,	5113767.0,	1950.1689453125,	208
...

BEGIN CROSS-SECTIONS:

# New Cross Section
# Cross section must include records identifying the stream, reach and station value of cross section
CROSS-SECTION:
STREAM ID: flanginec
REACH ID: headwaters
STATION: 210
BANK POSITIONS:	0.0,	1.0
REACH LENGTHS: 70.74648682614566,	50.0,	29.576641453276633
NVALUES: 
0.00,	0.0333
LEVEE POSITIONS:
1,0.0,0.0
2,1.0,120.0
CUT LINE: 
1640033.0,	5113739.0
1639937.0,	5113811.0

SURFACE LINE: 
1640033.0,	5113739.0,	1960.803955078125
1640025.0,	5113745.0,	1957.1629638671875
...
```

## Reference

```{include} ../generated/HecrasInputBuilder.md
```
