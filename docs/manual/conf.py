"""Sphinx configuration for the HortonMachine user manual (hosted on Read the Docs)."""

project = "HortonMachine"
copyright = "2008-2026, Andrea Antonello - manual content licensed under CC BY 4.0"
author = "Andrea Antonello"
release = "0.11.5"

extensions = [
    "myst_parser",
    "sphinx.ext.autosectionlabel",
]

# Headings are labeled as "document:heading" to avoid collisions between
# identically named headings across chapters (every app has a "Launching").
autosectionlabel_prefix_document = True

myst_enable_extensions = [
    "colon_fence",
    "deflist",
]
# Auto-generate anchors for headings up to this depth, so plain markdown
# links like [text](file.md#heading-slug) work without manual anchors.
myst_heading_anchors = 4

source_suffix = {
    ".md": "markdown",
}

# The module reference fragments are not pages: they are included in the module pages.
exclude_patterns = ["README.md", "_build", ".venv", "Thumbs.db", ".DS_Store", "modules/generated/*"]

html_theme = "furo"
html_title = "HortonMachine Manual"
html_static_path = ["_static"]
html_css_files = ["custom.css"]

html_theme_options = {
    "light_css_variables": {
        "color-brand-primary": "#2e7d32",
        "color-brand-content": "#2e7d32",
    },
    "dark_css_variables": {
        "color-brand-primary": "#81c784",
        "color-brand-content": "#81c784",
    },
    "sidebar_hide_name": False,
}
