package Network;
import Utilities.Constants;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkSynchronizer extends Thread{

	public static NetworkListener getNetworkListener() {
		return networkListener;
	}

	private static NetworkListener networkListener;

	public static KeepAlive getKeepAlive() {
		return keepAlive;
	}

	private static ConcurrentLinkedQueue<RepeatingTask> tasks;

	private static KeepAlive keepAlive;

	private static ExecutorService executorService ;

	public static void addToTasks(RepeatingTask task)
	{
		tasks.add(task);
	}

	public static void removeFromTasks(RepeatingTask task)
	{
		tasks.remove(task);//TODO untested
	}

	static {
		tasks =new ConcurrentLinkedQueue<RepeatingTask>();
		executorService = Executors.newFixedThreadPool(10);
	}


	public static  void execute(Runnable command)
	{
		executorService.execute(command);
	}




	public void run()
	{
		networkListener = new NetworkListener();
		networkListener.start();
		tasks.add(new KeepAlive());
		tasks.add(DomainsTree.getDomainsTree());
		while(true)
		{
			RepeatingTask[] r = tasks.toArray(new RepeatingTask[0]);
			for (RepeatingTask task : r)
			{
				if (task.tick(Constants.tickValue))
					executorService.execute(new Runnable() {
						public void run() {
							task.action();
						}
					});
			}
			try
			{
				Thread.sleep(Constants.tickValue);
			} catch (Exception ignored) {
			}
		}
	}

	public interface RepeatingTask
	{
		void action();
		boolean tick(int i);
	}

}
