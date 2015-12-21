package teamkipez.jandroid.jandroidclient;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;

import com.camera.simplemjpeg.MjpegView;
import com.camera.simplemjpeg.MjpegInputStream;

import java.lang.Math;
import java.util.ArrayList;

public class ControlActivity extends Activity implements SensorEventListener{

	private MjpegView videoView = null;
	private static final String URL = "http://192.168.12.1:8090/?action=stream";
	//private static String URL = "http://mjpeg.sanford.io/count.mjpeg";
	private boolean suspending = false;
	public static byte MotorHeader = 'M';
	public static byte CamHeader = 'C';


	private SensorManager sensorManager;
	private Sensor accelerometer;
	private Joystick leftJoystick;
	private Joystick rightJoystick;
	private ImageButton speakButton;
	private ImageButton sensorButton;
	boolean sensor = false;

	//DEBUG
	private TextView angleTextView;
	private TextView powerTextView;
	private TextView directionTextView;
	private TextView x;
	private TextView y;
	private TextView z;
	//!DEBUG

	@Override
	protected void onCreate(Bundle savedInstanceState) {

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
			public void run()
			{
				videoView.setSource(MjpegInputStream.read(URL));
			}
		}).start();

		//Speak Recognition
		speakButton = (ImageButton) findViewById(R.id.button_speach);
		speakButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				speechInput();
			}
		});

		//Debugs Joystick
		angleTextView = (TextView) findViewById(R.id.angleTextView);
		powerTextView = (TextView) findViewById(R.id.powerTextView);
		directionTextView = (TextView) findViewById(R.id.directionTextView);

		//Joysticks
		rightJoystick = (Joystick) findViewById(R.id.joystickRight);
		leftJoystick = (Joystick) findViewById(R.id.joystick);

		leftJoystick.setJoystickListener(initJoystick(MotorHeader));
		rightJoystick.setJoystickListener(initJoystick(CamHeader));

		//DEBUG ACCELEROMETERS
		x = (TextView) findViewById(R.id.x);
		y = (TextView) findViewById(R.id.y);
		z = (TextView) findViewById(R.id.z);

		sensorButton = (ImageButton) findViewById(R.id.button_sensor);

		sensorButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v)
			{

				if(sensor)
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

	private void enableSensor()
	{
		if(null != accelerometer)
		{
			sensor = true;
			sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
			Toast.makeText(getApplicationContext(), R.string.resume, Toast.LENGTH_SHORT).show();
		}
	}

	private void disableSensor()
	{
		if(null != accelerometer)
		{
			sensor = false;
			sensorManager.unregisterListener(this);
			Toast.makeText(getApplicationContext(), R.string.resume, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_control, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

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

				angleTextView.setText("x " + String.valueOf(x));
				powerTextView.setText("y " + String.valueOf(y));

				sendJoystickInput(which, x, y);
			}

			@Override
			public void onUp()
			{
				angleTextView.setText("x " + 0);
				powerTextView.setText("y " + 0);

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

	private void speechInput()
	{
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		try
		{
			startActivityForResult(intent, 100);
		} catch (ActivityNotFoundException a) {
			Toast.makeText(getApplicationContext(), R.string.wrong_string, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode)
		{
			case 100:
				if (resultCode == RESULT_OK && null != data)
				{
					ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
					Toast.makeText(getApplicationContext(),result.get(0), Toast.LENGTH_SHORT).show();
				}
				break;
		}
	}

	protected void onResume()
	{
		super.onResume();

		if(sensor)
			sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

		if(videoView != null)
			if(suspending)
			{
				videoView.startPlayback();
				suspending = false;
			}
	}

	protected void onPause()
	{
		super.onPause();
		videoView.stopPlayback();
		sensorManager.unregisterListener(this);
		suspending = true;
	}

	public void onDestroy()
	{
		super.onDestroy();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		float vx,vy,vz;
		vx = event.values[0];
		vy = event.values[1];
		vz = event.values[2];

		x.setText(Float.toString(vx));
		y.setText(Float.toString(vy));
		z.setText(Float.toString(vz));
	}
}
