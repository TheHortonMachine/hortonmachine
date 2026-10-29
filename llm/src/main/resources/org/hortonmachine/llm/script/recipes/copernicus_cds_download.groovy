// Download data from the Copernicus Climate Data Store (CDS) with its OGC API Processes: options, valid values, job, download
// keywords: copernicus, cds, climate data store, era5, agrometeorological, meteo, climate, download, ogc api processes, job, api token, data
import org.hortonmachine.gears.io.copernicus.CopernicusDailyAgrometeoManagerBase
import org.hortonmachine.gears.io.copernicus.OGCProcessesManager

// the personal access token of the CDS account, from the HortonMachine preferences or the
// environment variable hm_pref_copernicus_api_token: never write it in a script
def apiToken = CopernicusDailyAgrometeoManagerBase.getApiToken()
def outZip = "/path/to/result.zip"
def baseUrl = "https://cds.climate.copernicus.eu/api/retrieve/v1"
def collectionId = "sis-agrometeorological-indicators"
def variable = "2m_temperature"

def manager = new OGCProcessesManager(baseUrl, null)
manager.open()
manager.setApiToken(apiToken)
try {
    // the inputs of the process and their allowed values
    def description = manager.getProcessDescription(collectionId)
    println "Inputs: " + description.inputs.keySet()
    println "Variables: " + description.inputs.variable.schema.enum

    // the values still valid once some inputs are chosen
    def valid = manager.getConstraints(collectionId, [variable: variable])
    println "Statistics for ${variable}: " + valid.statistic
    println "Years for ${variable}: " + valid.year

    def inputs = [
        variable : variable,
        statistic: ["24_hour_mean"],
        year     : ["2023"],
        month    : ["06"],
        day      : (1..15).collect { String.format("%02d", it) },
        version  : "2_0"
    ]
    def job = manager.submitJob(collectionId, inputs)
    println "Submitted: " + job
    // poll every 10 seconds, for at most 30 minutes
    job = manager.waitForCompletion(job, 10_000L, 30L * 60_000L)
    manager.downloadResult(job.jobId, null, outZip)
    println "Downloaded to " + outZip
} finally {
    manager.close()
}
