import org.hortonmachine.modules.SWYRechargeRouting

// the folder with the gura folder of the sample data: change it to yours
var folder = "/data/"
var gura = folder + "gura/"
var monthly = gura + "monthly/"

// the recharge of the year, from the rasters of the 12 months, found by their name prefix
var recharge = new SWYRechargeRouting()
recharge.inFlowdirections = gura + "drain.tif"
recharge.inPrecipitationFolder = monthly
recharge.pPrecipitationPrefix = "prcp_"
recharge.inQuickflowFolder = monthly
recharge.pQuickflowPrefix = "qf_"
recharge.inPetFolder = monthly
recharge.pPetPrefix = "pet_"
// as in InVEST: alpha 1/12 for every month (the default), beta and gamma 1
recharge.pBeta = 1.0
recharge.pGamma = 1.0
recharge.outRecharge = gura + "l.tif"
recharge.outAvailableRecharge = gura + "l_avail.tif"
recharge.outUpslopeSubsidy = gura + "l_sum_avail.tif"
recharge.outAet = gura + "aet.tif"
recharge.process()
