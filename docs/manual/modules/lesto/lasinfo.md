# LasInfo

LasInfo prints what a LAS file contains: the fields of its header, its reference system and its first three points. It is the quick check of a survey before using it, as of its point format, its number of points and its bounds. Given a folder, it prints the same for each LAS file in it.

## Details

```{include} ../generated/LasInfo_details.md
```

## Parameters

```{include} ../generated/LasInfo.md
```

## Example

The information of the sample survey:

```{literalinclude} ../examples/hm/lasinfo.groovy
:language: groovy
```

The header tells that the survey has 33,410 points of format 1, with times and without colors, in the bounds of the plot, from 1896.66 to 1948.81 m of elevation:

```text
Version: 1.0
Point data format ID (0-99 for spec): 1
Number of point records: 33410
Scale: [0.01, 0.01, 0.001]
Offset: [0.0, 0.0, 0.0]
X Range: [725717.98, 725778.47]
Y Range: [5205734.78, 5205795.28]
Z Range: [1896.66, 1948.81]
Has gps time info: true
Has color info: false
```

The reference system follows, WGS 84 / UTM zone 32N, then the first points, each with all its attributes:

```text
Point 1
Dot ( 
	x = 725729.0700000001
	y = 5205795.19
	z = 1900.15
	intensity = 174
	impulse = 1
	impulseNum = 1
	classification = 2
	gpsTime = 204505.164429716
 )
```
