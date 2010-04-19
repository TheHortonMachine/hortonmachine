package eu.hydrologis.edc.oms.datareader;

import static java.lang.Math.pow;
import static java.lang.Double.NaN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Out;

import org.hibernate.Criteria;
import org.hibernate.classic.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;

import eu.hydrologis.edc.annotatedclasses.HydrometersDischargeScalesTable;
import eu.hydrologis.edc.annotatedclasses.HydrometersTable;
import eu.hydrologis.edc.annotatedclasses.ScaleTypeTable;
import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesHydrometersTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;
import eu.hydrologis.edc.utils.Constants;
import eu.hydrologis.jgrass.jgrassgears.libs.modules.HMConstants;
import eu.hydrologis.jgrass.jgrassgears.libs.modules.HMModel;
import eu.hydrologis.jgrass.jgrassgears.libs.modules.ModelsEngine;
import eu.hydrologis.jgrass.jgrassgears.libs.modules.SplitVectors;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.DummyProgressMonitor;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.IHMProgressMonitor;
import eu.hydrologis.jgrass.jgrassgears.utils.math.ListInterpolator;
import eu.hydrologis.jgrass.jgrassgears.utils.sorting.QuickSortAlgorithm;

@SuppressWarnings("nls")
public class HydrometerSeriesAggregator implements ITimeseriesAggregator {
    @Description("The EDC instance to use.")
    @In
    public EdcSessionFactory edcSessionFactory = null;

    @Description("The id of the hydrometer to use.")
    @In
    public long inId;

    @Description("The start date for data fetching (yyyy-mm-dd hh:mm).")
    @In
    public String tStart;

    @Description("The end date for data fetching (yyyy-mm-dd hh:mm).")
    @In
    public String tEnd;

    @Description("The scaletype to do level to discharge conversions and corrections.")
    @In
    public long pTypeid;

    @Description("The aggregation type of the fetched data. (0 = hour, 1 = day, 2 = month, 3 = year)")
    @In
    public int pAggregation;

    @Description("The progress monitor.")
    @In
    public IHMProgressMonitor pm = new DummyProgressMonitor();

    @Description("The result of the data aggregation")
    @Out
    public AggregatedResult outData;

    private Session session;

    private LinkedHashMap<DateTime, Double> timestamp2Data;
    private DateTimeFormatter formatter = Constants.utcDateFormatterYYYYMMDDHHMM;

    private ListInterpolator dischargeScaleInterpolator;

    @Execute
    public void getData() {
        if (outData != null) {
            return;
        }

        session = edcSessionFactory.openSession();

        DateTime startDateTime = formatter.parseDateTime(tStart);
        DateTime endDateTime = formatter.parseDateTime(tEnd);

        pm.message("Getting hydrometers series data...");
        List<SeriesHydrometersTable> seriesData = getSeriesData(startDateTime, endDateTime);
        timestamp2Data = new LinkedHashMap<DateTime, Double>();
        pm.message("Getting discharge scale data...");
        LinkedHashMap<Double, Double> dischargeScale = getDischargeScale();

        pm.message("Correct/convert data...");
        for( SeriesHydrometersTable seriesHydrometersTable : seriesData ) {
            DateTime timestampUtc = seriesHydrometersTable.getTimestampUtc();
            DateTime dateTime = timestampUtc.toDateTime(DateTimeZone.UTC);
            Double value = seriesHydrometersTable.getValue();
            Double toPrincipal = seriesHydrometersTable.getUnit().getToPrincipal();
            value = value * toPrincipal;

            if (dischargeScale != null) {
                Double tmpvalue = dischargeScale.get(value);

                if (tmpvalue == null) {
                    value = getInterpolated(dischargeScale, value);
                } else {
                    value = tmpvalue;
                }
            }

            timestamp2Data.put(dateTime, value);
        }

        pm.message("Aggregating data...");
        switch( pAggregation ) {
        case 0:
            // 0 = hour
            outData = getHourlyAggregation();
            break;
        case 1:
            // 1 = day
            outData = getDailyAggregation();
            break;
        case 2:
            // 2 = month
            outData = getMonthlyAggregation();
            break;
        case 3:
            // 3 = year
            outData = getYearlyAggregation();
            break;
        default:
            break;
        }
    }

