package org.jgrasstools.hortonmachine.modules.networktools.trento_p;

import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_CELERITY_FACTOR;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_EPSILON;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_ESP1;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_EXPONENT;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_FRANCO;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_GAMMA;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_J_MAX;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_MAX_JUNCTION;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_MAX_THETA;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_MING;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_MINIMUM_DEPTH;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_MIN_DISCHARGE;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_TOLERANCE;

import java.util.Properties;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Range;
import oms3.annotations.Status;
import oms3.annotations.Unit;

@Description("The optional parameters to run the TrentoP model")
@Author(name = "Daniele Andreis")
@Keywords("TrentoP")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class TrentoPOptionalParameter {

    @Description("Minimum excavation depth")
    @Unit("m")
    @In
    public double pMinimumDepth = DEFAULT_MINIMUM_DEPTH;

    @Description("Max number of pipes that can converge in a junction.")
    @Unit("-")
    @Range(max = 6, min = 0)
    @In
    public int pMaxJunction = DEFAULT_MAX_JUNCTION;

    @Description("Max number of bisection to do (default is 40)to search a solution of a transcendently equation.")
    @Unit("-")
    @Range(max = 1000, min = 3)
    @In
    public int pJMax = DEFAULT_J_MAX;

    @Description("Accuracy to use to calculate the discharge.")
    @Unit("-")
    @Range(max = 1, min = 0)
    @In
    public double pEpsilon = DEFAULT_EPSILON;

    @Description("Minimum Fill degree")
    @Unit("-")
    @Range(max = 0.1, min = 0)
    @In
    public double pMinG = DEFAULT_MING;

    @Description("Minimum discharge in a pipe")
    @Unit("m3/s")
    @Range(min = 0)
    @In
    public double pMinDischarge = DEFAULT_MIN_DISCHARGE;

    @Description("Maximum Fill degree")
    @Unit("-")
    @Range(min = 3.14)
    @In
    public double pMaxTheta = DEFAULT_MAX_THETA;

    @Description("Celerity factor, value used to obtain the celerity of the discharge wave.")
    @Unit("-")
    @Range(min = 1, max = 1.6)
    @In
    public double pCelerityFactor = DEFAULT_CELERITY_FACTOR;

    @Description("Exponent of the basin extension. Used to calculate the average acces time to the network.")
    @Unit("-")
    @Range(min = 0)
    @In
    public double pExponent = DEFAULT_EXPONENT;

    @Description("tollerance, used to obtain the radius")
    @Unit("-")
    @Range(min = 0)
    @In
    public double pTolerance = DEFAULT_TOLERANCE;

    @Description("Division base to height in the rectangular or trapezium section.")
    @Unit("-")
    @Range(min = 0)
    @In
    public double pC = 1;

    @Description("Exponent of the average ponderal slope of a basin to calculate the average access time to the network  for area units.")
    @Unit("-")
    @Range(min = 0)
    @In
    public double pGamma = DEFAULT_GAMMA;

    @Description("Exponent of the influx coefficent to calculate the average residence time in the network .")
    @Unit("-")
    @Range(min = 0)
    @In
    public double pEspInflux = DEFAULT_ESP1;

    @Description("Minimum dig depth, for rectangular or trapezium pipe.")
    @Unit("m")
    @Range(min = 0)
    @In
    public double pFranco = DEFAULT_FRANCO;

    @Description(" Use mode, 0=project, 1=verify.")
    @In
    public short pTest;
    
    @Description("The Properties needed for trentop.")
    @Out
    public Properties outProperties = new Properties();

    @Execute
    public void process() throws Exception {
    }

}
