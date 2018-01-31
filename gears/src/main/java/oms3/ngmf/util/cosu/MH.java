/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.ngmf.util.cosu;

// MCMCSCMH.java: Markov chain Monte Carlo
// Generic single-component Metropolis-Hastings algorithm
// with adapter for simulating discrete Markov chains.
// sestoft@dina.kvl.dk 1998-02-24, 1998-03-17, 1999-08-06
import java.awt.*;
import java.applet.Applet;

abstract class MHsetup {

    public abstract int getDimension();

    public abstract SCMH newMH();

    public void compute(double[][] obs, int dim, int n, TextAreaOutput out) {
    }
}

abstract class MCMCApplet extends Applet {

    Button stopsimulation, startsimulation;
    TextAreaOutput taoutput;
    Controls controls;

    public void addcomponents(Panel allPanel) {
    }

    public void init() {
        Panel allPanel = new Panel();
        allPanel.setLayout(new BorderLayout(20, 20));
        addcomponents(allPanel);

        Panel botPanel = new Panel();
        stopsimulation = new Button("Stop simulation");
        startsimulation = new Button("Start simulation");
        botPanel.add(stopsimulation);
        botPanel.add(startsimulation);
        allPanel.add("South", botPanel);
        add(allPanel);
        TextArea output = new TextArea(16, 40);
        taoutput = new TextAreaOutput(output);
        add(output);
    }

    public abstract MHsetup makeSetup();

    public boolean action(Event e, Object o) {
        if (e.target == stopsimulation) {
            if (controls != null) {
                controls.stopsimulation();
            }
        } else if (e.target == startsimulation) {
            if (controls != null) {
                controls.stopsimulation();
            }
            controls = new Controls(makeSetup(), taoutput);
            controls.show();
        }
        return true;
    }

    public void stop() {
        if (controls != null) {
            controls.stopsimulation();
        }
    }
}

class TextAreaOutput {

    private TextArea ta;

    public TextAreaOutput(TextArea ta) {
        this.ta = ta;
        ta.setText("");
    }

    public void clear() {
        if (ta != null) {
            ta.setText("");
        }
    }

    public void print(int i) {
        ta.appendText(Integer.toString(i));
    }

    public void print(double d) {
        ta.appendText(Double.toString(d));
    }

    public void print(String s) {
        ta.appendText(s);
    }

    public void println(int i) {
        ta.appendText(Integer.toString(i));
        ta.appendText("\n");
    }

    public void println(double d) {
        ta.appendText(Double.toString(d));
        ta.appendText("\n");
    }

    public void println(String s) {
        ta.appendText(s);
        ta.appendText("\n");
    }
}

class Controls extends Frame {

    private MHsetup setup;
    private int dimension;
    private Graph[] graphs;
    private TextAreaOutput taoutput;
    private SCMH mh;		// The active simulation, if any
    private TextField iterIn = new TextField(15);
    private TextField ageOut = new TextField(20);
    private Button burnin = new Button("Burn-in (w/o graph)");
    private Button further = new Button("Simulation (with graph)");
    private Checkbox codaout = new Checkbox("CODA output");

    public Controls(MHsetup setup, TextAreaOutput taoutput) {
        super("Simulation controls");
        resize(300, 150);

        this.setup = setup;
        this.taoutput = taoutput;
        dimension = setup.getDimension();
        graphs = new Graph[dimension];
        for (int d = 1; d <= dimension; d++) {
            graphs[d - 1] = new Graph("x" + d, 900, 200);
        }
        for (int d = 0; d < dimension; d++) {
            graphs[d].show();
        }

        setLayout(new BorderLayout());
        Panel iters = new Panel();
        iters.setLayout(new GridLayout(2, 2));
        iters.add(new Label("Number of steps per click:"));
        iters.add(iterIn);
        iterIn.setText("1000");
        iters.add(new Label("Total number of steps:"));
        iters.add(ageOut);
        ageOut.setEditable(false);
        Panel buttons = new Panel();
        buttons.setLayout(new GridLayout(1, 3));
        buttons.add(codaout);
        buttons.add(burnin);
        buttons.add(further);
        add("North", iters);
        add("South", buttons);
        pack();
    }

    private void simulateAndDraw() {
        int iterations = Integer.parseInt(iterIn.getText());
        double[][] obs = new double[dimension][iterations];
        for (int i = 0; i < iterations; i++) {
            double[] obsi = mh.next();
            for (int d = 0; d < dimension; d++) {
                obs[d][i] = obsi[d];
            }
        }
        ageOut.setText("" + mh.getAge());
        for (int d = 0; d < dimension; d++) {
            graphs[d].draw(obs[d], iterations, mh.getAge() - iterations);
        }
        if (iterations > 0 && dimension > 0) {
            if (codaout.getState()) {
                taoutput.clear();
            }
            taoutput.println("Iteration " + (mh.getAge() - iterations + 1) + " through " + mh.getAge() + ":");
            if (codaout.getState()) {
                writeCODA(obs, dimension, iterations, taoutput);
            }
            setup.compute(obs, dimension, iterations, taoutput);
            taoutput.println("");
        }
    }

