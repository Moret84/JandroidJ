package teamkipez.jandroid.jandroidclient;

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
	public static final int CONNECT = 3;
	public static final String HEADER = "HEADER";
	public static final String X = "X";
	public static final String Y = "Y";
	public static final byte PING = 'O';

	public Handler handler;

	private Handler mUIHandler;

	private Socket mJoystickSocket = null;
	private DataOutputStream mDos = null;
	private IConnectionState mConnectionState;

	private Connections()
	{
		mUIHandler = new Handler(Looper.getMainLooper());
	}

	public void setConnectionStateListener(IConnectionState connectionStateListener)
	{
		mConnectionState = connectionStateListener;
	}

	@Override
	public void run()
	{
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
					case(CONNECT):
						connect();
						break;
				}
			}
		};

		connect();

		Looper.loop();
	}

	public static Connections getInstance()
	{
		return instance;
	}

	private boolean isConnected()
	{
		return (null != mJoystickSocket) && (null != mDos);
	}

	private void sendJoystickInput(byte header, byte x, byte y)
	{
		try
		{
			if(isConnected())
			{
				mDos.writeByte(header);
				mDos.writeByte(x);
				mDos.writeByte(y);
				Log.i("jandroid, " + header, "j'ai envoyé ça pèse sa race " + x + " " + y);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
			mUIHandler.post(new Runnable()
					{
						@Override
						public void run()
						{
							mConnectionState.onConnectionFail();
						}
					});
		}
	}

	private void connect()
	{
		try
		{
			if(null == mJoystickSocket)
				mJoystickSocket = new Socket(IP, PORT);
			mDos = new DataOutputStream(mJoystickSocket.getOutputStream());
			mUIHandler.post(new Runnable()
					{
						@Override
						public void run()
						{
							mConnectionState.onConnectionSuccess();
						}
					});
		}
		catch(IOException e)
		{
			e.printStackTrace();
			mUIHandler.post(new Runnable()
					{
						@Override
						public void run()
						{
							mConnectionState.onConnectionFail();
						}
					});
		}
	}
}
