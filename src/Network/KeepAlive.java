package Network;

import DataStructures.IPPort;
import Utilities.*;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class KeepAlive implements NetworkSynchronizer.RepeatingTask {


	private static ConcurrentLinkedQueue<IPPort> keepAliveQueue;
	
	static
	{
		keepAliveQueue = new ConcurrentLinkedQueue<IPPort>();
	}
	
	public static void addToKeepAliveQueue(IPPort user)
	{
		keepAliveQueue.add(user);
	}

	private static int tickval=0;

	public static ConcurrentLinkedQueue<IPPort> getKeepAliveQueue() {
		return keepAliveQueue;
	}


	public void action() {

		int size = keepAliveQueue.size();
		IPPort[] users = keepAliveQueue.toArray(new IPPort[0]);

		if (DomainSpaceTree.getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(Utilities.toSHA256("facebook.fw")))!=null)
			DomainSpaceTree.getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(Utilities.toSHA256("facebook.fw"))).printNodeInfo();
		for (IPPort user : users)
		{
			try
			{
				if (user==null)
				{
					user = new IPPort(Constants.randomIp,ThreadLocalRandom.current().nextInt(10000, 20000 + 1));
				}
				//System.out.println("keepalive to " + user.toString());
				NetworkListener.getServerSocket().send(Utilities.createDatagramPacket(Utilities.intToByteArray(MessageParser.KEEPALIVE),user));
			}
			catch(Exception e)
			{

			}
			if (keepAliveQueue.size()> Constants.keepAliveQueueSize)
			{
				keepAliveQueue.poll();
			}
		}
	}

	@Override
	public boolean tick(int i) {
		tickval-=i;
		if (tickval<=0) {
			tickval=5000;
			return true;
		}
		return false;
	}
}
