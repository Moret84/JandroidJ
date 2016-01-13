package teamkipez.jandroid.jandroidclient;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.camera.simplemjpeg.MjpegView;
import com.camera.simplemjpeg.MjpegInputStream;

import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import java.io.IOException;
import java.io.File;
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

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

public class ControlActivity extends Activity implements SensorEventListener, RecognitionListener, NewFrameListener
{
	private static final String SEARCH_TYPE = "pesance";
	private static final String TAG = "ControlActivity";

	private MjpegView videoView = null;
	private static final String URL = "http://192.168.12.1:8090/?action=stream";
	//private static String URL = "http://mjpeg.sanford.io/count.mjpeg";
	public static final byte MotorHeader = 'M';
	public static final byte CamHeader = 'C';

	//Sensor
	private SensorManager sensorManager;
	private Sensor accelerometer;
	private ImageButton sensorButton;
	boolean sensor = false;

	//Joysticks
	private Joystick leftJoystick, rightJoystick;

	//Speak
	private ImageButton speakButton;
	private SpeechRecognizer recognizer;

	//Tracking
	private final int TRACKING_SETTINGS_NB = 6, MAX_SETTING_VALUE = 256;
	private boolean trackingIsOn = false, visibleSeekBars = false;
	private LinearLayout seekBarsLayout;
	private ImageButton trackingButton, trackingSettingsButton;
	private SparseArray<SeekBar> seekBarsMap;

