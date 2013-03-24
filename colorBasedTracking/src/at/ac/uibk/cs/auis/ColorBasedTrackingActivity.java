package at.ac.uibk.cs.auis;

import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;

public class ColorBasedTrackingActivity extends Activity implements
		CvCameraViewListener2 {

	private MenuItem[] mItems;

	private static final String TAG = "OCVSample::Activity";

	/** size of the frame in the middle of the screen */
	private static final int PIXELS_OF_PREVIEW = 32;

	private static final int SIZE_OF_CENTER_OF_MASS = 6;

	/** thickness of rectangle in the middle of the screen */
	private static final int THICKNESS_OF_RECTANGLE = 5;

	private static final Scalar INDICATING_COLOR = new Scalar(0xbf, 0xfe, 0x00,
			0x00);

	private CameraBridgeViewBase mOpenCvCameraView;

	private boolean isTracking = false;
	private boolean trackColor = true;

	private ColorBasedTracker colorBasedTracker = new ColorBasedTracker();
	private TrackerHelper trackerHelper = new TrackerHelper();
	private ActivityMode selectedMenu = ActivityMode.CenterOfMassRgbMode;

	private enum ActivityMode {
//		TrackColorMode, // indicates that the user is currently choosing a color
//						// (which later on should be used for tracking the ROI)
		CenterOfMassRgbMode, // the center of mass is displayed in a rgb-frame
		BlackWhiteMode, // the inRange-function is displayed
		DilateMode, // the dilated black-White-Frame is displayed
		BoundingRectangleMode, // displays the bounding rectangle in rgb-frame
		CenterOfMassModeBlackWhiteMode, // the center of mass is displayed in a
										// black-white-frame
		ContoursMode, // the contours are displayed
		TrackingPathMode,
	}

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.color_based_tracking_surface_view);
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_based_tracking_surface_view);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
		mOpenCvCameraView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				isTracking = true;
				mOpenCvCameraView.setClickable(false);
			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mItems = new MenuItem[ActivityMode.values().length];
		for (int i = 0; i < mItems.length; i++) {
			mItems[i] = menu.add(ActivityMode.values()[i].toString());
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
		selectedMenu = ActivityMode.valueOf((String) item.getTitle());
		return true;
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
				mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {
	}

	public void onCameraViewStopped() {
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		Mat rgba = inputFrame.rgba();

		if (isTracking == false)
			return createRectangle(rgba, PIXELS_OF_PREVIEW, PIXELS_OF_PREVIEW,
					THICKNESS_OF_RECTANGLE);

		else {
			Mat hsv = new Mat();
			Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGB2HSV_FULL);

			if (trackColor == true) {
				trackColor = false;
				Point center = new Point(hsv.width() / 2, hsv.height() / 2);
				colorBasedTracker.setColorForTrackingHSV(trackerHelper
						.calcColorForTracking(hsv, center));
			}

			Point centerOfMass = null;
			
			try {
				centerOfMass = colorBasedTracker.calcCenterOfMass(hsv);
			} catch (IllegalArgumentException e) {
			}
			;
			switch (selectedMenu) {
			case BlackWhiteMode:
				rgba = colorBasedTracker.getBlackWhiteMask();
				break;

			case BoundingRectangleMode:
				if (colorBasedTracker.getBoundingRect() != null)
					rgba = createRectangle(rgba,
							colorBasedTracker.getBoundingRect());
				break;

			case CenterOfMassModeBlackWhiteMode:
				Mat rgbaBwMask = new Mat();
				Imgproc.cvtColor(colorBasedTracker.getDilatedMask(),
							rgbaBwMask, Imgproc.COLOR_GRAY2BGRA);
				if(colorBasedTracker.getBoundingRect() != null)
					rgba = drawCenterOfMass(rgbaBwMask, centerOfMass);
				break;

			case CenterOfMassRgbMode:
				if(colorBasedTracker.getBoundingRect() != null)
					rgba = drawCenterOfMass(rgba, centerOfMass);
				break;

			case ContoursMode:
				if(colorBasedTracker.getBoundingRect() != null)
					// -1 because all contours are drawn
					Imgproc.drawContours(rgba, colorBasedTracker.getContour(), -1, INDICATING_COLOR);
				break;

			case DilateMode:
				rgba = colorBasedTracker.getDilatedMask();
				break;
				
			case TrackingPathMode:
				rgba = drawTrackPath(rgba, colorBasedTracker.getTrackPath());
				break;

			}

			return rgba;
		}
	}

	private Mat createRectangle(Mat picture, int heigth, int width,
			int borderSize) {
		int OffsetXofRect = picture.width() / 2 - width / 2 - borderSize;
		int OffsetYofRect = picture.height() / 2 - heigth / 2 - borderSize;

		Point topLeft = new Point(OffsetXofRect - THICKNESS_OF_RECTANGLE,
				OffsetYofRect - THICKNESS_OF_RECTANGLE);
		Point bottomRight = new Point(OffsetXofRect + width
				+ THICKNESS_OF_RECTANGLE, OffsetYofRect + heigth
				+ THICKNESS_OF_RECTANGLE);
		Core.rectangle(picture, topLeft, bottomRight, INDICATING_COLOR,
				THICKNESS_OF_RECTANGLE);
		return picture;
	}

	private Mat createRectangle(Mat picture, Rect rec) {
		Core.rectangle(picture, rec.tl(), rec.br(), INDICATING_COLOR);
		return picture;
	}

	private Mat drawCenterOfMass(Mat picture, Point centerOfMass) {
		
		int centerMaxX = (int) (centerOfMass.x + SIZE_OF_CENTER_OF_MASS / 2);
		int centerMinX = (int) (centerOfMass.x - SIZE_OF_CENTER_OF_MASS / 2);
		int centerMaxY = (int) (centerOfMass.y + SIZE_OF_CENTER_OF_MASS / 2);
		int centerMinY = (int) (centerOfMass.y - SIZE_OF_CENTER_OF_MASS / 2);

		if (centerMaxX > picture.width() - 1)
			centerMaxX = picture.width() - 1;

		if (centerMaxY > picture.height() - 1)
			centerMaxY = picture.height() - 1;

		if (centerMinX < 0)
			centerMinX = 0;

		if (centerMinY < 0)
			centerMinY = 0;

		// for printing the bounding rectangle
		// Core.rectangle(maskRGB, new Point(boundingRect.x,
		// boundingRect.y),
		// new Point(boundingRect.x + boundingRect.width, boundingRect.y +
		// boundingRect.height),
		// COLOR_OF_RECT, THICKNESS_OF_RECTANGLE);

		Mat subMatColorMass = picture.submat(centerMinY, centerMaxY,
				centerMinX, centerMaxX);
		subMatColorMass.setTo(INDICATING_COLOR);

		return picture;
	}

	private Mat drawTrackPath(Mat rgba, List<Point> path){
		for(int i = 0; i<path.size() - 1; i++)
		Core.line(rgba, path.get(i), path.get(i+1), INDICATING_COLOR);
		return rgba;
	}
	
	
	@Override
	public void onBackPressed() {
		startActivity(new Intent(this, ColorBasedTrackingActivity.class));
		finish();
	}

}
