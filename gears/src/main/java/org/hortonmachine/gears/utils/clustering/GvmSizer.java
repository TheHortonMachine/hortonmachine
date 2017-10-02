package org.hortonmachine.gears.utils.clustering;
//package org.hortonmachine.lidartools.clustering;
//
//import com.tomgibara.cluster.ClusterPainter;
//
//public class GvmSizer implements ClusterPainter.Sizer<GvmResult<?>> {
//
//	@Override
//	public double distance(GvmResult<?> r1, GvmResult<?> r2) {
//		return r1.getSpace().distance(r1.getPoint(), r2.getPoint());
//	}
//
//	@Override
//	public double radius(GvmResult<?> r) {
//		return Math.sqrt(r.getVariance()) * 2.0; // 2 std deviations
//	}
//
//	@Override
//	public long points(GvmResult<?> r) {
//		return r.getCount();
//	}
//
//}
