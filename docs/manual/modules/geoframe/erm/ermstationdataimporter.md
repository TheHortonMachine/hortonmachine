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

```{include} ../../generated/ErmStationDataImporter.md
```
