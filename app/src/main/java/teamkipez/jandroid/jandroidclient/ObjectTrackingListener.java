package teamkipez.jandroid.jandroidclient;

public interface ObjectTrackingListener
{
	public void onNewObjectPosition(double x, double y);
	public void onObjectDisappeared();
}
