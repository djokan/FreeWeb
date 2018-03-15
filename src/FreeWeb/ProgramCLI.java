package FreeWeb;

import BrowserInteraction.BrowserRequestHandler;
import Database.Database;
import Network.DomainSpaceTree;
import Network.NetworkSynchronizer;
import Utilities.Constants;
import Utilities.Utilities;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

public class ProgramCLI {
	public static void main(String[] args) throws Exception {

		NetworkSynchronizer s;
		HttpServer server;
		//Start Database
		Database.startServer();
		//Start Browser Handler
		try {
			server = HttpServer.create(new InetSocketAddress(Constants.browserlistenPort), 0);
	        server.createContext("/", new BrowserRequestHandler());
	        server.start();
		} catch (Exception e) {
			System.out.println("Unable to start Browser Request Handler!");
			e.printStackTrace();
		}
		//Start Network Synchronizer
		s = new NetworkSynchronizer();
		s.start();
		//ProgramGUI.test(args);

		DomainSpaceTree.getFileInfo(Utilities.toSHA256("facebook.fw"));

	}

	
}
