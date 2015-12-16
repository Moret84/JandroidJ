package teamkipez.jandroid.jandroidclient;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.io.IOException;
import java.io.DataOutputStream;
import java.lang.Thread;
import java.net.Socket;

public class Connections extends Thread
{
	private static Connections instance = new Connections();
	public static String IP = "192.168.12.1";
	public static int PORT = 23;
	public static final int SEND = 1;
	public static final int STATE = 2;
	public static String HEADER = "HEADER";
	public static String X = "X";
	public static String Y = "Y";

	public Handler handler;

	private Socket mJoystickSocket = null;
	private DataOutputStream mDos = null;

	private Connections()
	{
	}

	@Override
	public void run()
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

		Looper.prepare();

		handler = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				Bundle bundle = msg.getData();
				int action = msg.what;
				switch(action)
				{
					case(SEND):
						sendJoystickInput(bundle.getByte(HEADER), bundle.getByte(X), bundle.getByte(Y));
						break;
					case(STATE):
						break;
				}
			}
		};

		Looper.loop();
	}

	public static Connections getInstance()
	{
		return instance;
	}

	private boolean joystickIsConnected()
	{
		return (null != mJoystickSocket);
	}

	private void sendJoystickInput(byte header, byte x, byte y)
	{
		try
		{
			mDos.writeByte(header);
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
