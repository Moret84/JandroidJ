package teamkipez.jandroid.jandroidclient;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class StartActivity extends AppCompatActivity {

    Button play, connect;
    ImageView connectionStatusImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

		//Connections.getInstance().attemptJoystickConnection();
		Connections.getInstance().start();
        //updateConnectionStatus();

        play = (Button) findViewById(R.id.button_play);
        play.setOnClickListener(playListener);
    }

    private View.OnClickListener playListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            //if(Connections.getInstance().joystickIsConnected()) {

                Intent newActivity = new Intent();
                newActivity.setClass(getApplicationContext(), ControlActivity.class);
                startActivity(newActivity);

            //}else{

             //   Toast.makeText(getApplicationContext(), R.string.connection_not, Toast.LENGTH_SHORT).show();
            //}
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start, menu);
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

            //if(!Connections.getInstance().joystickIsConnected())
            //{
             //   Connections.getInstance().attemptJoystickConnection();
                //updateConnectionStatus();
            //}

         //   else
          //      Toast.makeText(getApplicationContext(), R.string.already_connected, Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionStatus(){

        connectionStatusImg = (ImageView) findViewById(R.id.image_status);
        //if(Connections.getInstance().joystickIsConnected()){
        //    connectionStatusImg.setBackgroundResource(R.drawable.actif);
         //   Toast.makeText(getApplicationContext(), R.string.connection_success, Toast.LENGTH_LONG).show();
        //}else{
         //   connectionStatusImg.setBackgroundResource(R.drawable.non_actif);
          //  Toast.makeText(getApplicationContext(), R.string.connection_failed, Toast.LENGTH_LONG).show();
        //}

    }
}
