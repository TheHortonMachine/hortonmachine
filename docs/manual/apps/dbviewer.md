# Database Viewer

The Database Viewer explores and edits spatial databases: it shows their tables and columns, runs SQL queries, displays the results, and offers ready-made queries and tools for the most common tasks, from counting records to creating spatial indexes or importing data.

It supports GeoPackage, SpatiaLite, SQLite, MBTiles and H2GIS files, and PostGIS and H2GIS databases on a server.

## Launching

Start the application with the `hm-dbviewer` launcher (see [Launching an application](../installation.md#launching-an-application)).

The launcher optionally takes the path of a database file to open at startup:

```sh
./hm-dbviewer.sh /data/natural_earth_vector_4.1.0.gpkg
```

Without it, the viewer reopens the last database file used, if it still exists.

## The main window

:::{figure} ../images/apps/dbviewer/main.png
:alt: The Database Viewer main window
:width: 100%
:align: center

The Database Viewer connected to the Natural Earth GeoPackage.
:::

The window has four parts:

- the **toolbar** at the top, to create, open and close databases, and to reach the SQL history and templates;
- the **database tree** on the left, with the tables of the database and their columns;
- the **SQL Editor** on the top right, with five editor tabs to keep different queries at hand;
- the **Data Viewer** on the bottom right, with five viewer tabs showing the results.

## Connecting to a database

new
: creates a new database, choosing its path and type: SQLite, MBTiles, H2GIS, GeoPackage, SpatiaLite or PostGIS. For H2GIS and PostGIS a user name and password can be set.

connect
: opens an existing database file. Right-click on the button to load a saved connection, open a recent one, or export and import the saved connections.

remote
: connects to a database on a server, through its JDBC address (for example `jdbc:postgresql://host:5432/dbname` for PostGIS, or `jdbc:h2:tcp://host:9092/path` for H2GIS), user name and password.

disconnect
: closes the current database.

:::{figure} ../images/apps/dbviewer/new_database.png
:alt: The new database dialog
:width: 70%
:align: center

The dialog to create a new database.
:::

## The database tree

The tree shows the tables of the database. Spatial tables have a globe icon; expand a table to see its columns, with their type. Geometry columns show the geometry type, the EPSG code of their projection and whether they have a spatial index, like `geom [Polygon,EPSG:4326,idx:true]` in the example above.

Selecting a table shows its first 100 records in the Data Viewer. A table or column name can be dragged from the tree into the SQL Editor.

Right-click on the elements of the tree to get the actions available for them.

### Database actions

:::{figure} ../images/apps/dbviewer/database_menu.png
:alt: The database context menu
:width: 45%
:align: center

The actions on a SpatiaLite database.
:::

On the database node, at the top of the tree:

- **Refresh** and **Reconnect**;
- **Copy path** of the database, and **Save Connection** to find it later from the connect button;
- **Import sql file**, to run the statements of a SQL file;
- on spatial databases, **Create table from vector file** and **Update Layer Statistics**, and on SpatiaLite **Attach readonly shapefile**;
- on GeoPackages, **Import raster to tileset** and **Import vector to tileset**, to create tile tables;
- on PostGIS, the server actions described in [PostGIS databases](#postgis-databases);
- **Create table from CSV**.

### Table actions

:::{figure} ../images/apps/dbviewer/table_menu.png
:alt: The table context menu
:width: 55%
:align: center

The actions on a spatial table of a GeoPackage.
:::

On a table:

- **Count table records**;
- **Select statement**, **Insert statement** and **Drop table statement**, which write the corresponding SQL into the editor, ready to be completed and run;
- **Generate insert sql statements**, to export the content of the table as SQL;
- on spatial tables, **Import data from vector file**, **Quick View Table in 3D** on a NASA WorldWind globe, and **Open in SLD editor** or **Open in FORMS editor** to create a style or a form for the table;
- **Import CSV into table**.

### Column actions

:::{figure} ../images/apps/dbviewer/column_menu.png
:alt: The column context menu
:width: 55%
:align: center

The actions on a column.
:::

On a column, the menu writes into the editor a select on the column, sorted in ascending or descending order, grouped with the count of each value, or an update of the column.

On a geometry column there is also **Show spatial metadata**, and depending on the database, actions to create, check, recover or disable the spatial index.

## Running queries

Write a query in the SQL Editor and press the run button (the green arrow) on its left. The results appear in the current tab of the Data Viewer, and the number of records and the time taken are shown below it.

:::{figure} ../images/apps/dbviewer/query.png
:alt: A query and its results
:width: 100%
:align: center

The European countries, sorted by population.
:::

The buttons on the left of the editor:

Run
: runs the query and shows the result.

Run to file
: runs a select query and saves the result to a CSV file (semicolon separated, with a header).

Run to vector file
: runs a select query with a geometry column and saves the result as a shapefile or GeoPackage.

Clear
: empties the editor (the trash button).

The options below the editor:

Limit result to
: the maximum number of records shown, 1000 by default. Use `-1` for no limit.

Refresh tree after query
: reloads the database tree after running the query, useful after creating or dropping tables.

The options below the Data Viewer:

Format dates
: shows the columns whose name contains one of the **patterns** (by default `date`, `ts` and `timestamp`) as readable dates instead of numeric timestamps.

Right-click in the SQL Editor to save the current query, load a saved one, or export and import the saved queries.

While a query runs, its progress is shown in a console window: press its stop button to cancel the query. On PostGIS, the time limits for the editor queries and for the table previews are set in the **Database** tab of the [Settings](settings.md#database).

### SQL history and templates

**sql history** lists the queries run so far; select one to see it in full and press **Use** to bring it back into the editor.

**sql templates** offers templates of common statements, like simple, filtered, limited and sorted selects, adding a column, or selecting geometries. Press **Use** to copy the selected template into the editor.

:::{figure} ../images/apps/dbviewer/templates.png
:alt: The SQL templates
:width: 70%
:align: center

The SQL templates.
:::

## Working with the results

Right-click on the selected cells of the Data Viewer to copy their content. The menu offers more, depending on the content of the cell.

For geometries:

View geometry
: shows the geometries of the selected cells in a small viewer.

Plot geometry
: plots the geometries of the selected cells as a chart.

View geometry with directions hint
: shows the geometries with arrows marking the direction of their lines.

:::{figure} ../images/apps/dbviewer/cell_menu.png
:alt: The context menu of a geometry cell
:width: 90%
:align: center

The actions on a geometry cell.
:::

:::{figure} ../images/apps/dbviewer/geometry.png
:alt: A geometry shown in the viewer
:width: 60%
:align: center

The geometry of Italy, shown with View geometry.
:::

For binary data, like the images of a tile table, **View as image** shows the content as an image, and **View as string** as text.

:::{figure} ../images/apps/dbviewer/tiles_menu.png
:alt: The context menu of a tile cell
:width: 90%
:align: center

The tile table of a GeoPackage, with the actions on the tile data.
:::

:::{figure} ../images/apps/dbviewer/tile_image.png
:alt: A tile shown as image
:width: 50%
:align: center

A tile of the orthophoto, shown with View as image.
:::

### Editing values

Double-click on a cell to change its value in the database. This works on the results of queries that include the primary key of the table, which is used to find the record to update.

## PostGIS databases

PostGIS databases are opened with **remote**, through a `jdbc:postgresql://host:5432/dbname` address. A database that the server doesn't expose to the internet can be reached through an SSH tunnel, opened with [SSH Utils](sshutils.md). Everything described above works on them too, and the database node of the tree has a few more actions for the server:

Switch database
: lists the databases of the server and connects to the chosen one with the same user, in the same window or, with **Open in new window**, in a new one.

List active connections
: shows the connections currently open on the server, with their database, user, state, client and running query.

Clean up idle connections
: closes the connections that are open but idle.