    private Double getInterpolated( LinkedHashMap<Double, Double> dischargeScale, Double value ) {
        if (dischargeScaleInterpolator == null) {
            List<Double> xList = new ArrayList<Double>();
            List<Double> yList = new ArrayList<Double>();
            Set<Entry<Double, Double>> entrySet = dischargeScale.entrySet();
            for( Entry<Double, Double> entry : entrySet ) {
                Double v1 = entry.getKey();
                Double v2 = entry.getValue();
                xList.add(v1);
                yList.add(v2);
            }
            dischargeScaleInterpolator = new ListInterpolator(xList, yList);
        }
        Double newValue = dischargeScaleInterpolator.linearInterpolateY(value);
        return newValue;
    }

    private LinkedHashMap<Double, Double> getDischargeScale() {
        Criteria criteria = session.createCriteria(HydrometersDischargeScalesTable.class);
        HydrometersTable hydrometer = new HydrometersTable();
        hydrometer.setId(inId);
        criteria.add(Restrictions.eq("hydrometer", hydrometer));
        ScaleTypeTable scaleType = new ScaleTypeTable();
        scaleType.setId(pTypeid);
        criteria.add(Restrictions.eq("scaleType", scaleType));
        List<HydrometersDischargeScalesTable> dischargeScaleList = criteria.list();

        if (dischargeScaleList == null || dischargeScaleList.size() == 0) {
            return null;
        }
        LinkedHashMap<Double, Double> dischargeScale = new LinkedHashMap<Double, Double>();
        for( HydrometersDischargeScalesTable scale : dischargeScaleList ) {
            Double levelToPrincipal = scale.getLevelUnit().getToPrincipal();
            Double dischargeToPrincipal = scale.getDischargeUnit().getToPrincipal();

            Double discharge = scale.getDischarge();
            Double level = scale.getLevel();

            dischargeScale.put(level * levelToPrincipal, discharge * dischargeToPrincipal);
        }
        return dischargeScale;
    }

    private List<SeriesHydrometersTable> getSeriesData( DateTime startDateTime, DateTime endDateTime ) {
        Criteria criteria = session.createCriteria(SeriesHydrometersTable.class);
        // set id
        HydrometersTable hydrometer = new HydrometersTable();
        hydrometer.setId(inId);
        criteria.add(Restrictions.eq("hydrometer", hydrometer));
        // set time frame
        criteria.add(Restrictions.between("timestampUtc", startDateTime, endDateTime));
        criteria.addOrder(Order.asc("timestampUtc"));

        List<SeriesHydrometersTable> seriesData = criteria.list();
        return seriesData;
    }

    @Finalize
    public void close() {
        session.close();
    }

