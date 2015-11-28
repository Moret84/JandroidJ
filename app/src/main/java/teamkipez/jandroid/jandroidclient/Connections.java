package teamkipez.jandroid.jandroidclient;

import java.net.Socket;
import java.io.IOException;

public class Connections
{
	private static final Connections instance = new Connections();
	public static String IP = "192.168.12.1";
	public static int PORT = 23;
	private Socket mJoystickSocket = null;

	private Connections()
	{
		joystickConnect();
	}

	private void joystickConnect()
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
}
