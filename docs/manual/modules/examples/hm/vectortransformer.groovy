import org.hortonmachine.modules.VectorTransformer

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var transformer = new VectorTransformer()
// the cells of GridsGenerator
transformer.inVector = folder + "grid.shp"
// turned by 30 degrees clockwise around their lower left corner
transformer.pRotate = 30.0
transformer.pCenterX = 1637140.0
transformer.pCenterY = 5110830.0
// and moved 300 m east
transformer.pTransX = 300.0
transformer.pTransY = 0.0
transformer.outVector = folder + "grid_rotated.shp"
transformer.process()
