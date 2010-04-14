package eu.hydrologis.edc.oms.datareader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
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
import eu.hydrologis.jgrass.jgrassgears.utils.math.ListInterpolator;

@SuppressWarnings("nls")
public class HydrometerSeriesReader implements ITimeseriesAggregator {
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

    // @Description("The timestep (in minutes) filter for the fetched data.")
    // @In
    // public int tTimestep;

    @Description("The scaletype to do level to discharge conversions and corrections.")
    @In
    public long pTypeid;

    @Description("The aggregation type of the fetched data. (0 = hour, 1 = day, 2 = month, 3 = year)")
    @In
    public int pAggregation;

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

        List<SeriesHydrometersTable> seriesData = getSeriesData(startDateTime, endDateTime);
        timestamp2Data = new LinkedHashMap<DateTime, Double>();
        LinkedHashMap<Double, Double> dischargeScale = getDischargeScale();

        for( SeriesHydrometersTable seriesHydrometersTable : seriesData ) {
            DateTime timestampUtc = seriesHydrometersTable.getTimestampUtc();
            DateTime dateTime = timestampUtc.toDateTime(DateTimeZone.UTC);
            Double value = seriesHydrometersTable.getValue();

            if (dischargeScale != null) {
                value = dischargeScale.get(value);

                if (value == null) {
                    value = getInterpolated(dischargeScale, value);
                }
            }

            timestamp2Data.put(dateTime, value);
        }

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
            List<Double> yList = new ArrayList<Double>();
            List<Double> xList = new ArrayList<Double>();
            Set<Entry<Double, Double>> entrySet = dischargeScale.entrySet();
            for( Entry<Double, Double> entry : entrySet ) {
                Double v1 = entry.getKey();
                Double v2 = entry.getValue();
                xList.add(v1);
                xList.add(v2);
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

                mean = mean + value;
                count++;

                previous = current;
                current = null;
            }
            mean = mean / count;

            DateTime timestamp = previous.getKey();
            aggregatedMap.put(timestamp, mean);
            numberOfValuesUsed.add(count);
        }

        AggregatedResult result = new AggregatedResult(aggregatedMap, numberOfValuesUsed);
        return result;
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

}
