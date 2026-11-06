import org.hortonmachine.modules.CurvaturesBivariate

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the curvatures of the forms of about 90 m: a window of 9 x 9 cells of 10 m
var curvatures = new CurvaturesBivariate()
curvatures.inElev = folder + "pit.tif"
curvatures.pCells = 9
curvatures.outProf = folder + "prof_bivariate.tif"
curvatures.outPlan = folder + "plan_bivariate.tif"
curvatures.outSlope = folder + "slope_bivariate.tif"
curvatures.outAspect = folder + "aspect_bivariate.tif"
curvatures.process()