    public boolean action(Event e, Object o) {
        int oldcursor = getCursorType();
        setCursor(Frame.WAIT_CURSOR);
        if (e.target == burnin) {
            if (mh == null) {
                mh = setup.newMH();
            }
            int iterations = Integer.parseInt(iterIn.getText());
            for (int d = 0; d < dimension; d++) {
                graphs[d].draw(null, 0, 0);
            }
            for (int i = 0; i < iterations; i++) {
                mh.next();
            }
            ageOut.setText("" + mh.getAge());
        } else if (e.target == further) {
            if (mh == null) {
                mh = setup.newMH();
            }
            simulateAndDraw();
        }
        setCursor(oldcursor);
        return true;
    }

    void writeCODA(double[][] obs, int dim, int n, TextAreaOutput out) {
        // Write observations to file.out and parameter names to file.ind
        // as required by the CODA program (Best, Cowles, Vines 1996, page 39)
        StringBuffer buf = new StringBuffer(8 * n + 100);
        buf.append("\nData for file.ind:\n\n");
        for (int d = 0; d < dim; d++) {
            buf.append("x" + (d + 1) + "   " + (d * n + 1) + "  " + ((d + 1) * n) + "\n");
        }
        buf.append("\nData for file.out:\n\n");
        for (int d = 0; d < dim; d++) {
            for (int i = 0; i < n; i++) {
                buf.append((i + 1) + "  " + obs[d][i] + "\n");
            }
        }
        buf.append("\n");
        out.print(buf.toString());
    }

    public void stopsimulation() {
        mh = null;
        taoutput.clear();
        for (int d = 0; d < dimension; d++) {
            graphs[d].hide();
            graphs[d].dispose();
        }
        hide();
        dispose();
    }

    public boolean handleEvent(Event e) {
        if (e.id == Event.WINDOW_DESTROY) {
            stopsimulation();
        }
        return super.handleEvent(e);
    }
}

class ErrorMessage extends Dialog {

    private Button ok;

    ErrorMessage(String msg) {
        super(null, true);
        add("Center", new Label("Error: " + msg));
        ok = new Button("OK");
        add("South", ok);
        pack();
        show();
    }

    public boolean action(Event e, Object o) {
        if (e.target == ok) {
            hide();
            dispose();
        }
        return true;
    }
}

class SCMH {
    // Single-component Metropolis-Hastings algorithm on probability
    // space R^n = double[], where n is fixed

    private SCMHparam param;	// The parameters of the MC
    private double[] lastx;	// The state of the MC
    private int age;		// Number of steps simulated so far

    public SCMH(SCMHparam param, double[] x0) {
        this.param = param;
        this.lastx = x0;
    }

    public double[] next() {
        for (int i = 0; i < lastx.length; i++) {
            double yi = param.qobs(i, lastx);
            if (Math.random() <= param.alpha(lastx, i, yi)) {
                lastx[i] = yi;
            }
        }
        age++;
        return param.ext(lastx);
    }

    public int getAge() {
        return age;
    }
}

abstract class SCMHparam {

    // Draw an observation from the i'th dimension of the proposal
    // distribution, given xt:
    abstract double qobs(int i, double[] xt);

    // Probability of accepting new point yi in i'th dimension, given
    // that xt is the state after step i-1 of the current iteration.
    abstract double alpha(double[] xt, int i, double yi);

    // Observations may be transformed before being plotted etc, using
    // the function ext.  By default, this is the identity.
    double[] ext(double[] obs) {
        return obs;
    }
}

// Special case: one-dimensional Markov chain on discrete space (int),
// specified by a function pi giving the transition probabilities.
abstract class MHdparam extends SCMHparam {

    // Probability of going from (internal) state x to (internal) state y
    abstract double pi(int x, int y);

    // Draw an (internal) new state y given that we're in (internal) state xt
    double qobs(int i, double[] xt) {
        int x = (int) (xt[0]);
        double p = Math.random();
        int y = 0;
        p -= pi(x, y);
        while (p > 0) {
            y++;
            p -= pi(x, y);
        }
        return (double) y;
    }

    // In this case the probability of accepting the transition is 1
    double alpha(double[] xt, int i, double yi) {
        return 1.0;
    }
}

class InputTransitions extends Panel {
    // Data entry for an n by n transition matrix

    private int dimension;
    private TextField[][] pIn;

    public InputTransitions(int dimension) {
        this.dimension = dimension;
        Panel matrixPanel = new Panel();
        pIn = new TextField[dimension][dimension];
        matrixPanel.setLayout(new GridLayout(dimension, dimension));
        for (int d1 = 0; d1 < dimension; d1++) {
            for (int d2 = 0; d2 < dimension; d2++) {
                pIn[d1][d2] = new TextField(10);
                matrixPanel.add(pIn[d1][d2]);
            }
        }
        setLayout(new BorderLayout(20, 20));
        add("North", new Label("Transition matrix"));
        add("Center", matrixPanel);
    }

    public InputTransitions(int dimension, double[][] P0) {
        this(dimension);
        for (int d1 = 0; d1 < dimension; d1++) {
            for (int d2 = 0; d2 < dimension; d2++) {
                pIn[d1][d2].setText(P0[d1][d2] + "");
            }
        }
    }

