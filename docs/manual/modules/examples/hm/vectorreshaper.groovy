import org.hortonmachine.modules.VectorReshaper

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var reshaper = new VectorReshaper()
// the sub-basins of BasinShape
reshaper.inVector = folder + "subbasins.shp"
// two new fields: the relief of the sub-basin and its area in square kilometers
reshaper.pCql = """relief=maxelev-minelev
area_km2=area/1000000"""
// the height is not needed any more
reshaper.pRemove = "height"
reshaper.outVector = folder + "subbasins_reshaped.shp"
reshaper.process()
