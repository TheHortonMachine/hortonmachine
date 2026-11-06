# Settings

The Settings application edits the preferences shared by all the HortonMachine applications and modules: network proxy, character encoding, interface scaling, SSH keys, native libraries, credentials for data services, database query timeouts and the theme of the interface.

## Launching

Start the application with the `hm-settings` launcher (see [Launching an application](../installation.md#launching-an-application)).

The settings are organized in tabs, and are saved with the **Apply** button at the bottom of the window, or when the window is closed. They apply to the applications started afterwards.

:::{figure} ../images/apps/settings/proxy.png
:alt: The Settings application
:width: 80%
:align: center

The Settings application, with the proxy tab.
:::

## Proxy

When the internet is reached through a proxy, check **enable proxy** and insert its **host** and **port**, and its **user** and **password** if it requires authentication. The proxy is applied at startup by the [Web Services Browser](webservices.md), [Database Viewer](dbviewer.md), [Geoscript Console](geoscript.md), [Style Editor](sld.md), [Form Builder](gforms.md) and [Quick Folder](quickfolder.md) applications.

## Internationalization

Charset
: the character encoding used to read and write some files, like the attributes of shapefiles and the forms. Leave it empty to use the default one; `UTF-8` is a safe choice for new data.

Component Orientation
: the direction of the user interface, left to right or right to left.

## Preferences

:::{figure} ../images/apps/settings/preferences.png
:alt: The preferences tab
:width: 80%
:align: center

The folder of the preferences database and the interface scaling.
:::

Folder to store the preferences database
: the folder where the preferences of the applications are stored, like the recent files, the saved connections of the Database Viewer or the style templates.

UI scaling to apply
: a factor to enlarge the fonts and the components of the interface: `1.2` enlarges them by 20%. Leave it empty (*automatic*, the default) to use the scale of the system, taken from its font size and the resolution of the screen; set a value only if the automatic one doesn't suit you.

## SSH

The path to the private key, and its passphrase, used by the tools that connect to remote servers through SSH, for example to reach a database through an SSH tunnel.

## Native libs

The folder containing the SpatiaLite loadable modules (`mod_spatialite`), needed to work with SpatiaLite databases when they are not installed in a standard location of the system.

## Copernicus

The API token and the download folder used by the modules that download data from the Copernicus climate services. These are not needed for the Copernicus DEM used by the 3D terrain of the [Simple NWW Viewer](nwwviewer.md), which is open data.

## Database

The time limits, in seconds, for the queries the [Database Viewer](dbviewer.md) runs on PostGIS databases. A query that runs longer is stopped and the viewer reports that it timed out. `0` means no limit. They don't apply to the other database types.

Table preview query timeout
: the limit for loading the first records of a table when it is selected in the database tree, 60 seconds by default. It keeps a very slow table from loading forever.

SQL editor query timeout
: the limit for the queries run from the SQL Editor, `0` (no limit) by default. Keep it at `0` to run long commands, like a `VACUUM FULL` on a large table; a running query can always be stopped with the stop button of its console window.

## Appearance

The **Theme** of the applications. The default is *HortonMachine Light*, used in all the images of this manual; *HortonMachine Dark* is its dark counterpart, and a few Material and Flat themes, light and dark, are available too.

:::{figure} ../images/apps/settings/appearance_dark.png
:alt: The appearance tab with the dark theme
:width: 80%
:align: center

The Settings application with the HortonMachine Dark theme.
:::

A new theme is applied immediately to the open windows of the Settings application and is used by the applications started afterwards. Code editors that are already open keep their colors until they are reopened.
