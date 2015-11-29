package teamkipez.jandroid.jandroidclient;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.util.ArrayList;
import java.util.Locale;

public class ControlActivity extends Activity implements SensorEventListener{

    private static ProgressDialog progressDialog;
    VideoView videoView;
    String url = "https://archive.org/download/ksnn_compilation_master_the_internet/ksnn_compilation_master_the_internet_512kb.mp4";

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private JoystickView joystickLeft;
    private JoystickView joystickRight;
    private ImageButton buttonSpeak;
    private ImageButton buttonSensor;
    boolean sensor = true;

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
        setContentView(R.layout.activity_control);

        //Settings viewComponents
        videoView = (VideoView) findViewById(R.id.videoView);
        progressDialog = ProgressDialog.show(ControlActivity.this, "", "Buffering video...", true);
        progressDialog.setCancelable(true);

        //Speack Reco
        buttonSpeak = (ImageButton) findViewById(R.id.button_speach);
        buttonSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                speechInput();
            }
        });

        //PlayVideo();


        //Debugs Joystick
        angleTextView = (TextView) findViewById(R.id.angleTextView);
        powerTextView = (TextView) findViewById(R.id.powerTextView);
        directionTextView = (TextView) findViewById(R.id.directionTextView);


        //Joysticks
        joystickRight = (JoystickView) findViewById(R.id.joystickRight);
        joystickLeft = (JoystickView) findViewById(R.id.joystick);
        setJoystickListener(joystickLeft, "left");
        setJoystickListener(joystickRight, "right");


        //DEBUG ACCELEROMETERS
        x = (TextView) findViewById(R.id.x);
        y = (TextView) findViewById(R.id.y);
        z = (TextView) findViewById(R.id.z);

        buttonSensor = (ImageButton) findViewById(R.id.button_sensor);
        buttonSensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (sensor) {
                    onPause();
                    sensor = false;
                    Toast.makeText(getApplicationContext(), R.string.resume, Toast.LENGTH_SHORT).show();
                } else {
                    onResume();
                    sensor = true;
                    Toast.makeText(getApplicationContext(), R.string.pause, Toast.LENGTH_SHORT).show();
                }
            }
        });
		Connections.getInstance().prepareInputSending();
        //Check Accelerometers
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            onPause();
        } else {

            //HIDE THE BUTTON

        }

        //Force full Screen
        DisplayMetrics metrics = new DisplayMetrics(); getWindowManager().getDefaultDisplay().getMetrics(metrics);
        android.widget.RelativeLayout.LayoutParams params = (android.widget.RelativeLayout.LayoutParams) videoView.getLayoutParams();
        params.width =  metrics.widthPixels;
        params.height = metrics.heightPixels;
        params.leftMargin = 0;
        videoView.setLayoutParams(params);

        //Play the video
        PlayVideo();

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

    private void PlayVideo()
    {
        try
        {
            getWindow().setFormat(PixelFormat.TRANSLUCENT);
            MediaController mediaController = new MediaController(ControlActivity.this);
            mediaController.setAnchorView(videoView);

            Uri video = Uri.parse(url);
            videoView.setMediaController(null);
            Log.i("info", video.toString());
            videoView.setVideoURI(video);
            videoView.requestFocus();
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
            {

                public void onPrepared(MediaPlayer mp)
                {
                    progressDialog.dismiss();
                    videoView.start();
                }
            });


        }
        catch(Exception e)
        {
            progressDialog.dismiss();
            System.out.println("Video Play Error :"+e.toString());
            finish();
        }
    }

    private void setJoystickListener(JoystickView joystick, String dir){

       switch(dir){

           case "left":
               joystick.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener() {
                   @Override
                   public void onValueChanged(int angle, int power, int direction) {
                       angleTextView.setText(" " + String.valueOf(angle) + "°");
                       powerTextView.setText(" " + String.valueOf(power) + "%");
                       switch (direction) {
                           case JoystickView.FRONT:
                               directionTextView.setText("front");
                               break;

                           case JoystickView.FRONT_RIGHT:
                               directionTextView.setText("front");
                               break;


                           case JoystickView.RIGHT_BOTTOM:
                               directionTextView.setText("Bottom");
                               break;

                           case JoystickView.BOTTOM:
                               directionTextView.setText("Bottom");
                               break;

                           case JoystickView.BOTTOM_LEFT:
                               directionTextView.setText("Bottom ");
                               break;

                           case JoystickView.LEFT_FRONT:
                               directionTextView.setText("front");
                               break;

                           default:
                               directionTextView.setText("Center");
                       }
                   }
               }, JoystickView.DEFAULT_LOOP_INTERVAL);
               break;
           case "right" :
               joystick.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener() {
                   @Override
                   public void onValueChanged(int angle, int power, int direction) {
                       angleTextView.setText(" " + String.valueOf(angle) + "°");
                       powerTextView.setText(" " + String.valueOf(power) + "%");
                       switch (direction) {
                           case JoystickView.FRONT:
                               directionTextView.setText("front");
                               break;

                           case JoystickView.FRONT_RIGHT:
                               directionTextView.setText("Front-right");
                               break;

                           case JoystickView.RIGHT:
                               directionTextView.setText("Right");
                               break;

                           case JoystickView.RIGHT_BOTTOM:
                               directionTextView.setText("Right bottom");
                               break;

                           case JoystickView.BOTTOM:
                               directionTextView.setText("Bottom");
                               break;

                           case JoystickView.BOTTOM_LEFT:
                               directionTextView.setText("Bottom left");
                               break;

                           case JoystickView.LEFT:
                               directionTextView.setText("LEFT");
                               break;

                           case JoystickView.LEFT_FRONT:
                               directionTextView.setText("LEFT_FRONT");
                               break;

                           default:
                               directionTextView.setText("Center");
                       }
                   }
               }, JoystickView.DEFAULT_LOOP_INTERVAL);

       }
    }

    private void speechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        try {
            startActivityForResult(intent, 100);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(), R.string.wrong_string, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 100: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Toast.makeText(getApplicationContext(),result.get(0), Toast.LENGTH_SHORT).show();
                }
                break;
            }

        }
    }

    //onResume() register the accelerometer for listening the events
   protected void onResume() {
       super.onResume();
       if(!sensor) {
           sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
       }else
           sensor = false;
    }

    //onPause() unregister the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onSensorChanged(SensorEvent event) {

        float vx,vy,vz;
        vx = event.values[0];
        vy = event.values[1];
        vz = event.values[2];

        x.setText(Float.toString(vx));
        y.setText(Float.toString(vy));
        z.setText(Float.toString(vz));
    }
}

