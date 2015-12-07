package teamkipez.jandroid.jandroidclient;

import java.net.Socket;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.lang.Thread;
import java.lang.Runnable;
import java.net.UnknownHostException;

public class Connections
{
	private static final Connections instance = new Connections();
	public static String IP = "192.168.12.1";
	public static int PORT = 23;

	private Socket mJoystickSocket = null;
	private BufferedWriter mBufferedWriter = null;

	private Connections()
	{
		joystickConnect();
	}

	private void joystickConnect()
	{
		new Thread(new Runnable()
		{
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

	public static Connections getInstance()
	{
		return instance;
	}

	public void attemptJoystickConnection()
	{
		joystickConnect();
	}

	public boolean joystickIsConnected()
	{
		return (null != mJoystickSocket);
	}

	public void prepareInputSending()
	{
		try
		{
			OutputStream os = mJoystickSocket.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(os);
			mBufferedWriter = new BufferedWriter(osw);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	public void sendJoystickInput(int angle, int strength)
	{
		try
		{
			mBufferedWriter.write(String.format("%04d", angle) + ":" + String.format("%04d", strength) + "\0");
			mBufferedWriter.flush();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}
