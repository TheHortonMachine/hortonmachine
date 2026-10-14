import org.hortonmachine.modules.SWYQuickflow

// the folder with the gura folder of the sample data: change it to yours
var folder = "/data/"
var gura = folder + "gura/"
var monthly = gura + "monthly/"

// the quickflow of each month
for (month in 1..12) {
    var quickflow = new SWYQuickflow()
    quickflow.inRainfall = monthly + "prcp_" + month + ".tif"
    quickflow.inCurveNumber = gura + "cn.tif"
    quickflow.inNet = gura + "net.tif"
    quickflow.inNumberOfEvents = monthly + "events_" + month + ".tif"
    quickflow.outQuickflow = monthly + "qf_" + month + ".tif"
    quickflow.process()
}