    private AggregatedResult getAggregation( int type ) {
        LinkedHashMap<DateTime, Double> aggregatedMap = new LinkedHashMap<DateTime, Double>();
        List<Integer> numberOfValuesUsed = new ArrayList<Integer>();
        List<Double> varList = new ArrayList<Double>();
        List<double[]> quantilesList = new ArrayList<double[]>();

        Set<Entry<DateTime, Double>> entrySet = timestamp2Data.entrySet();

        Iterator<Entry<DateTime, Double>> iterator = entrySet.iterator();
        Entry<DateTime, Double> lastFromBefore = null;
        while( iterator.hasNext() ) {

            Entry<DateTime, Double> current = null;
            if (lastFromBefore != null) {
                /*
                 * from the second cycle on there is the last from the 
                 * cycle before that needs to be considered
                 */
                current = lastFromBefore;
            }
            Entry<DateTime, Double> previous = null;
            List<Double> valuesInTimeframe = new ArrayList<Double>();
            double mean = 0;
            int count = 0;
            while( iterator.hasNext() ) {
                if (current == null) {
                    current = iterator.next();
                }
                Double value = current.getValue();
                if (previous != null) {
                    DateTime currentDateTime = current.getKey();
                    DateTime previousDateTime = previous.getKey();
                    int currentT;
                    int previousT;
                    switch( type ) {
                    case 0:
                        // 0 = hour
                        currentT = currentDateTime.getHourOfDay();
                        previousT = previousDateTime.getHourOfDay();
                        break;
                    case 1:
                        // 1 = day
                        currentT = currentDateTime.getDayOfMonth();
                        previousT = previousDateTime.getDayOfMonth();
                        break;
                    case 2:
                        // 2 = month
                        currentT = currentDateTime.getMonthOfYear();
                        previousT = previousDateTime.getMonthOfYear();
                        break;
                    case 3:
                        // 3 = year
                        currentT = currentDateTime.getYear();
                        previousT = previousDateTime.getYear();
                        break;
                    default:
                        throw new IllegalArgumentException("Aggregation type not valid");
                    }

                    if (currentT != previousT) {
                        /*
                         * we read from the next date, so we need 
                         * to keep that value for the next cycle
                         */
                        lastFromBefore = current;
                        current = null;
                        break;
                    }
                }
                valuesInTimeframe.add(value);
                mean = mean + value;
                count++;

                previous = current;
                current = null;
            }

            // mean
            mean = mean / count;

            int size = valuesInTimeframe.size();
            if (size < 2) {
                // no data available
                continue;
            }
            double[] valuesArray = new double[size];
            boolean allNaN = true;
            for( int i = 0; i < size; i++ ) {
                valuesArray[i] = valuesInTimeframe.get(i);
                if (!HMConstants.isNovalue(valuesArray[i])) {
                    allNaN = false;
                }
            }

            // variance
            double var = calculateVariance(valuesArray, mean);
            varList.add(var);

            double[] quantiles = new double[]{NaN, NaN, NaN, NaN, NaN};
            if (valuesArray.length > 10 && !allNaN) {
                quantiles = calculateQuantiles(valuesArray);
            }
            quantilesList.add(quantiles);

            DateTime timestamp = previous.getKey();
            aggregatedMap.put(timestamp, mean);
            numberOfValuesUsed.add(count);
        }

        AggregatedResult result = new AggregatedResult(aggregatedMap, numberOfValuesUsed, varList, quantilesList);
        return result;
    }

    private double[] calculateQuantiles( double[] valuesArray ) {
        ModelsEngine modelsEngine = new ModelsEngine();
        IHMProgressMonitor pm = new DummyProgressMonitor();
        QuickSortAlgorithm t = new QuickSortAlgorithm(pm);
        t.sort(valuesArray, null);
        SplitVectors theSplit = new SplitVectors();
        int num_max = 1000;
        modelsEngine.split2realvectors(valuesArray, valuesArray, theSplit, 10, num_max, pm);

        double[][] outCb = new double[theSplit.splitIndex.length][3];
        double maxCum = 0;
        for( int h = 0; h < theSplit.splitIndex.length; h++ ) {
            outCb[h][0] = modelsEngine.doubleNMoment(theSplit.splitValues1[h], (int) theSplit.splitIndex[h], 0.0, 1.0, pm);
            outCb[h][1] = theSplit.splitIndex[h];
            if (h == 0) {
                outCb[h][2] = theSplit.splitIndex[h];
            } else {
                outCb[h][2] = outCb[h - 1][2] + theSplit.splitIndex[h];
            }
            maxCum = outCb[h][2];
        }

        List<Double> cumNormalizedList = new ArrayList<Double>();
        List<Double> valueList = new ArrayList<Double>();
        for( double[] record : outCb ) {
            cumNormalizedList.add(record[2] / maxCum);
            valueList.add(record[0]);
        }

        ListInterpolator listInterpolator = new ListInterpolator(cumNormalizedList, valueList);

        Double quantile10 = listInterpolator.linearInterpolateY(0.1);
        Double quantile25 = listInterpolator.linearInterpolateY(0.25);
        Double quantile50 = listInterpolator.linearInterpolateY(0.50);
        Double quantile75 = listInterpolator.linearInterpolateY(0.75);
        Double quantile90 = listInterpolator.linearInterpolateY(0.90);
        double[] q = new double[]{quantile10, quantile25, quantile50, quantile75, quantile90};
        return q;
    }

    private double calculateVariance( double[] valuesArray, double mean ) {
        double var = 0;
        for( double value : valuesArray ) {
            var = var + pow(value - mean, 2.0);
        }
        return var / (valuesArray.length + 1);
    }

    public AggregatedResult getHourlyAggregation() {
        return getAggregation(0);
    }

    public AggregatedResult getDailyAggregation() {
        return getAggregation(1);
    }

    public AggregatedResult getMonthlyAggregation() {
        return getAggregation(2);
    }

    public AggregatedResult getYearlyAggregation() {
        return getAggregation(3);
    }