	private Mat erodeElement, dilateElement, toModify, ranged, tmp;
	private Rect bounding;
	private int width = 384, height = 216;
	private double area = 0, refArea = 0, x, y, MOVEX, MOVEY;
	private boolean objectFound = false;

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

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_control);

		//Video
		videoView = (MjpegView) findViewById(R.id.mv);
		videoView.setDisplayMode(MjpegView.SIZE_FULLSCREEN);
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				videoView.setSource(MjpegInputStream.read(URL));
			}
		}).start();

		//Tracking
		videoView.setNewFrameListener(this);
		trackingButton = (ImageButton) findViewById(R.id.trackingButton);
		trackingButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				toggleTracking();
			}
		});

		trackingSettingsButton = (ImageButton) findViewById(R.id.trackingSettingsButton);
		trackingSettingsButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				toggleTrackingSettings();
			}
		});

		seekBarsLayout = (LinearLayout) findViewById(R.id.seekBarsLayout);
		setupSeekBars();

		//Speak Recognition
		speakButton = (ImageButton) findViewById(R.id.button_speach);
		speakButton.setVisibility(View.INVISIBLE);
		speakButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				speechInput();
			}
		});

		//Synchronize Assets for pocketsphinx
		new AsyncTask<Void, Void, Exception>()
		{
			@Override
			protected Exception doInBackground(Void... params)
			{
				try
				{
					Assets assets = new Assets(ControlActivity.this);
					File assetDir = assets.syncAssets();
					setupRecognizer(assetDir);
				}
				catch(IOException e)
				{
					return e;
				}
				return null;
			}

			@Override
			protected void onPostExecute(Exception result)
			{
				if(null != result)
					result.printStackTrace();

				speakButton.setVisibility(View.VISIBLE);
			}
		}.execute();

		//Joysticks
		rightJoystick = (Joystick) findViewById(R.id.joystickRight);
		leftJoystick = (Joystick) findViewById(R.id.joystick);
		leftJoystick.setJoystickListener(initJoystick(MotorHeader));
		rightJoystick.setJoystickListener(initJoystick(CamHeader));

		//Sensor
		sensorButton = (ImageButton) findViewById(R.id.button_sensor);
		sensorButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if(!sensor)
					enableSensor();
				else
					disableSensor();
			}
		});

		//Check Accelerometers
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null)
			accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		else
			sensorButton.setVisibility(View.INVISIBLE);
	}

	@Override
	protected void onResume()
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

		if(sensor)
			sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		videoView.stopPlayback();
		sensorManager.unregisterListener(this);
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		recognizer.cancel();
		recognizer.shutdown();
	}

	//Tracking stuff
	private void toggleTrackingSettings()
	{
		if(trackingIsOn)
		{
			if(visibleSeekBars)
			{
				seekBarsLayout.setVisibility(View.INVISIBLE);
				visibleSeekBars = false;
			}
			else
			{
				seekBarsLayout.setVisibility(View.VISIBLE);
				visibleSeekBars = true;
			}
		}
	}

	private void resizeAndMoveVideoView()
	{
		videoView.getLayoutParams().width = 320;
		videoView.getLayoutParams().height = 240;
		((RelativeLayout.LayoutParams) videoView.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_BOTTOM) ;
	}

	private void toggleTracking()
	{
		if(trackingIsOn)
		{
			if(visibleSeekBars)
				toggleTrackingSettings();
			sensorButton.setVisibility(View.VISIBLE);
			speakButton.setVisibility(View.VISIBLE);
			trackingButton.setBackgroundResource(R.drawable.tracking_white);
			trackingSettingsButton.setVisibility(View.INVISIBLE);
			trackingIsOn = false;
		}
		else
		{
			sensorButton.setVisibility(View.INVISIBLE);
			speakButton.setVisibility(View.INVISIBLE);
			trackingButton.setBackgroundResource(R.drawable.tracking_black);
			trackingSettingsButton.setVisibility(View.VISIBLE);
			trackingIsOn = true;
		}
	}

	private void setupSeekBars()
	{
		seekBarsMap = new SparseArray<SeekBar>(TRACKING_SETTINGS_NB);

		seekBarsMap.append(R.id.seekBarHmin, (SeekBar) findViewById(R.id.seekBarHmin));
		seekBarsMap.append(R.id.seekBarHmax, (SeekBar) findViewById(R.id.seekBarHmax));
		seekBarsMap.append(R.id.seekBarSmin, (SeekBar) findViewById(R.id.seekBarSmin));
		seekBarsMap.append(R.id.seekBarSmax, (SeekBar) findViewById(R.id.seekBarSmax));
		seekBarsMap.append(R.id.seekBarVmin, (SeekBar) findViewById(R.id.seekBarVmin));
		seekBarsMap.append(R.id.seekBarVmax, (SeekBar) findViewById(R.id.seekBarVmax));

		for(int i = 0; i < seekBarsMap.size(); i++)
		{
			SeekBar b = seekBarsMap.valueAt(i);
			b.setMax(MAX_SETTING_VALUE);
			b.setProgress((i % 2 == 0) ? 0 : MAX_SETTING_VALUE);
		}
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

	private Bitmap processFrame(Bitmap frame)
	{
		//Convert Bitmap to OpenCV Mat
		toModify = bitmapToMat(frame);
		Imgproc.resize(toModify, toModify, new Size(384, 216));

		//Processing
		Imgproc.cvtColor(toModify, toModify, Imgproc.COLOR_BGR2HSV);

		Core.inRange(toModify,
				new Scalar(seekBarsMap.get(R.id.seekBarHmin).getProgress(),
					seekBarsMap.get(R.id.seekBarSmin).getProgress(),
					seekBarsMap.get(R.id.seekBarVmin).getProgress()),
				new Scalar(seekBarsMap.get(R.id.seekBarHmax).getProgress(),
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
			MOVEX = 0;
			MOVEY = 0;
		}
		else
		{
			MOVEX = (x - (toModify.size().width)/2) * 0.4;
			MOVEY = 50;
			Imgproc.putText(ranged, "X", new Point(x, y), 1, 1, new Scalar(0, 0, 255), 2);
		}

		Connections.getInstance().addCommandToSendQueue(MotorHeader, (byte) MOVEX, (byte) MOVEY);

		//Convert back to Bitmap and return
		return matToBitmap(ranged);
	}

	@Override
	public Bitmap onFrame(Bitmap frame)
	{
		return trackingIsOn? processFrame(frame): frame;
	}

	//Joysticks
	private JoystickListener initJoystick(final byte which)
	{
		return new JoystickListener()
		{
			@Override
			public void onDown()
			{
			}

			@Override
			public void onDrag(float degrees, float offset)
			{
				offset *= 100.0;
				byte x = (byte) (Math.cos(Math.toRadians(degrees)) * offset);
				byte y = (byte) (Math.sin(Math.toRadians(degrees)) * offset);

				Connections.getInstance().addCommandToSendQueue(which, x, y);
			}

			@Override
			public void onUp()
			{
				Connections.getInstance().addCommandToSendQueue(which, (byte) 0, (byte) 0);
			}
		};
	}

	//Speech recognition
	@Override
	public void onPartialResult(Hypothesis hypothesis)
	{
		if(hypothesis == null)
			return;
		sendVoiceCommand(hypothesis.getHypstr());
	}

	@Override
	public void onResult(Hypothesis hypothesis)
	{
	}

	@Override
	public void onBeginningOfSpeech()
	{
	}

	@Override
	public void onEndOfSpeech()
	{
	}

	@Override
	public void onError(Exception error)
	{
	}

	@Override
	public void onTimeout()
	{
		speakButton.setBackgroundResource(R.drawable.ico_mic);
	}

	private void setupRecognizer(File assetsDir) throws IOException
	{
		recognizer = SpeechRecognizerSetup.defaultSetup()
			.setAcousticModel(new File(assetsDir, "fr-fr-ptm"))
			.setDictionary(new File(assetsDir, "fr.dict"))
			.setRawLogDir(assetsDir)
			.setBoolean("-allphone_ci", true)
			.getRecognizer();

		recognizer.addListener(this);

		// Create language model search
		File directionGrammar = new File(assetsDir, "direction.gram");
		recognizer.addGrammarSearch(SEARCH_TYPE, directionGrammar);
	}

	private void speechInput()
	{
		speakButton.setBackgroundResource(R.drawable.active_mic);
		recognizer.stop();
		recognizer.startListening(SEARCH_TYPE, 1000);
	}

	private void sendVoiceCommand(String commande)
	{
		byte x = 0, y = 0;

		switch(commande)
		{
			case "avance":
				x = 0;
				y = 100;
				break;
			case "recule":
				x = 0;
				y = -100;
				break;
			case "stop":
				x = 0;
				y = 0;
				break;
			case "gauche":
				x = -45;
				y = 100;
				break;
			case "droite":
				x = 45;
				y = 100;
				break;
		}
		Connections.getInstance().addCommandToSendQueue(MotorHeader, x, y);
		recognizer.stop();
		speakButton.setBackgroundResource(R.drawable.ico_mic);
	}

	//Sensor
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		byte y = (byte) (-1 * filterData(event.values[0])),x = filterData(event.values[1]);
		Connections.getInstance().addCommandToSendQueue(MotorHeader, x, y);
	}

	private void enableSensor()
	{
		if(null != accelerometer)
		{
			sensor = true;
			sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
			sensorButton.setBackgroundResource(R.drawable.sensor);
			leftJoystick.setJoystickListener(null);
		}
	}

	private void disableSensor()
	{
		if(null != accelerometer)
		{
			sensor = false;
			sensorManager.unregisterListener(this);
			sensorButton.setBackgroundResource(R.drawable.sensor_inactive);
			leftJoystick.setJoystickListener(initJoystick(MotorHeader));
		}
	}

	private byte filterData(float input)
	{
		byte output = (byte) (input * 10.0f);

		//Reduce sensitivity
		if(Math.abs(output) <= 30)
			output = 0;

		return output;
	}
}
