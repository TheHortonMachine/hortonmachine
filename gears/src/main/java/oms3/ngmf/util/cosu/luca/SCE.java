package oms3.ngmf.util.cosu.luca;
//package ngmf.util.cosu.luca;
//
//import java.io.PrintStream;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Vector;
//import oms3.dsl.cosu.Step;
//
///**
// */
//public class SCE {
//
//    double[] initialParameterSet;    //INITIAL PARAMETER SET
//    double[] lowerBound;    //LOWER BOUND ON PARAMETERS
//    double[] upperBound;    // UPPER BOUND ON PARAMETERS
//    int numOfParams;     // NUMBER OF PARAMETERS TO BE OPTIMIZED
//    int initNumOfComplexes;    // NUMBER OF COMPLEXES IN THE INITIAL POPULATION
//    int numOfPointsInComplex;    // NUMBER OF POINTS IN EACH COMPLEX
//    int initTotalNumOfPoints;    //(initTotalNumOfPoints = initNumOfComplexes * numOfPointsInComplex
//
//    //TOTAL NUMBER OF POINTS IN INITIAL POPULATION (NPT=NGS*NPG)
//    int numOfPointsInSubComplex;    //NUMBER OF POINTS IN A SUB-COMPLEX
//    int numOfEvolutionSteps;    //NUMBER OF EVOLUTION STEPS ALLOWED FOR EACH COMPLEX BEFORE
//    //COMPLEX SHUFFLING
//    int minNumOfComplexes;    //MINIMUM NUMBER OF COMPLEXES REQUIRED, IF THE NUMBER OF
//    //COMPLEXES IS ALLOWED TO REDUCE AS THE OPTIMIZATION PROCEEDS
//    //ISEED = INITIAL RANDOM SEED
//    //INIFLG = FLAG ON WHETHER TO INCLUDE THE INITIAL POINT IN POPULATION
//    // = 0, NOT INCLUDED
//    //= 1, INCLUDED
//    boolean includeInitialPOINT;
//    //CONVERGENCE CHECK PARAMETERS
//    //MAXN = MAX NO. OF TRIALS ALLOWED BEFORE OPTIMIZATION IS TERMINATED
//    int maxNumOfTrials;
//    // NUMBER OF SHUFFLING LOOPS IN WHICH THE CRITERION VALUE MUST
//    //  CHANGE BY THE GIVEN PERCENTAGE BEFORE OPTIMIZATION IS TERMINATED
//    int numOfShufflingLoops;
//    //PERCENTAGE BY WHICH THE CRITERION VALUE MUST CHANGE IN
//    // GIVEN NUMBER OF SHUFFLING LOOPS
//    double percentage;
//    // FLAG INDICATING WHETHER PARAMETER CONVERGENCE IS REACHED
//    boolean paramConvergenceSATISFIED;
//
//    //COORDINATES OF POINTS IN THE POPULATION
//    double[][] pointsX;
//    //FUNCTION VALUES OF X(.,.)
//    double[] objFuncValueOfX;
//    //COORDINATES OF A SINGLE POINT IN X
//    double[] pointInX;
//    //COORDINATES OF POINTS IN A COMPLEX
//    double[][] pointsInComplex;
//    //FUNCTION VALUES OF CX(.,.)
//    double[] objFuncValuesOfComplex;
//    //COORDINATES OF POINTS IN THE CURRENT SIMPLEX
//    double[][] pointsInSimplex;
//    //FUNCTION VALUES OF S(.,.)
//    double[] objFuncValuesOfSimplex;
//    //WORST POINT AT CURRENT SHUFFLING LOOP
//    double[] worstPoint;
//    //FUNCTION VALUE OF WORSTX(.)
//    double objFuncValueOfWorstPoint;
//    //STANDARD DEVIATION OF PARAMETERS IN THE POPULATION
//    double[] stdDevOfPopulation;
//    //NORMALIZED GEOMETRIC MEAN OF PARAMETER RANGES
//    double normalizedGeometricMean;
//    //INDICES LOCATING POSITION OF S(.,.) IN X(.,.)
//    int[] indicesOfSimplex;
//    //BOUND ON ITH VARIABLE BEING OPTIMIZED
//    double[] bound;
//    //NUMBER OF COMPLEXES IN CURRENT POPULATION
//    int currentNumOfComplexes;
//    //NUMBER OF COMPLEXES IN LAST POPULATION
//    int lastNumOfComplexes;
//    double[] bestCriterion;
//
//
//    int totalNumOfPoints; // the number of points in current population
//    double[] initialPoint; // initial point == initialParameterSet
//    //
//    private ExecutionHandle executionHandle;
//    Step stepData;
//    Step.Data data;
//    int NLOOP = 0;
//    int LOOP = 0;
//    int IGS = 0;
//    int icall = 0;
//    //
//
//    PrintStream out = System.out;
//
//    public SCE(ExecutionHandle executionHandle, Step stepData, Step.Data data)  {
//
//        this.executionHandle = executionHandle;
//        this.stepData = stepData; // stepData contains data needed for running SCE
//        this.data = data;
//
//        // get values
//        numOfParams = data.getParamValues().length;
//        //numOfParams = stepData.params().getCount();
//        
//        initNumOfComplexes = stepData.getInitComplexes();
//        numOfPointsInComplex = stepData.getPointsPerComplex();
//        numOfPointsInSubComplex = stepData.getPointsPerSubcomplex();
//        numOfEvolutionSteps = stepData.getEvolutions();
//        minNumOfComplexes = stepData.getMinComplexes();
//        includeInitialPOINT = true;
//        maxNumOfTrials = stepData.getMaxExec();
//        numOfShufflingLoops = stepData.getShufflingLoops();
//        percentage = stepData.getOfPercentage();
//        upperBound = data.getUpperBound();
//        lowerBound = data.getLowerBound();
//        initialParameterSet = data.getParamValues();
//        
////        System.out.println(maxNumOfTrials + " " + initNumOfComplexes + " " + numOfPointsInComplex + " " + numOfPointsInSubComplex + " " + numOfEvolutionSteps
////                + " " + minNumOfComplexes + " " + numOfShufflingLoops + " " + percentage);
//
//        initTotalNumOfPoints = initNumOfComplexes * numOfPointsInComplex;
//
//        // initialize arrays
//        pointsX = new double[initTotalNumOfPoints][numOfParams];
//        objFuncValueOfX = new double[initTotalNumOfPoints];
//        pointInX = new double[numOfParams];
//
//        pointsInComplex = new double[numOfPointsInComplex][numOfParams];
//        objFuncValuesOfComplex = new double[numOfPointsInComplex];
//
//        pointsInSimplex = new double[numOfPointsInSubComplex][numOfParams];
//        objFuncValuesOfSimplex = new double[numOfPointsInSubComplex];
//        worstPoint = new double[numOfParams];
//
//        stdDevOfPopulation = new double[numOfParams];
//        indicesOfSimplex = new int[numOfPointsInSubComplex];
//        bound = new double[numOfParams];
//        bestCriterion = new double[10];
//        initialPoint = new double[numOfParams];
//    }
//
//    public void setOut(PrintStream out) {
//        this.out = out;
//    }
//
//    public void run() throws Exception {
//        currentNumOfComplexes = initNumOfComplexes;
//        totalNumOfPoints = initTotalNumOfPoints;
//        double objFuncValue;
//
//        for (int j = 0; j < numOfParams; j++) {
//            bound[j] = upperBound[j] - lowerBound[j];
//            initialPoint[j] = initialParameterSet[j];
//        }
//
//        objFuncValue = execute(initialPoint); // write initialPoint in the 'newPARAMS' file, executes runMMS and SRobjfun()
//
//        out.println("\n Initial OF value : " + objFuncValue);
//        out.print(" Initial Parameterset : ");
//        for (int j = 0; j < initialParameterSet.length; j++) {
//            out.print(" " + initialParameterSet[j]);
//        }
//        out.println();
//
//        if (maxNumOfTrials <= 1) {
//            out.println("Due to max model execution of 1, no optimization was done. " +
//                    "The only thing that was done is the calculation of objective function for initial values.");
//            return; // done with this method
//        }
//
//        out.println(" SCE generating data points ....");
//        if (includeInitialPOINT) {
////            out.println("Initial point will be included");
//            for (int j = 0; j < numOfParams; j++) {
//                pointsX[0][j] = initialParameterSet[j];
//            }
//            objFuncValueOfX[0] = objFuncValue;
//        } else {
////            out.println("Initial point won't be included");
//            for (int j = 0; j < numOfParams; j++) {
//                pointsX[0][j] = lowerBound[j] + bound[j] * Math.random();
//                pointInX[j] = pointsX[0][j];
//            }
//            // write pointInX in the 'newPARAMS' file, executes runMMS and SRobjfun()
//            objFuncValue = execute(pointInX);
//            objFuncValueOfX[0] = objFuncValue;
//        }
//
//        data.setObjFuncValueOfBestPoint(objFuncValueOfX[0]);
//        int outputType = 1;
//        if (icall < maxNumOfTrials) {
//            for (int i = 1; i < totalNumOfPoints; i++) {
//                for (int j = 0; j < numOfParams; j++) {
//                    pointsX[i][j] = lowerBound[j] + bound[j] * Math.random();
//                    pointInX[j] = pointsX[i][j];
//                }
//
//                objFuncValueOfX[i] = execute(pointInX);
//                //ICALL++;
//                if (icall >= maxNumOfTrials) {
//                    totalNumOfPoints = i + 1;
//                    pointsX = copy(pointsX, totalNumOfPoints);
//                    objFuncValueOfX = copy(objFuncValueOfX, totalNumOfPoints);
//                    break;
//                }
//            }
////            out.println("size of pointsX = " + totalNumOfPoints + " (max size is " + pointsX.length + ")" +
////                    ", max size of objFuncValueOfX = " + objFuncValueOfX.length);
//
//            sort_duan(pointsX, objFuncValueOfX);
//
//            // set the best point and its objective function value
//            data.setBestParamData(pointsX[0], objFuncValueOfX[0]);
//            for (int j = 0; j < numOfParams; j++) {
//                worstPoint[j] = pointsX[totalNumOfPoints - 1][j];
//            }
//            objFuncValueOfWorstPoint = objFuncValueOfX[totalNumOfPoints - 1];
//
//            parstt();
////            double distribution = normdistForBestPoint();
//            out.print("\n Results of the initial SCE research:");
////            out.println("NLOOP: " + NLOOP);
////            out.println("ICALL: " + icall);
////            out.println("Number of complexes in current population: " + currentNumOfComplexes);
////            out.println("Function value of best point: " + data.getObjFuncValueOfBestPoint());
////            out.println("Function value of worst point: " + objFuncValueOfWorstPoint);
////            out.println("DIST for best point (DIST(0)): " + distribution);
////            out.println("Best point at current shuffling loop");
//            for (int j = 0; j < numOfParams; j++) {
//                out.print(" " + pointsX[0][j]); // display the best point
//            }
//            out.println();
//            if (icall >= maxNumOfTrials) {
//                outputType = 1;
//            } else if (paramConvergenceSATISFIED) {
//                outputType = 3;
//            } else {
//                outputType = mainLoop();
//            }
//        }
//
//        /*************************************************************************
//         * Creating a parameter file that contains the best point. This is not done
//         * in the original SCE code.
//         **************************************************************************/
//        // get the best point
//        double[] bestPoint = data.getBestParamDataArray();
//        data.setParamValues(bestPoint); // set the best point in stepData
//        executionHandle.writeParameterFile(data);
//
//        String output = "";
//        out.println("\n **************************************************");
//        if (outputType == 1) {
//            output = " Optimization terminated, limit " +
//                    "on the maximum number of trials, " + maxNumOfTrials + ", was exceeded.\n" +
//                    " Search was stopped at sub-complex " + (LOOP + 1) + " of complex " + (IGS + 1) +
//                    " in shuffling loop. " + NLOOP;// + "\n" +
//        } else if (outputType == 2) {
////            double percentage2 = percentage * 100;
//            output = " Optimization terminated, OF value " +
//                    "has not changed " + percentage + "% in " + numOfShufflingLoops +
//                    " shuffling loops.";
//        } else if (outputType == 3) {
//            double normalizedGeometricMean2 = normalizedGeometricMean * 100;
//            output = " Optimization terminated, population has " +
//                    "converged into " + normalizedGeometricMean2 + "% of the feasible space.";
//        } else if (outputType == 4) {
//            output = " SCE was stopped.";
//        }
//
//        out.println(output);
//        out.println(" **************************************************");
//        out.print(" Final parameter estimates:");
//        for (int j = 0; j < numOfParams; j++) {
//            out.print(" " + bestPoint[j]);
//            initialParameterSet[j] = bestPoint[j];
//        }
//        out.println();
//        out.println(" Final OF value: " + data.getObjFuncValueOfBestPoint());
//    }
//
//    //  MAIN LOOP
//    private int mainLoop() throws Exception {
//        out.println(" SCE shuffling ....");
//        int outputType = 1; // different output will be displayed depending on the value of ouputTYpe
//        while (true) {
//            NLOOP++;
//            for (IGS = 0; IGS < currentNumOfComplexes; IGS++) {
//                for (int k1 = 0; k1 < numOfPointsInComplex; k1++) {
//                    int k2 = k1 * currentNumOfComplexes + IGS;
//                    for (int j = 0; j < numOfParams; j++) {
//                        pointsInComplex[k1][j] = pointsX[k2][j];
//                    }
//                    objFuncValuesOfComplex[k1] = objFuncValueOfX[k2];
//                }
//                for (LOOP = 0; LOOP < numOfEvolutionSteps; LOOP++) {
//                    if (numOfPointsInSubComplex == numOfPointsInComplex) {
//                        for (int k = 0; k < numOfPointsInSubComplex; k++) {
//                            indicesOfSimplex[k] = k;
//                        }
//                    } else {
//                        //  k = 0 instead of k = 1 because the line above (indicesOfSimplex[0] = ....)
//                        // is removed.
//                        for (int k = 0; k < numOfPointsInSubComplex; k++) {
//                            boolean again = true;
//                            int lpos = -1;
//                            while (again) {
//                                again = false;
//                                lpos = (int) (numOfPointsInComplex + 0.5 -
//                                        Math.sqrt(Math.pow((numOfPointsInComplex + 0.5), 2) -
//                                        numOfPointsInComplex * (numOfPointsInComplex + 1) * Math.random()));
//                                // check if any element from indicesOfSimplex[0] to indicesOfSimplex[k-1]
//                                // is equal to LPOS. If not, get out of the for loop, finish the while(AGAIN) loop,
//                                // and set LPOS as a value of indicesOfSimplex[k]
//                                for (int k1 = 0; k1 < k; k1++) {
//                                    if (lpos == indicesOfSimplex[k1]) {
//                                        again = true;
//                                        break;
//                                    }
//                                }
//                            }
//                            indicesOfSimplex[k] = lpos;
//                        }
//                        // sort the indiciesOfSimplex array in increasing order
//                        Arrays.sort(indicesOfSimplex);
//                    }
//
//                    for (int k = 0; k < numOfPointsInSubComplex; k++) {
//                        for (int j = 0; j < numOfParams; j++) {
//                            pointsInSimplex[k][j] = pointsInComplex[indicesOfSimplex[k]][j];
//                        }
//                        objFuncValuesOfSimplex[k] = objFuncValuesOfComplex[indicesOfSimplex[k]];
//                    }
//                    cce();
//                    for (int k = 0; k < numOfPointsInSubComplex; k++) {
//                        for (int j = 0; j < numOfParams; j++) {
//                            pointsInComplex[indicesOfSimplex[k]][j] = pointsInSimplex[k][j];
//                        }
//                        objFuncValuesOfComplex[indicesOfSimplex[k]] = objFuncValuesOfSimplex[k];
//                    }
//                    sort_duan(pointsInComplex, objFuncValuesOfComplex);
//                    if (icall >= maxNumOfTrials) {
//                        break;
//                    }
//                } // end of loop with LOOP
//                for (int k1 = 0; k1 < numOfPointsInComplex; k1++) {
//                    int k2 = k1 * currentNumOfComplexes + IGS;
//                    for (int j = 0; j < numOfParams; j++) {
//                        pointsX[k2][j] = pointsInComplex[k1][j];
//                    }
//                    objFuncValueOfX[k2] = objFuncValuesOfComplex[k1];
//                }
//                if (icall >= maxNumOfTrials) {
//                    break;
//                }
//            } // end of for loop with IGS
//
//            sort_duan(pointsX, objFuncValueOfX);
//            // set the best point and its objective function value
//            data.setBestParamData(pointsX[0], objFuncValueOfX[0]);
//            for (int j = 0; j < numOfParams; j++) {
//                worstPoint[j] = pointsX[totalNumOfPoints - 1][j];
//            }
//            objFuncValueOfWorstPoint = objFuncValueOfX[totalNumOfPoints - 1];
//
//            parstt();
////            double distribution = normdistForBestPoint();
////            out.println("loop " + NLOOP + "  ICALL = " + icall);
////            out.println("Number of complexes in a current population: " + currentNumOfComplexes);
////            out.println("Objective Function value of best point: " + data.getObjFuncValueOfBestPoint());
////            out.println("Objective Function value of worst point: " + objFuncValueOfWorstPoint);
////            out.println("Normal Distribution of best point: " + distribution);
////            out.println();
//            
//            if (icall >= maxNumOfTrials) {
//                outputType = 1;
//                break;
//            }
//
//            int lastIndex = bestCriterion.length - 1; // currently this value is 9
//
//            bestCriterion[lastIndex] = data.getObjFuncValueOfBestPoint();
//            if (NLOOP > numOfShufflingLoops) // ### Is this right?
//            {
//                // This condition is needed. If NLOOP < vectorOfBestCriterion.length,
//                // then there are some elements that are not set yet in vectorOfBestCriterion.
//                if (NLOOP >= bestCriterion.length) {
//                    int idx = lastIndex - numOfShufflingLoops;
//                    if (idx < 0) //#### For now, tempIndex is set to 0 if tempIndex < 0. This line should be changed.
//                    {
//                        idx = 0;
//                    }
//                    double denomi = Math.abs(bestCriterion[idx] + bestCriterion[lastIndex]) / 2;
//                    double timeou = Math.abs(bestCriterion[idx] - bestCriterion[lastIndex]) / denomi;
////                    out.println("\n$$$$$$$$$$$$ NLOOP = " + NLOOP + "\tnumOfShufflingLoops = " + numOfShufflingLoops +
////                            "\tvectorOfBestCriterion length = " + bestCriterion.length + "\tPercentage is calculated: " + timeou);
//                    if (timeou < percentage) {
//                        outputType = 2;
//                        break;
//                    }
//                }
//            }
//            for (int l = 0; l < lastIndex; l++) {
//                bestCriterion[l] = bestCriterion[l + 1];  // ######## Does this work????
//            }
//            if (paramConvergenceSATISFIED) {
//                outputType = 3;
//                break;
//            }
//            if (currentNumOfComplexes > minNumOfComplexes) {
//                lastNumOfComplexes = currentNumOfComplexes;
//                currentNumOfComplexes -= 1;
//                totalNumOfPoints = currentNumOfComplexes * numOfPointsInComplex;
//                comp();
//            }
//        }
//        return outputType;
//    }
//
//    //#########################################################################
//    //##  Other functions
//    //########################################################################
//    double execute(double[] array) throws Exception {
//        data.setParamValues(array);
//        executionHandle.execute(data);
//        icall++;
//        double of = stepData.calculateObjectiveFunctionValue(executionHandle);
//         double distribution = normdistForBestPoint();
////          out.println("loop " + NLOOP + "  ICALL = " + icall);
////            out.println("Number of complexes in a current population: " + currentNumOfComplexes);
////            out.println("Objective Function value of best point: " + data.getObjFuncValueOfBestPoint());
////            out.println("Objective Function value of worst point: " + objFuncValueOfWorstPoint);
////            out.println("Normal Distribution of best point: " + distribution);
//
//        out.print("\n    " + icall + ": " + of + " [" + data.getObjFuncValueOfBestPoint() + "/" + objFuncValueOfWorstPoint+"]" + " c:" + currentNumOfComplexes + " d:" + distribution);
//        return of;
//    }
//
//    void sort_duan(double[][] x, double[] y) {
//        Vector<Integer> indices = new Vector<Integer>();
//        for (int i = 0; i < x.length; i++) {
//            indices.add(new Integer(i));
//        }
//        if (!stepData.maximizeObjectiveFunctionValue()) {
//            Sorter sorter = new Sorter(y, Sorter.ASCENDING);
//            Collections.sort(indices, sorter);
//            Arrays.sort(y);
//        } else {
//            Sorter sorter = new Sorter(y, Sorter.DESCENDING);
//            Collections.sort(indices, sorter);
//            // sort y[] in ascending order
//            Arrays.sort(y);
//            // reverse the order of y to be in descending order
//            double[] new_y = new double[y.length];
//            for (int i = 0; i < y.length; i++) {
//                new_y[i] = y[y.length - 1 - i];
//            }
//            for (int i = 0; i < y.length; i++) {
//                y[i] = new_y[i];
//            }
//        }
//        double[][] new_x = new double[x.length][x[0].length];
//        for (int i = 0; i < new_x.length; i++) {
//            new_x[i] = x[indices.get(i).intValue()];
//        }
//        for (int i = 0; i < x.length; i++) {
//            x[i] = new_x[i];
//        }
//    }
//
//    void parstt() {
//        double[] xMax = new double[numOfParams];
//        double[] xMin = new double[numOfParams];
//        double[] xMean = new double[numOfParams];
//        double delta = Math.pow(10, -20);
////        double delta = 10E-20;
//        double peps = Math.pow(10, -3); // minimum standard deviation
////        double peps = 10E-3;              // minimum standard deviation
//        double gSum = 0;
//        for (int k = 0; k < numOfParams; k++) {
//            xMax[k] = Double.MIN_VALUE;
//            xMin[k] = Double.MAX_VALUE;
//            double xSum1 = 0;
//            double xSum2 = 0;
//            for (int i = 0; i < totalNumOfPoints; i++) {
//                xMax[k] = Math.max(pointsX[i][k], xMax[k]);
//                xMin[k] = Math.min(pointsX[i][k], xMin[k]);
//                xSum1 = xSum1 + pointsX[i][k];
//                xSum2 = xSum2 + pointsX[i][k] * pointsX[i][k];
//            }
//            xMean[k] = xSum1 / (double) totalNumOfPoints;
//            stdDevOfPopulation[k] = xSum2 / (double) totalNumOfPoints - xMean[k] * xMean[k];
//            if (stdDevOfPopulation[k] <= delta) {
//                stdDevOfPopulation[k] = delta;
//            }
//            stdDevOfPopulation[k] = Math.sqrt(stdDevOfPopulation[k]) / bound[k];
//            gSum += Math.log(delta + (xMax[k] - xMin[k]) / bound[k]);
//        }
//        normalizedGeometricMean = Math.pow(Math.E, gSum / (double) numOfParams);
//        paramConvergenceSATISFIED = false;
//        if (normalizedGeometricMean <= peps) {
//            paramConvergenceSATISFIED = true;
//        }
//    }
//
//    /*  This method returns the normal distance of the best point. This method
//     *  is used instead of normdist() because normdist() determins the normal
//     *  distances for all points, which is not needed. */
//    double normdistForBestPoint() {
//        double normalDistance = 0;
//        for (int i = 0; i < numOfParams; i++) {
//            normalDistance += Math.abs(pointsX[0][i] - initialPoint[i]) / bound[i];
//        }
//        normalDistance = normalDistance / numOfParams;
//        return normalDistance;
//    }
//
//    void comp() {
//        double[][] tempPoints = new double[totalNumOfPoints][numOfParams];
//        double[] tempObjFuncValues = new double[totalNumOfPoints];
//
//        // copy selected elements in pointsX and objFuncValueOfX to tempPoints and tempObjFuncValues
//        for (int igs = 0; igs < currentNumOfComplexes; igs++) {
//            for (int ipg = 0; ipg < numOfPointsInComplex; ipg++) {
//                int k1 = ipg * lastNumOfComplexes + igs;
//                int k2 = ipg * currentNumOfComplexes + igs;
//                for (int i = 0; i < numOfParams; i++) {
//                    tempPoints[k2][i] = pointsX[k1][i];
//                }
//                tempObjFuncValues[k2] = objFuncValueOfX[k1];
//            }
//        }
//        // reinitialize pointsX and objFuncValueOfX with a correct size
//        pointsX = new double[totalNumOfPoints][numOfParams];
//        objFuncValueOfX = new double[totalNumOfPoints];
//        // copy elements of tempPointx and tempObjFuncValues back to pointsX and objFuncValueOfX
//        for (int j = 0; j < totalNumOfPoints; j++) {
//            for (int i = 0; i < numOfParams; i++) {
//                pointsX[j][i] = tempPoints[j][i];
//            }
//            objFuncValueOfX[j] = tempObjFuncValues[j];
//        }
//    }
//
//    void cce() throws Exception {
//        double[] worstPointSimplex = new double[numOfParams]; // WO(.)
//        double[] centroid = new double[numOfParams]; //CE(.)
//        double[] newPoint = new double[numOfParams]; //SNEW(.)
//        double[] vector = new double[numOfParams]; //STEP(.)
//        double worstObjFuncValue; //FW
//
//        for (int j = 0; j < numOfParams; j++) {
//            // pointsInSimplex[] is sorted based on the objective functions values,
//            // so the element in the last index is the worst point.
//            worstPointSimplex[j] = pointsInSimplex[numOfPointsInSubComplex - 1][j];
//            centroid[j] = 0;
//            // exclude the last point (worst point) in this loop
//            for (int i = 0; i < (numOfPointsInSubComplex - 1); i++) {
//                centroid[j] += pointsInSimplex[i][j];
//            }
//            centroid[j] = centroid[j] / ((double) (numOfPointsInSubComplex - 1));
//            vector[j] = centroid[j] - worstPointSimplex[j];
//        }
//        worstObjFuncValue = objFuncValuesOfSimplex[numOfPointsInSubComplex - 1];
//        for (int j = 0; j < numOfParams; j++) {
//            newPoint[j] = worstPointSimplex[j] + 2 * vector[j];
//        }
//        boolean outOfBOUND = false;
//        for (int j = 0; j < numOfParams; j++) {
//            if ((newPoint[j] > upperBound[j]) || (newPoint[j] < lowerBound[j])) {
//                outOfBOUND = true;
//                break;
//            }
//        }
//        if (outOfBOUND) {
//            getNewPointAtRandom(newPoint);
//        }
//        double newObjFuncValue = execute(newPoint);
//
//        if ((stepData.maximizeObjectiveFunctionValue() && newObjFuncValue <= worstObjFuncValue) ||
//                (!stepData.maximizeObjectiveFunctionValue() && newObjFuncValue >= worstObjFuncValue)) {
//            if (icall >= maxNumOfTrials) {
//                return; //ICALL;
//            }
//            for (int j = 0; j < numOfParams; j++) {
//                newPoint[j] = worstPointSimplex[j] + 0.5 * vector[j];
//            }
//            newObjFuncValue = execute(newPoint);
//            if ((stepData.maximizeObjectiveFunctionValue() && newObjFuncValue < worstObjFuncValue) ||
//                    (!stepData.maximizeObjectiveFunctionValue() && newObjFuncValue > worstObjFuncValue)) {
//                if (icall >= maxNumOfTrials) {
//                    return;
//                }
//                getNewPointAtRandom(newPoint);
//                newObjFuncValue = execute(newPoint);
//
//            }// end of the 2nd if ((newObjFuncValue > worstObjFuncValue) ... )
//        } // end of the 1st if ((newObjFuncValue > worstObjFuncValue) ... )
//
//        for (int j = 0; j < numOfParams; j++) {
//            pointsInSimplex[numOfPointsInSubComplex - 1][j] = newPoint[j];
//        }
//        objFuncValuesOfSimplex[numOfPointsInSubComplex - 1] = newObjFuncValue;
//    }
//
//    /* a new point is assigned to newPoint based on stdDevOfPopulation[],
//     *  gasdev(), bound[], and etc.*/
//    void getNewPointAtRandom(double[] newPoint) {
//        for (int j = 0; j < numOfParams; j++) {
//            int nnn = 0;
//            do {
//                double R = gasdev();
//                newPoint[j] = pointsInSimplex[0][j] + stdDevOfPopulation[j] * R * bound[j];
//                nnn++;
//                if (nnn == 1001) {
//                    out.println("SCE: getNewPointAtRandom(): Having hard time generating a new point in a feasible region");
//                }
//                if (nnn > 1000) {
//                    newPoint[j] = lowerBound[j] + Math.abs(R) * (0.5 * bound[j]);
//                    if (nnn % 100 == 1) {
//                        out.print("Attempt " + nnn + ": new point = " + newPoint[j] +
//                                ", lower bound = " + lowerBound[j] + ", upper bound = " + upperBound[j]);
//                    }
//                    if ((newPoint[j] > upperBound[j]) || (newPoint[j] < lowerBound[j])) {
//                        out.println(" ---> out of bound");
//                    } else {
//                        out.println(" ---> in bound!!");
//                    }
//                }
//            } while ((newPoint[j] > upperBound[j]) || (newPoint[j] < lowerBound[j]));
//        }
//    }
//    //
//    boolean calculateGASDEV = true; // if true, gasdev() returns gasdevValue1
//    double gasdevValue1; // one of the two values generated in gasdev()
//    double gasdevValue2; // one of the two values generated in gasdev()
//
//    /* returns a normally distributed deviate with zero mean and unit variance,
//     *  using random number generator, as the source of uniform deviates.
//     */
//    double gasdev() {
//        double R, v1, v2;
//        if (calculateGASDEV) {
//            // if we don't have an extra deviate handy
//            do {
//                // pick two uniform numbers in the square extending from -1 to +1
//                // in each direction
//                v1 = 2 * Math.random() - 1;
//                v2 = 2 * Math.random() - 1;
//                // check if v1 and v2 are in the unit circle
//                R = v1 * v1 + v2 * v2;
//            } while (R >= 1); // if v1 and v2 are not in the unit circle
//
//            // make the Box-Muller transformation to get two normal deviates
//            double fac = Math.sqrt((-1) * ((2 * Math.log(R)) / R));
//            gasdevValue2 = v1 * fac; // one of the two normal deviates. gasdevValue2 is returned
//            // next time this function is called
//            gasdevValue1 = v2 * fac; // the other normal deviate, which will be returned at this time
//            calculateGASDEV = false;
//            return gasdevValue1;
//        } else {
//            calculateGASDEV = true;
//            return gasdevValue2;
//        }
//    }
//
//    /* returns an array of the specified size, containing the elements of index
//     *  from 0 to size - 1 in source. */
//    double[] copy(double[] source, int size) {
//        double[] newArray = new double[size];
//        for (int i = 0; i < size; i++) {
//            newArray[i] = source[i];
//        }
//        return newArray;
//    }
//
//    /* returns an 2D array[rowSize][length of columns of source], containing
//     *  the elements from source[0][] to source[size-1][]. */
//    double[][] copy(double[][] source, int rowSize) {
//        double[][] newArray = new double[rowSize][source[0].length];
//        for (int i = 0; i < rowSize; i++) {
//            for (int j = 0; j < newArray[0].length; j++) {
//                newArray[i][j] = source[i][j];
//            }
//        }
//        return newArray;
//    }
//}