    public double[][] getP() {
        double[][] P = new double[dimension][dimension];
        for (int d1 = 0; d1 < dimension; d1++) {
            for (int d2 = 0; d2 < dimension; d2++) {
                P[d1][d2] = new Double(pIn[d1][d2].getText()).doubleValue();
            }
        }
        // Check that the transition matrix is legal; return null if not
        double[] rowsum = new double[dimension];
        double[] colsum = new double[dimension];
        for (int d1 = 0; d1 < dimension; d1++) {
            for (int d2 = 0; d2 < dimension; d2++) {
                rowsum[d1] += P[d1][d2];
                colsum[d2] += P[d1][d2];
            }
        }
        for (int d = 0; d < dimension; d++) {
            if (Math.abs(rowsum[d] - 1) > 1E-10 || Math.abs(colsum[d] - 1) > 1E-10) {
                return null;
            }
        }
        return P;
    }

    public void setEditable(boolean editable) {
        for (int d1 = 0; d1 < dimension; d1++) {
            for (int d2 = 0; d2 < dimension; d2++) {
                pIn[d1][d2].setEditable(editable);
            }
        }
    }
}

// Another implementation of MHdparam: using a transition matrix
// accumP with accumulated probabilities
class Transition extends MHdparam {

    double[][] accumP;

    public Transition(double[][] accumP) {
        this.accumP = accumP;
    }

    double pi(int x, int y) // Not used
    {
        return 1 / (x - x);
    }

    // An efficient version of qobs, using binary search in accumP
    double qobs(int dummy, double[] xt) {
        int x = (int) (xt[0]);
        double p = Math.random();
        double[] distr = accumP[x];
        int i, left = 0, right = distr.length - 1;
        // Find and return least i s.t. p <= distr[i]
        while (left <= right) {
            // Here distr[left-1] <= p <= distr[right+1]
            i = (left + right) / 2;
            if (p < distr[i]) {
                right = i - 1;
            } else if (distr[i] < p) {
                left = i + 1;
            } else {
                return i;
            }
        }
        // Now p <= distr[left]
        return left;
    }
}

class Graph extends Frame {

    private GraphCanvas graph;

    public Graph(String title, int width, int height) {
        super(title);
        graph = new GraphCanvas(width, height);
        setLayout(new GridLayout(1, 1));
        add(graph);
        pack();
    }

    public void draw(double[] obs, int n, int age0) {
        graph.draw(obs, n, age0);
    }

    public boolean handleEvent(Event e) {
        if (e.id == Event.WINDOW_DESTROY) {
            hide();
            dispose();
        }
        return super.handleEvent(e);
    }
}

class GraphCanvas extends Canvas {

    private int n;		// the number of observations to draw
    private double[] obs;		// the observations
    private int age0;		// the age of obs[0] in the MC
    private int width,  height;
    private double ymin,  ymax,  yscale;
    private double xscale;

    public GraphCanvas(int width, int height) {
        resize(width, height);
    }

    public void draw(double[] obs, int n, int age0) {
        this.obs = obs;
        this.n = n;
        this.age0 = age0;
        if (n > 0) {
            ymin = ymax = obs[0];
            for (int i = 1; i < n; i++) {
                ymin = Math.min(ymin, obs[i]);
                ymax = Math.max(ymax, obs[i]);
            }
            if (ymax - ymin < 1E-10) {
                n = 0;		// Don't draw the graph
            }
        }
        repaint();
    }

    private int xcoord(int x) {
        return (int) (x * xscale);
    }

    private int ycoord(double y) {
        return (int) (height - 15 - (y - ymin) * yscale);
    }

    public void paint(Graphics g) {
        if (obs != null && n > 0) {
            width = size().width;
            height = size().height;
            yscale = (height - 25.0) / (ymax - ymin);
            xscale = (double) width / (n + 2);
            g.clearRect(0, 0, width, height);

            g.setColor(Color.black);
            for (int i = 1; i < n; i++) {
                g.drawLine(xcoord(i - 1), ycoord(obs[i - 1]),
                        xcoord(i), ycoord(obs[i]));
            }
            g.setColor(Color.green);
            // The line y = 0:
            g.drawLine(0, ycoord(0), width, ycoord(0));
            // x-axis ticks
            int agen = age0 + n, step = 1;
            while (step < n / 20) {
                step *= 10;
            }
            for (int x = (age0 / step + 1) * step; x < agen; x += step) {
                int yc = ycoord(0);
                if (yc < 0 || yc > height - 15) {
                    yc = height - 15;
                }
                int xc = xcoord(x - age0);
                g.drawLine(xc, yc - 10, xc, yc);
                g.drawString(x + "", xc - 10, yc + 10);
            }
            // y-axis ticks
            step = 1;
            while (step < (ymax - ymin) / 20) {
                step *= 10;
            }
            for (int y = (int) (ymin / step) * step; y < ymax; y += step) {
                int yc = ycoord(y);
                g.drawString(y + "", 5, yc + 5);
            }
        }
    }
}