    public void printReport() throws IOException {
        AggregatedResult monthlyAggregation;
        AggregatedResult yearlyAggregation;

        if (pAggregation == 2) {
            monthlyAggregation = outData;
        } else {
            monthlyAggregation = getMonthlyAggregation();
        }
        if (pAggregation == 3) {
            yearlyAggregation = outData;
        } else {
            yearlyAggregation = getYearlyAggregation();
        }

        LinkedHashMap<DateTime, Double> timestamp2ValueMap = monthlyAggregation.getTimestamp2ValueMap();
        Set<Entry<DateTime, Double>> entrySet = timestamp2ValueMap.entrySet();
        List<Integer> numList = monthlyAggregation.getValidDataNumber();
        Iterator<Integer> numIter = numList.iterator();

        TreeMap<Integer, YearResult> reportMap = new TreeMap<Integer, YearResult>();

        // first add month info
        for( Entry<DateTime, Double> entry : entrySet ) {
            DateTime dt = entry.getKey();

            int year = dt.getYear();

            YearResult yearResult = reportMap.get(year);
            if (yearResult == null) {
                yearResult = new YearResult();
                reportMap.put(year, yearResult);
            }

            int monthOfYear = dt.getMonthOfYear();
            Double monthlyMean = entry.getValue();
            Integer monthlyNum = numIter.next();

            yearResult.year = year;
            yearResult.monthmean[monthOfYear - 1] = monthlyMean;
            yearResult.validnums[monthOfYear - 1] = monthlyNum;
        }

        timestamp2ValueMap = yearlyAggregation.getTimestamp2ValueMap();
        entrySet = timestamp2ValueMap.entrySet();
        numList = yearlyAggregation.getValidDataNumber();
        numIter = numList.iterator();

        // then years info
        for( Entry<DateTime, Double> entry : entrySet ) {
            DateTime dt = entry.getKey();

            int year = dt.getYear();

            YearResult yearResult = reportMap.get(year);
            if (yearResult == null) {
                continue;
            }

            Double yearlyMean = entry.getValue();
            Integer yearlyNum = numIter.next();

            yearResult.yearmean = yearlyMean;
            yearResult.yearvalidnums = yearlyNum;
        }

        StringBuilder sB = new StringBuilder();
        sB
                .append("year, jan, dd, feb, dd, mar, dd, apr, dd, may, dd, jun, dd, jul, dd, aug, dd, sep, dd, oct, dd, nov, dd, dec, dd, yearly, dd\n");
        Set<Entry<Integer, YearResult>> reportSet = reportMap.entrySet();
        for( Entry<Integer, YearResult> report : reportSet ) {
            String string = report.getValue().toString();
            sB.append(string);
        }

        System.out.println(sB.toString());
    }

    private static class YearResult {
        int year = -1;
        double yearmean = NaN;
        int yearvalidnums = -1;
        double[] monthmean = new double[]{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN};
        int[] validnums = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

        @Override
        public String toString() {
            StringBuilder sB = new StringBuilder();
            // sB.append("year, jan, dd, feb, dd, mar, dd, apr, dd, may, dd, jun, dd, jul, dd, aug, dd, sep, dd, oct, dd, nov, dd, dec, dd, yearly, dd\n");
            sB.append(year).append(",");
            sB.append(monthmean[0]).append(",").append(validnums[0]).append(",");
            sB.append(monthmean[1]).append(",").append(validnums[1]).append(",");
            sB.append(monthmean[2]).append(",").append(validnums[2]).append(",");
            sB.append(monthmean[3]).append(",").append(validnums[3]).append(",");
            sB.append(monthmean[4]).append(",").append(validnums[4]).append(",");
            sB.append(monthmean[5]).append(",").append(validnums[5]).append(",");
            sB.append(monthmean[6]).append(",").append(validnums[6]).append(",");
            sB.append(monthmean[7]).append(",").append(validnums[7]).append(",");
            sB.append(monthmean[8]).append(",").append(validnums[8]).append(",");
            sB.append(monthmean[9]).append(",").append(validnums[9]).append(",");
            sB.append(monthmean[10]).append(",").append(validnums[10]).append(",");
            sB.append(monthmean[11]).append(",").append(validnums[11]).append(",");
            sB.append(yearmean).append(",").append(yearvalidnums).append("\n");

            return sB.toString();
        }

    }

}
