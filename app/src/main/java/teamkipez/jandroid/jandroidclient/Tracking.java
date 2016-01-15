package teamkipez.jandroid.jandroidclient;

import android.graphics.Bitmap;
import android.util.SparseArray;
import android.widget.SeekBar;

import java.util.List;
import java.util.ArrayList;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import teamkipez.jandroid.jandroidclient.R;

public class Tracking
{
	private final int TRACKING_SETTINGS_NB = 6, MAX_SETTING_VALUE = 256;
	private Mat erodeElement, dilateElement, toModify, ranged, tmp;
	private Rect bounding;
	private int width = 384, height = 216;
	private double area = 0, refArea = 0, x, y, MOVEX, MOVEY;
	private boolean objectFound = false;
	private SparseArray<SeekBar> seekBarsMap;
	private ObjectTrackingListener mOTL;

	public Tracking(SparseArray sBM)
	{
		seekBarsMap = sBM;
		erodeElement = new Mat();
		dilateElement = new Mat();
		toModify = new Mat();
		ranged = new Mat();
		tmp = new Mat();
	}

	public void setObjectTrackingListener(ObjectTrackingListener OTL)
	{
		mOTL = OTL;
	}

	private Mat bitmapToMat(Bitmap input)
	{
		input = input.copy(Bitmap.Config.ARGB_8888, true);
		Mat output = new Mat();
		Utils.bitmapToMat(input, output);
		return output;
	}

	private Bitmap matToBitmap(Mat input)
	{
		Bitmap output = Bitmap.createBitmap(input.cols(), input.rows(), Bitmap.Config.ARGB_8888);;
		Utils.matToBitmap(input, output);
		return output;
	}

	public Bitmap processFrame(Bitmap frame)
	{
		//Convert Bitmap to OpenCV Mat
		toModify = bitmapToMat(frame);

		//Resize in order to speed up the processing
		Imgproc.resize(toModify, toModify, new Size(width, height));

		//Processing
		Imgproc.cvtColor(toModify, toModify, Imgproc.COLOR_BGR2HSV);

		Core.inRange(toModify,
				new Scalar(
					seekBarsMap.get(R.id.seekBarHmin).getProgress(),
					seekBarsMap.get(R.id.seekBarSmin).getProgress(),
					seekBarsMap.get(R.id.seekBarVmin).getProgress()),
				new Scalar(
					seekBarsMap.get(R.id.seekBarHmax).getProgress(),
					seekBarsMap.get(R.id.seekBarSmax).getProgress(),
					seekBarsMap.get(R.id.seekBarVmax).getProgress())
				, ranged);

		erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
		dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(8, 8));

		Imgproc.erode(ranged, ranged, erodeElement);
		Imgproc.erode(ranged, ranged, erodeElement);

		Imgproc.dilate(ranged, ranged, dilateElement);
		Imgproc.dilate(ranged, ranged, dilateElement);

		ranged.copyTo(tmp);

		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		MatOfInt4 hierarchy = new MatOfInt4();
		Imgproc.findContours(tmp, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_NONE);

		double width = hierarchy.size().width;
		refArea = 0;
		area = 0;

		if (width > 0 && width < 15)
		{
			for (int index = 0; index < contours.size(); index++)
			{
				bounding = Imgproc.boundingRect(contours.get(index));
				area = bounding.area();
				if (area > refArea && area > 100) {

					x = bounding.x + (bounding.width /2);
					y = bounding.y + (bounding.height /2);
					objectFound = true;
					refArea = area;
				} else
					objectFound = false;
			}
		}

		if (!objectFound)
		{
			Imgproc.putText(ranged, "TOO MUCH NOISE", new Point(0, 50), 1, 1, new Scalar(0, 0, 255), 2);
			MOVEX = MOVEY = 0;
			mOTL.onObjectDisappeared();
		}
		else
		{
			MOVEX = (x - (toModify.size().width)/2) * 0.4;
			MOVEY = 50;
			Imgproc.putText(ranged, "X", new Point(x, y), 1, 1, new Scalar(0, 0, 255), 2);
			mOTL.onNewObjectPosition(MOVEX, MOVEY);
		}

		//Convert back to Bitmap and return
		return matToBitmap(ranged);
	}
}
