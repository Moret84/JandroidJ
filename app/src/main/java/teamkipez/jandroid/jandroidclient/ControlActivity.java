package teamkipez.jandroid.jandroidclient;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class ControlActivity extends Activity {

    private static ProgressDialog progressDialog;
    VideoView videoView;
    String url = "https://archive.org/download/ksnn_compilation_master_the_internet/ksnn_compilation_master_the_internet_512kb.mp4";

    private JoystickView joystick;
    private TextView angleTextView;
    private TextView powerTextView;
    private TextView directionTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        videoView = (VideoView) findViewById(R.id.videoView);
        progressDialog = ProgressDialog.show(ControlActivity.this, "", "Buffering video...", true);
        progressDialog.setCancelable(true);

        //PlayVideo();

        angleTextView = (TextView) findViewById(R.id.angleTextView);
        powerTextView = (TextView) findViewById(R.id.powerTextView);
        directionTextView = (TextView) findViewById(R.id.directionTextView);
        joystick = (JoystickView) findViewById(R.id.joystick);

		Connections.getInstance().prepareInputSending();
        joystick.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener() {
            @Override
            public void onValueChanged(int angle, int power, int direction) {
                angleTextView.setText(" " + String.valueOf(angle) + "°");
                powerTextView.setText(" " + String.valueOf(power) + "%");

				Connections.getInstance().sendJoystickInput(angle, power);

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
}
