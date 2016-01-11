package teamkipez.jandroid.jandroidclient;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.List;

public class TrackingActivity extends AppCompatActivity implements CvCameraViewListener2, SeekBar.OnSeekBarChangeListener
{
    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;

    private SeekBar hmin;
    private SeekBar hmax;
    private SeekBar smin;
    private SeekBar smax;
    private SeekBar vmin;
    private SeekBar vmax;

    private Mat erodeElement;
    private Mat dilateElement;
    private Mat toModify;
    private Mat ranged;
    private Mat tmp;
    private Rect bounding;
    private double x,y;
    private int width = 384;
    private int height = 216;
    private double area = 0;
    private double refArea = 0;

    private double MOVE;

    boolean objectFound = false;

    int H_MIN = 0;
    int H_MAX = 256;
    int S_MIN = 0;
    int S_MAX = 256;
    int V_MIN = 0;
    int V_MAX = 256;

    int ID_H_MIN, ID_H_MAX, ID_S_MIN, ID_S_MAX, ID_V_MIN, ID_V_MAX;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
	{
        @Override
        public void onManagerConnected(int status)
		{
            switch (status)
			{
                case LoaderCallbackInterface.SUCCESS:
				{
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    erodeElement = new Mat();
                    dilateElement = new Mat();
                    toModify = new Mat();
                    ranged = new Mat();
                    tmp = new Mat();
					break;
                }
                default:
                    super.onManagerConnected(status);
					break;
            }
        }
    };

    public TrackingActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.tutorial1_surface_view);

        setupSeekBars();

		mOpenCvCameraView=(CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        mOpenCvCameraView.setMaxFrameSize(width,height);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        }

        @Override
        public void onPause()
        {
            super.onPause();
            if (mOpenCvCameraView != null)
                mOpenCvCameraView.disableView();
        }

        @Override
        public void onResume()
        {
            super.onResume();
            if (!OpenCVLoader.initDebug())
			{
                Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
            }
			else
			{
                Log.d(TAG, "OpenCV library found inside package. Using it!");
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        }

    public void setupSeekBars()
	{
        hmin = (SeekBar) findViewById(R.id.seekBar);
        hmax = (SeekBar) findViewById(R.id.seekBar2);
        smin = (SeekBar) findViewById(R.id.seekBar3);
        smax = (SeekBar) findViewById(R.id.seekBar4);
        vmin = (SeekBar) findViewById(R.id.seekBar5);
        vmax = (SeekBar) findViewById(R.id.seekBar6);

        hmin.setOnSeekBarChangeListener(this);
        hmax.setOnSeekBarChangeListener(this);
        smax.setOnSeekBarChangeListener(this);
        smin.setOnSeekBarChangeListener(this);
        vmax.setOnSeekBarChangeListener(this);
        vmin.setOnSeekBarChangeListener(this);

        hmin.setMax(256);
        hmax.setMax(256);
        smin.setMax(256);
        smax.setMax(256);
        vmin.setMax(256);
        vmax.setMax(256);

        hmin.setProgress(H_MIN);
        hmax.setProgress(H_MAX);
        smax.setProgress(S_MAX);
        smin.setProgress(S_MIN);
        vmin.setProgress(V_MIN);
        vmax.setProgress(V_MAX);

        ID_H_MAX = hmax.getId();
        ID_H_MIN = hmin.getId();
        ID_S_MAX = smax.getId();
        ID_S_MIN = smin.getId();
        ID_V_MAX = vmax.getId();
        ID_V_MIN = vmin.getId();
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

        toModify = inputFrame.rgba();


        Imgproc.cvtColor(toModify, toModify, Imgproc.COLOR_BGR2HSV);


        Core.inRange(toModify, new Scalar(H_MIN, S_MIN, V_MIN), new Scalar(H_MAX, S_MAX, V_MAX), ranged);

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
            Imgproc.putText(ranged, "TOO MUCH NOISE", new Point(0, 50), 1, 1, new Scalar(0, 0, 255), 2);
        else
            Imgproc.putText(ranged, "X", new Point(x, y), 1, 1, new Scalar(0, 0, 255), 2);

        MOVE =(x - (toModify.size().width)/2) * 0.5;
        Log.i(TAG, "Move X : "+MOVE);
		sendJoystickInput(ControlActivity.MotorHeader, (byte) MOVE, (byte) 50 );
        return ranged;
    }

	private void sendJoystickInput(final byte header, final byte x, final byte y)
	{
		if(Connections.getInstance().handler != null)
		{
			Message msg = Connections.getInstance().handler.obtainMessage();
			msg.what = Connections.SEND;
			Bundle bundle = new Bundle();
			bundle.putByte(Connections.HEADER, header);
			bundle.putByte(Connections.X, x);
			bundle.putByte(Connections.Y, y);
			msg.setData(bundle);
			Connections.getInstance().handler.sendMessage(msg);
		}
	}

    @Override
    public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser)
	{
        int id = seekBar.getId();

        if(id == ID_H_MAX)
            H_MAX = progresValue;
        else if(id == ID_H_MIN)
            H_MIN = progresValue;
        else if(id == ID_S_MAX)
            S_MAX = progresValue;
        else if(id == ID_S_MIN)
            S_MIN = progresValue;
        else if (id == ID_V_MAX)
            V_MAX = progresValue;
        else if(id == ID_V_MIN)
            V_MIN = progresValue;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar)
	{
        //Toast.makeText(getApplicationContext(), "Started tracking seekbar", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar)
	{

        //textView.setText("Covered: " + progress + "/" + seekBar.getMax());
        //Toast.makeText(getApplicationContext(), "Stopped tracking seekbar", Toast.LENGTH_SHORT).show();
    }
}
