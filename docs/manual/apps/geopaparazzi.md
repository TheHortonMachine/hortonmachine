# Geopaparazzi Viewer

The Geopaparazzi Viewer browses the survey projects of the [Geopaparazzi](https://www.geopaparazzi.org/) and [SMASH](https://www.geopaparazzi.org/smash/) mobile apps. It lists the projects of a folder and shows their content, GPS logs, notes and images, on a globe with an OpenStreetMap background, with the elevation profile of the GPS logs.

## Launching

Start the application with the `hm-geopaparazzi_viewer` launcher (see [Launching an application](../installation.md#launching-an-application)).

The globe is drawn with NASA WorldWind, which needs OpenGL. On systems where it is not available, the viewer starts without the globe and the charts, and only lists the projects and their content.

## Opening the projects

The viewer works on a folder of projects: Geopaparazzi and SMASH store each project in a single `.gpap` file, so a folder of archived surveys can be browsed at once.

1. Insert the folder in **Projects folder**, or choose it with the **...** button. The folder is remembered for the next sessions.
2. Press the refresh button on the right to read the projects of the folder. Reading many large projects can take a while.
3. Type in **Filter projects** to show only the projects whose file name or metadata contain the text.

:::{figure} ../images/apps/geopaparazzi/projects.png
:alt: The projects list filtered to a single project
:width: 100%
:align: center

A folder of SMASH projects, filtered to a single project.
:::

Select a project to see its metadata in the **Info** panel. Right-click on it to load it:

:::{figure} ../images/apps/geopaparazzi/project_menu.png
:alt: The project menu
:width: 50%
:align: center

The actions on a project.
:::

Load Project in Viewer
: draws all the GPS logs and notes of the project on the globe, and lists them in the tree below the project: first the GPS logs, then the notes and the images.

Edit Project Metadata
: edits the metadata of the project, like its name, description and author, and saves them into the project file.

## GPS logs, notes and images

Select an element of a loaded project to zoom the globe to it and see its details in the **Info** panel.

GPS logs
: the Info panel shows the name and the start and end time of the log, and the **Charts** panel its elevation profile along the distance.

:::{figure} ../images/apps/geopaparazzi/gps_log.png
:alt: A GPS log with its elevation profile
:width: 100%
:align: center

A GPS log recorded with SMASH in Tokyo, with its elevation profile.
:::

Notes
: the Info panel shows the text, the description, the time and the altitude of the note, which is labeled on the globe.

:::{figure} ../images/apps/geopaparazzi/note.png
:alt: A note on the globe
:width: 100%
:align: center

A note taken in Kanazawa.
:::

Images
: the Info panel shows the details of the image. Right-click on it to **Open Image**, shown in a window scaled to at most 800 pixels, or to **Save Image** into a folder as a JPEG file.

The logs are drawn with the color and line width set in the app during the survey.

**use GPS elevations for logs**, at the top, draws the GPS logs at the altitude recorded by the GPS, instead of draping them on the terrain. Change it before loading the project.
