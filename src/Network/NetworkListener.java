package Network;

import java.net.*;

import Utilities.*;

public class NetworkListener extends Thread {

	private static String requestType;
	private static DatagramSocket serverSocket;

	public void run()
	{
	    while (true) {
            try {
                serverSocket = new DatagramSocket(Constants.networklistenPort);
                serverSocket.setSoTimeout(5000);
                while (true)
                {
                    DatagramPacket d;
                    byte[] receiveData = new byte[1024];
                    d = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(d);
                    NetworkSynchronizer.execute(new Runnable() {
                        public void run() {
                            MessageParser.parse(d);
                        }
                    });

                }

            } catch (Exception e) {
                try {
                    serverSocket.close();
                }
                catch (Exception e1)
                {

                }
            }

        }
	}


	public static DatagramSocket getServerSocket() {
		return serverSocket;
	}

	
}
