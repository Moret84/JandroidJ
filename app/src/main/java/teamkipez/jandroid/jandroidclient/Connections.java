package teamkipez.jandroid.jandroidclient;

import android.app.IntentService;
import android.content.Intent;
import java.net.Socket;
import java.io.IOException;
import java.io.DataOutputStream;
import java.lang.Thread;
import java.lang.Runnable;
import java.net.UnknownHostException;
import android.util.Log;

public class Connections extends IntentService
{
	//private static final Connections instance = new Connections();
	public static String IP = "192.168.12.1";
	public static int PORT = 23;

	private Socket mJoystickSocket = null;
	private DataOutputStream mDos = null;

	public Connections(String name)
	{
		joystickConnect();
	}

	@Override
	public void onHandleIntent(Intent intent)
	{

	}

	private void joystickConnect()
	{
		try
		{
			mJoystickSocket = new Socket(IP, PORT);
			mDos = new DataOutputStream(mJoystickSocket.getOutputStream());
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	private static Connections getInstance()
	{
		//return instance;
	}

	private void attemptJoystickConnection()
	{
		joystickConnect();
	}

	private boolean joystickIsConnected()
	{
		return (null != mJoystickSocket);
	}

	private void sendJoystickInput(byte x, byte y)
	{
		try
		{
			mDos.writeByte(x);
			mDos.writeByte(y);
			Log.i("jandroid", "j'ai envoyé ça pèse sa race " + x + " " + y);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}
