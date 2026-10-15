"""Sphinx configuration for the HortonMachine user manual (hosted on Read the Docs)."""

import copy

from pygments.lexers.jvm import GroovyLexer
from pygments.token import String

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
exclude_patterns = ["README.md", "_build", ".venv", "Thumbs.db", ".DS_Store", "modules/generated/*", "modules/undocumented.md"]

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


def _groovy_tokens():
    """
    The tokens of the Pygments Groovy lexer, with a stricter slashy string: the lexer takes
    any / as the start of a /string/ up to the next /, also on the following lines, so a
    division as "a / b" colors the script as a string up to the next division. Here a
    slashy string can't start with a space, / , * or =, and ends on its line.
    """
    tokens = copy.deepcopy(GroovyLexer.tokens)
    for rules in tokens.values():
        for i, rule in enumerate(rules):
            if isinstance(rule, tuple) and rule[0] == r'/(\\\\|\\[^\\]|[^/\\])*/':
                rules[i] = (r'/(?![\s/*=])(\\\\|\\[^\\\n]|[^/\\\n])*/', String)
    return tokens


class ManualGroovyLexer(GroovyLexer):
    tokens = _groovy_tokens()


def setup(app):
    app.add_lexer("groovy", ManualGroovyLexer)
