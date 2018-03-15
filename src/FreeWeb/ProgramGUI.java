package FreeWeb;

import java.net.InetSocketAddress;

import DataStructures.DomainSpaceTreeData;
import DataStructures.DomainSpaceTreeInfo;
import DataStructures.DomainTreeNodeInfo;
import DataStructures.DomainTreeNodePosition;
import Database.Database;
import Utilities.*;
import com.sun.net.httpserver.HttpServer;
import BrowserInteraction.BrowserRequestHandler;
import GUI.MainWindow;
import DataStructures.IPPort;
import Network.*;
import Utilities.Constants;

public class ProgramGUI {

	public static void main(String[] args) {
		MainWindow frame;
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
		//Start Main Frame
		frame = new MainWindow();
		try {
			frame.makePacFile();
			frame.loadSettings();
			frame.setVisible(true);
			if (OSValidator.isWindows())
			{
				frame.loadTrayIcon();
				//TODO WinRegistry.writeStringValue(WinRegistry.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings", "AutoConfigURL", (new java.io.File( "." ).getCanonicalPath()) + "\\Proxy.pac");
			}
		} catch (Exception e) {
			System.out.println("Unable to start Main Frame!");
			e.printStackTrace();
			System.exit(-1);
		}
//		test(args);
	}

	public static void test(String[] args)
	{
		IPPort s;

		try {

			byte[] fchash = Utilities.toSHA256("facebook.fw");

			int i1=1;




			byte[] index = new byte[2000];


			DomainSpaceTreeInfo in = new DomainSpaceTreeInfo(new DomainTreeNodePosition(256,Utilities.toSHA256("facebook.fw/index.html")),new DomainSpaceTreeData(index,1));

 			DomainSpaceTree.DomainSpaceTreeSettings sett= new DomainSpaceTree.DomainSpaceTreeSettings(4,60000,32,100000,3000);

 			//DomainSpaceTree.createNewTree(fchash,sett);



			//DomainSpaceTree.getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(fchash)).setInfo(in);

			if ( args.length==3)
			{
				System.out.println("KREATOR DOMENAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
				DomainSpaceTree.createNewTree(fchash,sett);
				DomainSpaceTree.getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(fchash)).setInfo(in);
				return;
			}

			Thread.sleep(30000);

			System.out.println("OSTATAK");
			DomainSpaceTree.getDomainData(Utilities.toSHA256("facebook.fw/index.html"), Utilities.toSHA256("facebook.fw"));

			while (true) {
				Thread.sleep(100000);
				if (i1 == 0)
					break;
			}

			Thread.sleep(5000);
			DomainsTree.setInfo(new DomainTreeNodeInfo(new DomainTreeNodePosition(256, Utilities.toSHA256("facebook.fw")), new IPPort("13.13.13.13", 3347)));


			for (int i = 0; i < 33; i++) {
				DomainsTree.setInfo(new DomainTreeNodeInfo(new DomainTreeNodePosition(256, Utilities.toSHA256("facebook.fw" + i)), new IPPort("13.13.13.13", 3347)));
			}

			DomainsTree.getDomainRoots("facebook.fw");


			Thread.sleep(45 * 1000);


			if (DomainsTree.getMyDomainTreeLevel() == Constants.branchingLevel) {
				DomainsTree.getDomainsInfo().clear();
			}

			Thread.sleep(30 * 1000);

			if (DomainsTree.getMyDomainTreeLevel() == 0) {
				for (int i = 0; i < 33; i++) {
					DomainsTree.setInfo(new DomainTreeNodeInfo(new DomainTreeNodePosition(256, Utilities.toSHA256("facebook.fw" + i)), new IPPort("13.13.13.13", 3347)));
				}
			}


			System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
			for (int i = 0; i < 33; i++) {
				DomainsTree.getDomainRoots("facebook.fw" + i);
			}
			System.out.println("ZZZZZZZZZZAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("ok");
	}
	
}
