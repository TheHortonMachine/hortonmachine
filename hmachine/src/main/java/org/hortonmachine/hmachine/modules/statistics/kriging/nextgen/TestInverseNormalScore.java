package org.hortonmachine.hmachine.modules.statistics.kriging.nextgen;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.math.SimpleStats;
import org.hortonmachine.gears.utils.math.interpolation.LinearArrayInterpolator;
import org.hortonmachine.gears.utils.math.interpolation.LinearListInterpolator;

public class TestInverseNormalScore {

    public TestInverseNormalScore() throws Exception {
        String path = "C:/Users/hydrologis/Dropbox/hydrologis/lavori/2022_unitn/testKriging/adige_test_dataKriging2.csv";
        TiesStrategy strategy = TiesStrategy.AVERAGE;

        List<String> lines = FileUtilities.readFileToLinesList(new File(path));

        List<Double> interpolatedValues = new ArrayList<>();
        List<Double> orderedInterpolatedValues = new ArrayList<>();
        for( int i = 1; i < lines.size(); i++ ) {
            String line = lines.get(i);
            String[] lineSplit = line.split(",");
            double d = Double.parseDouble(lineSplit[2]);
            if (d != -9999) {
                interpolatedValues.add(d);
                orderedInterpolatedValues.add(d);
            }
        }
        Collections.sort(orderedInterpolatedValues);
        // calculate ranking
        double[] orderedInterpolatedValuesArray = new double[orderedInterpolatedValues.size()];
        for( int i = 0; i < orderedInterpolatedValuesArray.length; i++ ) {
            orderedInterpolatedValuesArray[i] = orderedInterpolatedValues.get(i);
        }
        NaturalRanking nr = new NaturalRanking(strategy);
        double[] rank = nr.rank(orderedInterpolatedValuesArray);
        List<Double> rankInterpolatedValues = new ArrayList<>();
        for( int i = 0; i < rank.length; i++ ) {
            rankInterpolatedValues.add(rank[i]);
        }
        List<Double> uniqueRankInterpolatedValues = rankInterpolatedValues.stream().distinct().collect(Collectors.toList());
        // z_star
        List<Double> uniqueOrderedInterpolatedValues = orderedInterpolatedValues.stream().distinct().collect(Collectors.toList());

        // funzione densita' di prob cumulata dei valori interpolati -> 
        // sono distribuiti come una Normale con media 0 e varianza 1
        List<Double> ppfNormalG = new ArrayList<Double>();
        NormalDistribution nd = new NormalDistribution(0, 1);
        for( int i = 0; i < uniqueOrderedInterpolatedValues.size(); i++ ) {
            double v = nd.cumulativeProbability(uniqueOrderedInterpolatedValues.get(i));
            ppfNormalG.add(v);
        }
        
        // la F(z) dei dati originali è nota solo in forma discreta, il calcolo dell'inversa F-1(z)
        // deve essere fatto tramite interpolazione. La tecnica di interpolazione dipende dalla
        // posizione del valore della G rispetto alla F originale: 
        // lower tail: F                   -> z_star[G<min(F)] ------- G[G<min(F)]
        // parte centrale (interpolazione) -> z_star[(G>=min(F))*(G<=max(F))] ------- G[(G>=min(F))*(G<=max(F))]
        // upper tail                      -> z_star[G>max(F)] ------- G[G>max(F)]

        // Inserisco la F(z) manualmente per questo tentativo...
        double[] cdfOriginalValuesF = {0.1,0.2,0.35,0.5,0.6,0.7,0.8,0.9};
        double[] originalMeasuredValuesZ = {1.3,2.3,3.2,4.2,4.7,5.7,6.2,8.2};
        
        DoubleSummaryStatistics stat = Arrays.stream(cdfOriginalValuesF).summaryStatistics();
        double minCdfOriginalValuesF = stat.getMin();
        double maxCdfOriginalValuesF = stat.getMax();
        DoubleSummaryStatistics stat2 = Arrays.stream(originalMeasuredValuesZ).summaryStatistics();
        double minCdfOriginalMeasuredValues = stat.getMin();
        double maxCdfOriginalMeasuredValues = stat.getMax();
        
        
        // create tails for the original values CDF function -> F(z)
        List<Double> lowerTailInterpolatedValues = new ArrayList<Double>();
        List<Double> upperTailInterpolatedValues = new ArrayList<Double>();
        List<Double> middleTailInterpolatedValues = new ArrayList<Double>();
        double currentInterpolatedValue = Double.NaN;
        
        for (int j = 0; j < ppfNormalG.size()-1; j++) {
        	currentInterpolatedValue = uniqueOrderedInterpolatedValues.get(j);
        	if (ppfNormalG.get(j) < minCdfOriginalValuesF) {
        		lowerTailInterpolatedValues.add(currentInterpolatedValue);
        	} else if (ppfNormalG.get(j) > maxCdfOriginalValuesF) {
        		upperTailInterpolatedValues.add(currentInterpolatedValue);
        	} else {
        		middleTailInterpolatedValues.add(currentInterpolatedValue);
			}
		}

        // create tails for the interpolated values CDF function -> G(z)
        List<Double> lowerTailPpfG = new ArrayList<Double>();
        List<Double> upperTailPpfG = new ArrayList<Double>();
        List<Double> middleTailPpfG = new ArrayList<Double>();
        double currentppfValueG = Double.NaN;
        
        for (int j = 0; j < ppfNormalG.size(); j++) {
        	currentppfValueG = ppfNormalG.get(j);
        	if (ppfNormalG.get(j) < minCdfOriginalValuesF) {
        		lowerTailPpfG.add(currentppfValueG);
        	} else if (ppfNormalG.get(j) > maxCdfOriginalValuesF) {
        		upperTailPpfG.add(currentppfValueG);
        	} else {
        		middleTailPpfG.add(currentppfValueG);
        	}
        }

        // Calcolo ora la F-1(z) partendo da G nelle tre fasce
        
        // middle tail -> interpolation
        LinearArrayInterpolator llInt = new LinearArrayInterpolator(cdfOriginalValuesF, originalMeasuredValuesZ);
        System.out.println("-----------------");
        for (int i = 0; i < middleTailPpfG.size(); i++) {
        	double value = middleTailPpfG.get(i);
        	double inverseInterpolatedMiddleG = llInt.getInterpolated(value);
        	System.out.println(inverseInterpolatedMiddleG);
		}
        System.out.println("-----------------");
        
        // lower tail -> use the expression G(z) = F(z1) [(z-z_min)/ (z1-zmin)]^(omega>1)
        System.out.println("lower tail");
        double omega = 5.0;
        double z_min = 0.0;
        for (int i = 0; i < lowerTailPpfG.size(); i++) {
        	double value = lowerTailPpfG.get(i);
        	double inverseInterpolatedLowerG = Math.pow((value/minCdfOriginalValuesF),(1/omega))*(minCdfOriginalMeasuredValues-z_min) + z_min;
        	System.out.println(inverseInterpolatedLowerG);
        }
        System.out.println("-----------------");
        
        // upper tail -> use the expression z = (-llambda/(upper_tail_G-1))^(1/1.5)
        System.out.println("upper tail");
        double omegaUp = 1.5;
        for (int i = 0; i < upperTailPpfG.size(); i++) {
        	double value = upperTailPpfG.get(i);
        	double lambda = Math.pow(maxCdfOriginalMeasuredValues,1.5) * (1-maxCdfOriginalValuesF);
        	double inverseInterpolatedUpperG = Math.pow((-lambda / (value - 1)),(1/omegaUp));
        	System.out.println(inverseInterpolatedUpperG);
        }
        System.out.println("-----------------");
        
        
        String collect = orderedInterpolatedValues.stream().map(d -> d.toString()).collect(Collectors.joining(","));
        System.out.println(collect);
        collect = interpolatedValues.stream().map(d -> d.toString()).collect(Collectors.joining(","));
        System.out.println(collect);
        collect = rankInterpolatedValues.stream().map(d -> d.toString()).collect(Collectors.joining(","));
        System.out.println(collect);
        collect = uniqueRankInterpolatedValues.stream().map(d -> d.toString()).collect(Collectors.joining(","));
        System.out.println(collect);
        collect = uniqueOrderedInterpolatedValues.stream().map(d -> d.toString()).collect(Collectors.joining(","));
        System.out.println(collect);
        collect = ppfNormalG.stream().map(d -> d.toString()).collect(Collectors.joining(","));
        System.out.println(collect);
//        collect = cdfOriginalValuesF.stream().map(d -> d.toString()).collect(Collectors.joining(","));
//        System.out.println(collect);
        System.out.println(minCdfOriginalValuesF);
        System.out.println(maxCdfOriginalValuesF);
        collect = lowerTailInterpolatedValues.stream().map(d -> d.toString()).collect(Collectors.joining(","));
        System.out.println(collect);
        collect = middleTailInterpolatedValues.stream().map(d -> d.toString()).collect(Collectors.joining(","));
        System.out.println(collect);
        collect = upperTailInterpolatedValues.stream().map(d -> d.toString()).collect(Collectors.joining(","));
        System.out.println(collect);
        collect = lowerTailPpfG.stream().map(d -> d.toString()).collect(Collectors.joining(","));
        System.out.println(collect);
        collect = middleTailPpfG.stream().map(d -> d.toString()).collect(Collectors.joining(","));
        System.out.println(collect);
        collect = upperTailPpfG.stream().map(d -> d.toString()).collect(Collectors.joining(","));
        System.out.println(collect);
//        collect = ppfNormal.stream().map(d -> d.toString()).collect(Collectors.joining(","));
//        System.out.println(collect);
//        collect = interpolatedValues.stream().map(d -> d.toString()).collect(Collectors.joining(","));
//        System.out.println(collect);

    }

    public static void main( String[] args ) throws Exception {
        new TestInverseNormalScore();
    }

}
