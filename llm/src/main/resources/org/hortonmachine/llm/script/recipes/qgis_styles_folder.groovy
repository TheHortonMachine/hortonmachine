// Create QGIS styles (qml files with a color table) for the rasters of a folder
// keywords: qgis, style, styles, qml, colortable, colors, legend, rasters, folder, visualize
import org.hortonmachine.* // the HM helper

def folder = "/path/to/rasters/"
// a color table, as elev, rainbow, flow, net, logarithmic, slope, aspect
def colortable = "rainbow"
// the decimals of the legend labels
def decimals = 2

(folder as File).eachFile { file ->
    def name = file.name.toLowerCase()
    if (name.endsWith(".tif") || name.endsWith(".tiff") || name.endsWith(".asc")) {
        HM.makeQgisStyleForRaster(colortable, file.absolutePath, decimals)
        println "Styled " + file.name
    }
}
