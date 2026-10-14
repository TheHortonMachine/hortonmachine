import org.hortonmachine.modules.SWYBaseflowRouting

// the folder with the gura folder of the sample data: change it to yours
var folder = "/data/"
var gura = folder + "gura/"

var baseflow = new SWYBaseflowRouting()
baseflow.inFlowdirections = gura + "drain.tif"
baseflow.inNet = gura + "net.tif"
baseflow.inRecharge = gura + "l.tif"
baseflow.inAvailableRecharge = gura + "l_avail.tif"
baseflow.outLsum = gura + "l_sum.tif"
baseflow.outBaseflow = gura + "b_sum.tif"
baseflow.outB = gura + "b.tif"
baseflow.outVri = gura + "vri.tif"
baseflow.process()

// the total recharge is in mm summed over the cells: a volume with the cell area of 15 x 15 m
var cellArea = 15 * 15
println "Annual recharge: " + baseflow.outQb * 0.001 * cellArea + " m3"
println "Sum of the values index: " + baseflow.outVriSum
