package teamkipez.jandroid.jandroidclient;

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

public class Connections
{
	private static final Connections instance = new Connections();
	public static String IP = "192.168.12.1";
	public static int PORT = 23;

	private Socket mJoystickSocket = null;
	private DataOutputStream mDos = null;
	private BufferedWriter mBufferedWriter = null;

	public Connections()
	{
		joystickConnect();
	}

	private void joystickConnect()
	{
		new Thread(new Runnable(){
			@Override
			public void run()
			{
				try
				{
					mJoystickSocket = new Socket(IP, PORT);
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
			}
		}).start();
	}

	public void prepareInputSending()
	{
		try
		{
			//mDos = new DataOutputStream(mJoystickSocket.getOutputStream());
			mBufferedWriter = new BufferedWriter(new PrintWriter(mJoystickSocket.getOutputStream()));
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	public static Connections getInstance()
	{
		return instance;
	}

	public void attemptJoystickConnection()
	{
		joystickConnect();
	}

	private boolean joystickIsConnected()
	{
		return (null != mJoystickSocket);
	}

	public void sendJoystickInput(byte x, byte y)
	{
		try
		{
			mBufferedWriter.write(String.format("%04d", x) + ":" + String.format("%04d", y) + "\0");
			Log.i("jandroid", "j'ai envoyé ça pèse sa race " + x + " " + y);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}
