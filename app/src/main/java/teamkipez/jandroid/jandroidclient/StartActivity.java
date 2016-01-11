package teamkipez.jandroid.jandroidclient;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class StartActivity extends AppCompatActivity implements IConnectionState {

	Button play, connect;
	ImageView connectionStatusImg;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);

		Connections.getInstance().setConnectionStateListener(this);
		Connections.getInstance().start();

		connectionStatusImg = (ImageView) findViewById(R.id.image_status);
		play = (Button) findViewById(R.id.button_play);
		play.setOnClickListener(playListener);
	}

	private View.OnClickListener playListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {

			Intent newActivity = new Intent();
			newActivity.setClass(getApplicationContext(), ControlActivity.class);
			startActivity(newActivity);
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
		if (id == R.id.action_settings)
		{
			askConnection();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void askConnection()
	{
		Message msg = Connections.getInstance().handler.obtainMessage();
		msg.what = Connections.CONNECT;
		Connections.getInstance().handler.sendMessage(msg);
	}

	@Override
	public void onConnectionSuccess()
	{
		connectionStatusImg.setBackgroundResource(R.drawable.actif);
		Toast.makeText(getApplicationContext(), R.string.connection_success, Toast.LENGTH_LONG).show();
	}

	@Override
	public void onConnectionFail()
	{
		connectionStatusImg.setBackgroundResource(R.drawable.non_actif);
		Toast.makeText(getApplicationContext(), R.string.connection_failed, Toast.LENGTH_LONG).show();
	}

	@Override
	public void alreadyConnected()
	{
		Toast.makeText(getApplicationContext(), R.string.already_connected, Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
	}
}
