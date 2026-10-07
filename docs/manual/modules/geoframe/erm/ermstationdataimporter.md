# ErmStationDataImporter

Imports the time series measured at the meteo stations and at the stream gauges. The CSV files have one column per station, identified by the station ids of the point layers, in the time series format of the HortonMachine:

```text
@T,table
Created,2026-07-17 10:00
Author,HortonMachine library
@H,timestamp,value_1,value_2,value_3
ID,,1,2,3
Type,Date,double,double,double
Format,yyyy-MM-dd HH:mm,
,2002-01-01 01:00,1.65,0.35,3.05
,2002-01-02 01:00,-2.6,-2.65,-0.3
```

The `ID` row holds the station ids, the following rows the timestamp and the value of each station; missing values are `-9999`.

```{literalinclude} ../../examples/erm/02_station_data_importer.groovy
:language: groovy
```

The imported data can be checked in the [Database Viewer](../../../apps/dbviewer.md): right-click on the `station_data` table of the GeoFrame database and choose **Open Station Data Chart**, then pick the station in the combo box at the top.

:::{figure} ../../../images/modules/Geoframe_erm_stationdata_chart.png
:alt: The station data chart of the Database Viewer, with the precipitation and temperature measured at a meteo station.

The data of a meteo station: the measured precipitation and temperature.
:::

```{include} ../../generated/ErmStationDataImporter.md
```
