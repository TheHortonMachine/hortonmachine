package org.jgrasstools.hortonmachine.modules.networkmanagement;

import static java.lang.Math.pow;
import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Range;
import oms3.annotations.Status;
import oms3.annotations.Unit;

import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;

@Description("The Trento P model for networks management.")
@Author(name = "Andrea Antonello, Franceschi Silvia, Riccardo Rigon, David Tamanini", contact = "www.hydrologis.com")
@Keywords("Network")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class TrentoP {
    @Description("Coefficient a of the designed rainfall.")
    @Unit("mm/hour^n")
    @In
    public double pA = -1;

    @Description("Coefficient n of the designed rainfall.")
    @In
    public double pN = -1;

    @Description("The minimum shear stress.")
    @Unit("Pa")
    @In
    public double pTau = -1;

    @Description("The maximum filling ratio.")
    @In
    public double pG = -1;

    @Description("The network vertical alignment option (0,1).")
    @In
    public int pALign = 0;

    @Description("")
    @Unit("m")
    @In
    public double pScavo = 1.20; // [m]

    @Description("Number of pipes allowed to join.")
    @In
    public double pMaxjunctions = 4;

    @Description("Number of bisection tried to obtain the root by the bisection method.")
    @In
    public double pJmax = 40;

    @Description("Accuracy used by the bisection method.")
    @In
    public double pAccuracy = 0.005;

    @Description("Time step at which the maximum discharge is evaluated.")
    @Unit("min")
    @In
    public double pDtp = 0.15;

    @Description("Minimum time of rain allowed in maximum discharge search.")
    @Unit("min")
    @In
    public double pTpmin = 1;

    @Description("Maximum time of rain allowed in maximum discharge search.")
    @Unit("min")
    @In
    public double pTpmax = 30;

    @Description("Accuracy in maximum discharge search")
    @In
    public double pEpsilon = 0.001;

    @Description("Minimum depth fraction allowed.")
    @In
    public double pMing = 0.01;

    @Description("Minimum discharge allowed in a pipe.")
    @Unit("l/s")
    @In
    public double pMinq = 1.0;

    @Description("For circular pipe cross section the maximum angle theta allowed when pG=0.8.")
    @Unit("rad")
    @In
    public double pMaxtheta = 4.43;

    @Description("Factor that multiplies velocity in pipes to obtain celerity.")
    @Range(max = 1.5, min = 1.0)
    @In
    public double pCelerityfactor = 1.5;

    @Description("Exponent used to calculate mean residence times outside pipes.")
    @Range(max = 0.5, min = 0.3)
    @In
    public double pExponent = 0.38;

    @Description("Accuracy in r value search.")
    @In
    public double tolerance = 0.001;

    @Description("Base-height ratio for the rectangular and trapezoidal pipe sections.")
    @In
    public double pQ = 0.5;  // c

    @Description("Exponent used to calculate mean residence times outside pipes.")
    @Range(max = 0.5, min = 0.3)
    @In
    public double pGamma = 0.35;

    @Description("Exponent used to calculate mean residence times outside pipes.")
    @Range(max = 0.5, min = 0.3)
    @In
    public double pB = 0.4;  // esp_1

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    public void process() {
        pA = pA / pow(60.0, pN); /* [mm/hour^n] -> [mm/min^n] */
    }
}
