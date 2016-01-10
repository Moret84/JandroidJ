package teamkipez.jandroid.jandroidclient;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;

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

public class ControlActivity extends Activity implements SensorEventListener, RecognitionListener
{
	private static final String SEARCH_TYPE = "pesance";

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
	private Joystick leftJoystick;
	private Joystick rightJoystick;

	//Speak
	private ImageButton speakButton;
	private SpeechRecognizer recognizer;

	//Tracking
	private Button trackingButton;

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

		trackingButton = (Button) findViewById(R.id.trackingButton);
		trackingButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(ControlActivity.this, TrackingActivity.class);
				startActivity(intent);
			}
		});
	}

	@Override
	protected void onResume()
	{
		super.onResume();

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

				sendJoystickInput(which, x, y);
			}

			@Override
			public void onUp()
			{
				sendJoystickInput(which, (byte) 0, (byte) 0);
			}
		};
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
		sendJoystickInput(MotorHeader, x, y);
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
		sendJoystickInput(MotorHeader, x, y);
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
