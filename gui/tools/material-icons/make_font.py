"""Build the cut-down static Material Symbols font used by the HortonMachine gui.

usage: python -I make_font.py <variable font ttf> <codepoints file> <glyph names file> <output ttf>
"""
import sys

from fontTools.ttLib import TTFont
from fontTools.varLib import instancer
from fontTools import subset

src, cpfile, namesfile, out = sys.argv[1:5]

codepoints = {}
for line in open(cpfile, encoding="utf-8"):
    parts = line.split()
    if len(parts) == 2:
        codepoints[parts[0]] = int(parts[1], 16)

names = [n.strip() for n in open(namesfile, encoding="utf-8") if n.strip() and not n.startswith("#")]
unicodes = sorted({codepoints[n] for n in names})

font = TTFont(src)
static = instancer.instantiateVariableFont(font, {"FILL": 0, "GRAD": 0, "opsz": 20, "wght": 400})

options = subset.Options()
options.layout_features = []  # no ligatures needed, glyphs are addressed by codepoint
options.name_IDs = ["*"]
options.notdef_outline = True
subsetter = subset.Subsetter(options)
subsetter.populate(unicodes=unicodes)
subsetter.subset(static)
static.save(out)

head = static["head"]
hhea = static["hhea"]
print("glyphs:", len(unicodes), "unitsPerEm:", head.unitsPerEm, "ascent:", hhea.ascent, "descent:", hhea.descent)
