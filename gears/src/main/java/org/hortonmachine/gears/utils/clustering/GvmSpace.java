package org.hortonmachine.gears.utils.clustering;

public abstract class GvmSpace {

	//TODO use within cluster
	public static double nonNegative(double v) {
		return v < 0.0 ? 0.0 : v;
	}
	
	public abstract Object newOrigin();
	
	public abstract Object newCopy(Object pt);
	
	public abstract double magnitudeSqr(Object pt);

	public abstract double sum(Object pt);

	//not used directly in algorithm, but useful - override for good performance
	public double magnitude(Object pt) {
		return Math.sqrt(magnitudeSqr(pt));
	}
	
	//not used directly in algorithm, but useful - override for good performance
	public double distance(Object pt1, Object pt2) {
		Object p = newCopy(pt1);
		subtract(p, pt2);
		return magnitude(p);
	}
	
	//naive implementation that must be overridden for good performance
	// m - total mass (not zero)
	// pt - aggregate point (prescaled by mass) 
	// ptSqr - aggregate point squared (prescaled by mass)
	public double variance(double m, Object pt, Object ptSqr) {
		Object x = newCopy(ptSqr);
//		// Var is E(X^2) - E(X)^2, but squaring pt1 introduces an extra factor which is the total mass, so:
		subtractScaledSqr(x, 1 / m, pt);
		return sum(x);
	}

	//naive implementation that must be overridden for good performance
	// m1 - established total mass
	// pt1 - aggregate point (prescaled by mass) 
	// ptSqr1 - aggregate point squared (prescaled by mass)
	// m2 - mass of candidate point
	// pt2 - candidate point (not prescaled by mass)
	// Note: (m1 + m2) never zero
	public double variance(double m1, Object pt1, Object ptSqr1, double m2, Object pt2) {
		// compute the total mass
		double m0 = m1 + m2;
		// compute the new sum
		Object pt0 = newCopy(pt1);
		addScaled(pt0, m2, pt2);
		// compute the new sum of squares
		Object ptSqr0 = newCopy(ptSqr1);
		addScaledSqr(ptSqr0, m2, pt2);
		// compute the variance
		return variance(m0, pt0, ptSqr0);
	}

	//naive implementation that must be overridden for good performance
	// m1 - established total mass
	// pt1 - aggregate point (prescaled by mass) 
	// ptSqr1 - aggregate point squared (prescaled by mass)
	// m2 - mass of candidate cluster
	// pt2 - candidate cluster point (prescaled by mass)
	// ptSqr2 - candidate cluster point squared (prescaled by mass)
	// Note: (m1 + m2) never zero
	public double variance(double m1, Object pt1, Object ptSqr1, double m2, Object pt2, Object ptSqr2) {
		// compute the total mass
		double m0 = m1 + m2;
		// compute the new sum
		Object pt0 = newCopy(pt1);
		add(pt0, pt2);
		// compute the new sum of squares
		Object ptSqr0 = newCopy(ptSqr1);
		add(ptSqr0, ptSqr2);
		// compute the variance
		return variance(m0, pt0, ptSqr0);
	}

	public abstract void setToOrigin(Object pt);
	
	public abstract void setTo(Object dstPt, Object srcPt);
	
	public abstract void setToScaled(Object dstPt, double m, Object srcPt);
	
	public abstract void setToScaledSqr(Object dstPt, double m, Object srcPt);

	public abstract void add(Object dstPt, Object srcPt);
	
	public abstract void addScaled(Object dstPt, double m, Object srcPt);
	
	public abstract void addScaledSqr(Object dstPt, double m, Object srcPt);

	public abstract void subtract(Object dstPt, Object srcPt);
	
	public abstract void subtractScaled(Object dstPt, double m, Object srcPt);
	
	public abstract void subtractScaledSqr(Object dstPt, double m, Object srcPt);
	
	public abstract void scale(Object dstPt, double m);
	
	public abstract void square(Object pt);

	public String toString(Object pt) {
		return pt == null ? "null" : pt.toString();
	}
	
}
