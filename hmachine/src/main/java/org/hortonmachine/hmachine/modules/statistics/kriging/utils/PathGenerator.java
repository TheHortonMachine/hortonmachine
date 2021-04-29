package org.hortonmachine.hmachine.modules.statistics.kriging.utils;

import java.io.IOException;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;

public class PathGenerator {

    @Description("String path to the input data")
    @In
    public String pathToOutData;

    @Description("Start date of the input data")
    @In
    public String tCurrent;

    @Description("String path to the output data")
    @Out
    public String pathOutDataComplete;

    @Execute
    public void process() throws IOException {

        String extension = pathToOutData.substring(pathToOutData.length() - 4, pathToOutData.length());

        String path = pathToOutData.substring(0, pathToOutData.length() - 4);

        String year = tCurrent.substring(0, 4);
        String month = tCurrent.substring(5, 7);
        String day = tCurrent.substring(8, 10);

        String hour = tCurrent.substring(11, 13);
        String minutes = tCurrent.substring(14, 16);

        pathOutDataComplete = path + "_" + year + month + day + "_" + hour + minutes + extension;

        // System.out.println(pathOutDataComplete);

    }

}
