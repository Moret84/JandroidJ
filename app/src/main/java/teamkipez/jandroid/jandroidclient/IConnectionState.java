package teamkipez.jandroid.jandroidclient;

public interface IConnectionState
{
	public void onConnectionSuccess();
	public void onConnectionFail();
	public void alreadyConnected();
}
