/*
 * $Id:$
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 *  1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software
 *     in a product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 * 
 *  2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 * 
 *  3. This notice may not be removed or altered from any source
 *     distribution.
 */
package oms3.io;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatedData { 
       // holds date and data arrays so can fill from data then compute things 
       // like mean monthly based on date
      
       // Data TimeSteps
       // kmolson TODO- move to SimConst?
       public static final int Daily = 0;
       public static final int MeanMonthly = 1;
       public static final int MonthlyMean = 2;
       public static final int AnnualMean = 3;
       public static final int PeriodMedian = 4;
       public static final int PeriodStandardDeviation = 5;
       public static final int PeriodMininum = 6;
       public static final int PeriodMaximum = 7;
       
       private List<Date> dates = new ArrayList<Date>();
       private List<Double> datas = new ArrayList<Double>();
       
      
       public DatedData() {
       }
       
       public DatedData(Date date, Double data){
       dates.add(date);
       datas.add(data);
       }
     
        public void add(Date date, Double data) {
             dates.add(date);
             datas.add(data);
       }
    
       public void addDate(Date date){
           dates.add(date);
       }
       
       public void addData(Double data){
           datas.add(data);
       } 
        
       public Date getDate(int i) {
           return dates.get(i);
       }    
        public Double getData(int i) {
           return datas.get(i);
       }  
        
       public int getNumDates() {
           return dates.size();
       }
       public int getNumData() {
           return datas.size();
       }
       
       
       public void writeDate(int i, Date date) {
           dates.set(i, date);
       }
       public void writeData(int i, Double data) {
               datas.set(i, data);
       }
     
       
       
       // ---------------------------------------------
       // TimeStep functions for data
       // ---------------------------------------------
       
      
      // ---------------------------------------------
       public double[] getMeanMonthly () {
      // ---------------------------------------------
         // Provides a per-month mean of data (across all years for that month). 
         // e.g. 12 means returned- one for all days in January in all years, 
         //      one for all dats in Feb in all years, etc.
                
        // Advance through all data, and sum each month's value.
        int monthlyCount[]; //the number of entries per month
        double monthlySum[]; //the sum of the data per month
        double meanMonthly[]; // The mean value per month
        
        monthlyCount = new int[12]; // Just need count and sum for 12 months
        monthlySum = new double[12];
        meanMonthly = new double[12];
        
        // Init data to clear.
        for (int i=0; i<12; i++) { 
            monthlyCount[i] = 0;
            monthlySum[i] = 0;
            meanMonthly[i] = 0;
        }
       
        int totalCount = 0;
       
        for (int i=0; i<this.getNumData(); i++) {
            Date date = this.getDate(i);
            double data = this.getData(i);
            int month = -1;
            
            if (i > this.getNumDates()) {throw new IllegalArgumentException("No date for data #" + i);}
            
            // Get month data out of date.
            month = date.getMonth();
           
             if (month > 11) {throw new IllegalArgumentException("Month number data out of range: " + month);}
             
            // Add to appropriate sum.
            monthlyCount[month]++;
            monthlySum[month] +=data;
            totalCount++;
        }
       
        if (totalCount == 0) {throw new IllegalArgumentException("No data used in Mean Montly calculation.");}
       
        for (int i=0; i<12; i++) {
          meanMonthly[i] = monthlySum[i]/monthlyCount[i];
        }
       
        return meanMonthly;
       
       }
       
       
       // ---------------------------------------------
        public double[] getMonthlyMean () {
       // ---------------------------------------------
            // Provides a mean for days in each month/year pair.  (12 months x N years of data)
            // e.g. (12 x N years) of means returned- one for all days in January in 1990,
            //      one for all dats in Feb in 1990, ... one for all days in January 1991, etc.
            //
            // Sum the daily data until the month/year changes then 
            // store the mean.
         
         List<Double> monthlyMean = new ArrayList<Double>();
       
    
        int totalCount = 0;
        int previousYear = -1;
        int previousMonth = -1;
        boolean currentValid = false;
        
        for (int i=0; i<this.getNumData(); i++) {
            int month = -1;
            int year = -1;
            double currentSum = 0;
            double currentCount = 0;
            
            Date date = this.getDate(i);
            double data = this.getData(i);
            
            if (i > this.getNumDates()) {throw new IllegalArgumentException("No matching date info for data #" + i);}
            
            // Get month data out of date.
            month = date.getMonth(); // kmolson TODO- use calendar instead?
            year = date.getYear();
            
             if (month > 11) {throw new IllegalArgumentException("Month number data out of range: " + month);}
             
             // Check if month or year advances.
             
             // This assumes data is in Chronological order.  If not, we need to save and 
             // scan back for matching month/year pairs already written.
             // Could add min and max year to data fields of DatedData class and allocate based on that.
             // I was just worried about access efficiency... if data is in Chronological order, 
             // we don't need to worry about it, not keeping track is faster.
                
             boolean sameMonthYear = (month == previousMonth) && (year==previousYear);
             
             if (sameMonthYear) {
                 // Same month/year pair, so update current entry.
                 currentSum += data;
                 currentCount++;
                 currentValid = true;
             }
             
             boolean isLastData = i==(this.getNumData()-1);
             if (currentValid && (!sameMonthYear || isLastData)) {
                // If moving to new month or year,  or it is the last data, add the mean to the result array.
                 if (currentValid) {
                     double currentMean = currentSum/currentCount;
                     monthlyMean.add(currentMean);
                 }
                 
                 // Set current to new data and a count of 1
                 currentCount = 1;
                 currentSum = data;
          
             }
             
            previousMonth = month;
            previousYear = year; 
            // Add to appropriate sum
            totalCount++;
        }
       
        if (totalCount == 0) {throw new IllegalArgumentException("No data used in Mean Monthly calculation.");}
       
       // Assign MonthlyMean data to output array.
       double[] arr = new double[monthlyMean.size()];
       for (int i=0; i < monthlyMean.size(); i++) {
            arr[i] = monthlyMean.get(i);
         }
       return arr;
       }
        
        
        
        public double[] getOriginalData() {
            // Get source data as is, without any date-based computation.
            double[] originalData = new double[this.datas.size()];
            return originalData;
        }
        public double[] getDailyData(){
            // kmoson TODO- Do we want to take the mean of the 
            // subdaily data if there is any, or is daily the mininum granularity?
            double[] originalData = new double[this.datas.size()];
            return originalData;  
        }
        
       
       public double[] computeData(int TimeStep) {
           // kmolson- timestep originally comes from OF timestep in .luca file.
           switch (TimeStep) {
               case Daily : { return this.getDailyData(); }
               case MeanMonthly : { return this.getMeanMonthly(); }
               case MonthlyMean : { return this.getMonthlyMean(); }
               default : return this.getOriginalData(); }    
           }
}

                   
                   
    
  