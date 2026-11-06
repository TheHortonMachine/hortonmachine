# Material Symbols icon font

The gui draws its icons from a cut-down, static version of Google's
[Material Symbols Outlined](https://github.com/google/material-design-icons)
font (Apache License 2.0), stored as
`gui/src/main/resources/org/hortonmachine/gui/icons/MaterialSymbolsOutlined-hm.ttf`.

It only contains the glyphs listed in `glyphs.txt`, instanced at
FILL=0, GRAD=0, opsz=20, wght=400.

## Adding an icon

1. Look up the glyph name on https://fonts.google.com/icons (Material Symbols, Outlined).
2. Add the name to `glyphs.txt`.
3. Rebuild the font (python with fonttools, outside of the maven build):

   ```bash
   python3 -m venv /tmp/fontvenv && /tmp/fontvenv/bin/pip install fonttools
   base='https://raw.githubusercontent.com/google/material-design-icons/master/variablefont/MaterialSymbolsOutlined%5BFILL%2CGRAD%2Copsz%2Cwght%5D'
   curl -L -o /tmp/ms.ttf "$base.ttf"
   curl -L -o /tmp/ms.codepoints "$base.codepoints"
   /tmp/fontvenv/bin/python -I make_font.py /tmp/ms.ttf /tmp/ms.codepoints glyphs.txt \
       ../../src/main/resources/org/hortonmachine/gui/icons/MaterialSymbolsOutlined-hm.ttf
   ```

4. Map the image key to the glyph codepoint (from `ms.codepoints`) in `ImageCache`.
