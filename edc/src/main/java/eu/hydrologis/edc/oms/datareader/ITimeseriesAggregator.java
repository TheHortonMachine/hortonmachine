package eu.hydrologis.edc.oms.datareader;

public interface ITimeseriesAggregator {
    
    public AggregatedResult getHourlyAggregation();

    public AggregatedResult getDailyAggregation();

    public AggregatedResult getMonthlyAggregation();

    public AggregatedResult getYearlyAggregation();

}
