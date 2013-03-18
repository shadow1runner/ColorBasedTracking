package at.ac.uibk.cs.auis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class ColorBasedTracker {

	private static final double MINIMAL_VALUE_OF_CONTOUR_AREA = 0.1;
	
	private Mat blackWhiteMask;
	private Mat dilatedMask;

	private Scalar colorForTrackingHSV;
	private Scalar colorRadius = new Scalar(25, 25, 25, 0);
	private Scalar lowerBound;
	private Scalar upperBound;

	private List<MatOfPoint> contour;
	private Rect boundingRect;
	private static double MINIMAL_VALUE_OF_CONTOUR_ARE = 0.1;
	
	/**
	 * initializes the bounds used for range checking in HSV-color-space
	 */
	private void initializeBounds() {
		lowerBound = new Scalar(0);
		upperBound = new Scalar(0);
		lowerBound.val[0] = colorForTrackingHSV.val[0] - colorRadius.val[0];
		upperBound.val[0] = colorForTrackingHSV.val[0] + colorRadius.val[0];
		lowerBound.val[1] = colorForTrackingHSV.val[1] - colorRadius.val[1];
		upperBound.val[1] = colorForTrackingHSV.val[1] + colorRadius.val[1];
		lowerBound.val[2] = colorForTrackingHSV.val[2] - colorRadius.val[2];
		upperBound.val[2] = colorForTrackingHSV.val[2] + colorRadius.val[2];
		lowerBound.val[3] = 0;
		upperBound.val[3] = 255;
	}

	
	/**
	 * calculates the center of mass using <code>colorForTrackingHSV</code> and <code>colorRadius</code> as radius (in HSV-color space)
	 * @param hsv
	 *  the frame of which the center of mass should be calculated off
	 * @return
	 *  the center of mass as a point in pixel coordinates (i.e. integer)
	 */
	public Point calcCenterOfMass(Mat hsv) {
		blackWhiteMask = new Mat();
		Core.inRange(hsv, lowerBound, upperBound, blackWhiteMask);

		dilatedMask = new Mat();
		Imgproc.dilate(blackWhiteMask, dilatedMask, new Mat());

		contour = new ArrayList<MatOfPoint>();
		Mat mHierarchy = new Mat();
		Mat tempDilatedMask = dilatedMask.clone();
		Imgproc.findContours(tempDilatedMask, contour, mHierarchy,
				Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

		boundingRect = Imgproc.boundingRect(findLargenstContour(contour));

		int centerOfMassX = boundingRect.x + boundingRect.width / 2;
		int centerOfMassY = boundingRect.y + boundingRect.height / 2;

		return new Point(centerOfMassX, centerOfMassY);
	}

	private MatOfPoint findLargenstContour(List<MatOfPoint> contours) {
		// Find max contour area
		MatOfPoint maxAreaMatrix = new MatOfPoint();

		double maxArea = 0;

		Iterator<MatOfPoint> each = contour.iterator();
		while (each.hasNext()) {
			MatOfPoint wrapper = each.next();
			double area = Imgproc.contourArea(wrapper);
			if (area > maxArea) {
				maxArea = area;
				maxAreaMatrix = wrapper;
			}
		}

		if (maxArea < MINIMAL_VALUE_OF_CONTOUR_AREA)
			throw new IllegalArgumentException();

		return maxAreaMatrix;
	}

	public Scalar getColorForTrackingHSV() {
		return colorForTrackingHSV;
	}

	public void setColorForTrackingHSV(Scalar colorForTrackingHSV) {
		this.colorForTrackingHSV = colorForTrackingHSV;
		initializeBounds();
	}

	public Scalar getColorRadius() {
		return colorRadius;
	}

	public void setColorRadius(Scalar colorRadius) {
		this.colorRadius = colorRadius;
	}

	public Mat getBlackWhiteMask() {
		return blackWhiteMask;
	}

	public Mat getDilatedMask() {
		return dilatedMask;
	}

	/**
	 * @return the surrounding of the rectangle
	 */
	public Rect getBoundingRect() {
		return boundingRect;
	}
	
	

}
