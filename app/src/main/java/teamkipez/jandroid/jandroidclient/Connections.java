package teamkipez.jandroid.jandroidclient;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.content.Intent;
import java.net.Socket;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.DataOutputStream;
import java.lang.Thread;
import java.lang.Runnable;
import java.net.UnknownHostException;
import android.util.Log;

public class Connections extends Thread
{
	private static Connections instance = new Connections();
	public static String IP = "192.168.12.1";
	public static int PORT = 23;
	public Handler mHandler;

	private Socket mJoystickSocket = null;
	private DataOutputStream mDos = null;
	private BufferedWriter mBufferedWriter = null;

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
		mHandler = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				Bundle bundle = msg.getData();
				String action = bundle.getString("ACTION");
				if(action.equals("SEND"))
					sendJoystickInput(bundle.getByte("X"), bundle.getByte("Y"));	
			}
		};

		Looper.loop();
	}

	public static Connections getInstance()
	{
		return instance;
	}

	public void attemptJoystickConnection()
	{
		//joystickConnect();
	}

	private boolean joystickIsConnected()
	{
		return (null != mJoystickSocket);
	}

	public void sendJoystickInput(byte x, byte y)
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
