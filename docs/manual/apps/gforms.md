# Form Builder

The Form Builder edits the forms used by the Geopaparazzi and SMASH mobile apps to collect structured notes, stored in `tags.json` files or in GeoPackage tables.

:::{note}
This application has been superseded by the form builder of SMASH itself, which is the recommended tool to create forms: see [Building forms visually with the Formbuilder](https://smash-smart-mobile-app-for-surveyors-happyness.readthedocs.io/en/latest/notes.html#building-forms-visually-with-the-formbuilder) in the SMASH manual. The HortonMachine Form Builder still works, and is described here only briefly.
:::

## Launching

Start the application with the `hm-gforms` launcher (see [Launching an application](../installation.md#launching-an-application)), optionally followed by the path of the forms file to open:

```sh
./hm-gforms.sh /path/to/tags.json
```

It can also be opened from the [Database Viewer](dbviewer.md), with **Open in FORMS editor** on a spatial table of a GeoPackage.

## Editing forms

:::{figure} ../images/apps/gforms/image_note.png
:alt: The Form Builder with the image note section
:width: 100%
:align: center

The default forms of SMASH, with the image note section selected.
:::

- The drop-down at the top selects the **section** (the note type) to edit; **add** and **del** create and remove sections.
- Each section has one or more **forms**, shown as tabs on the left, added and removed with the **add** and **del** buttons on the right.
- The widgets of the selected form are shown in the middle. The bar at the bottom adds a widget of the type chosen on the left (text, numbers, booleans, dates, pictures, combos and more), or removes the widget chosen on the right.

:::{figure} ../images/apps/gforms/text_note.png
:alt: The text note section
:width: 100%
:align: center

The text note section, with its title and description fields.
:::
