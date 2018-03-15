package Database;


import java.io.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import Utilities.Utilities;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.h2.tools.Server;

import com.google.gson.Gson;

import Utilities.Constants;

import javax.sql.rowset.serial.SerialBlob;


public class Database {
	
	private static Server tcpserver=null;
	private static Server webserver=null;
	private static Server pgserver=null;
 	
	public static Connection connectToDatabase(String database,String admin,String password) throws ClassNotFoundException, SQLException
	{
		Connection c = null;
		try{
			Class.forName("org.h2.Driver");
        	c =  DriverManager.getConnection("jdbc:h2:ssl://localhost:" + ((Integer)Constants.sqlServerTcpPort).toString() + "/./" + database + ";" + Constants.databaseConnectionSuffix , admin, password + " " + password);
        	c.setTransactionIsolation(8); // serializable level
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
        return c;
	}
	
	public static Connection connectToWebsite(String website, String user, String password) throws ClassNotFoundException, SQLException
	{
		Connection c = null;
		try{
			Class.forName("org.h2.Driver");
			c =  DriverManager.getConnection("jdbc:h2:ssl://localhost:" + ((Integer)Constants.sqlServerTcpPort).toString() + "/./websites/" + website + "/" + user + Constants.databaseConnectionSuffix, user, password + " " + password); 
			c.setTransactionIsolation(8); // serializable level
		}
		catch(Exception e)
		{
			return null;
		}
        return c;
	}
	
	public static Connection connect(String database, String user, String password) throws ClassNotFoundException, SQLException
	{
		Connection c = null;
		try{
			Class.forName("org.h2.Driver");
        	c =  DriverManager.getConnection("jdbc:h2:ssl://localhost:" + ((Integer)Constants.sqlServerTcpPort).toString() + "/./" + database + Constants.databaseConnectionSuffix, user, password + " " + password); 
        	c.setTransactionIsolation(8); // serializable level
		}
		catch (Exception e)
		{
			return null;
		}
        return c;
	}
	
	public static void startServer() 
	{
		Server tcpserver=null;
		Server webserver=null;
		Server pgserver=null;
		try 
		{
			if (tcpserver==null)
			{
				
				tcpserver = Server.createTcpServer( new String[] { "-tcpPort" , ((Integer)Constants.sqlServerTcpPort).toString() , "-tcpSSL", "-ifExists" }).start();
				
				Database.tcpserver = tcpserver;
			}
			if (webserver==null)
			{
				
				webserver = Server.createWebServer( new String[] { "-webPort" , ((Integer)Constants.sqlServerWebPort).toString(), "-ifExists" }).start();
				
				Database.webserver = webserver;
			}
			if (pgserver==null)
			{
				
				pgserver = Server.createPgServer( new String[] { "-pgPort" , ((Integer)Constants.sqlServerPgPort).toString(), "-ifExists" }).start();
				
				Database.pgserver = pgserver;
			}
		}
		catch (Exception e)
		{
			
		}
	}
	
	public static void stopServer()
	{
		if (tcpserver!=null)
		{
			tcpserver.stop();
		}
		if (webserver!=null)
		{
			webserver.stop();
		}
		if (pgserver!=null)
		{
			pgserver.stop();
		}
	}
	public static void makeNewWebsiteUser(String website,String user,String password) throws Exception
	{
		makeNewDatabase("websites/" + website + "/" + user, user, password);

		Connection c = connectToWebsite(website,user,password);

		Statement stmt = c.createStatement();

		stmt.execute("CREATE TABLE Keys(" +
				"public BLOB," +
				"private BLOB" +
				");");

		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(Constants.PUBLICKEYSIZE, new SecureRandom());
		KeyPair pair= keyGen.genKeyPair();

		Blob b1 = new SerialBlob(Utilities.PublicKeytoByteArray(pair.getPublic()));
		Blob b2 = new SerialBlob(Utilities.PrivateKeytoByteArray(pair.getPrivate()));
		PreparedStatement pstmt = c.prepareStatement("INSERT INTO Keys(public,private) VALUES(?,?)");
		pstmt.setBlob(1,b1);
		pstmt.setBlob(2,b2);
		pstmt.execute();

	}
	
	
	public static void makeNewDatabase(String database,String admin,String password) throws Exception
	{
		
		while (true)
		try
		{
			if (doesDatabaseExist(database)) break;
			Server temptcpserver=null;
			int i = ThreadLocalRandom.current().nextInt(10000, 20000 + 1);
			temptcpserver = Server.createTcpServer( new String[] { "-tcpPort" , ((Integer)i).toString(), "-tcpSSL"}).start();
			
			Class.forName("org.h2.Driver");
			Connection c = DriverManager.getConnection("jdbc:h2:ssl://localhost:"+((Integer)i).toString()+"/./"+database+Constants.databaseConnectionSuffix, admin, password + " " + password); 
			c.close();
			
			temptcpserver.stop();
			break;
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static boolean canWebsiteUserConnect(String website,String user,String password) throws Exception
	{
		if (!doesWebsiteUserDatabaseExist(website,user)) return false;
		try {
			connectToWebsite(website,user,password);
			return true;
		}catch(Exception e)
		{
			//e.printStackTrace();
			return false;
		}
	}
	
	public static boolean doesWebsiteUserDatabaseExist(String website,String user) throws Exception
	{
		return doesDatabaseExist("websites/" + website + "/" + user);
	}
	
	public static boolean doesDatabaseExist(String database)
	{
		return Utilities.doesFileExist(database+".mv.db");
	}
	
	public static String getResultOfQuery(Connection connection, String query) throws RuntimeException {
        List<Map<String, Object>> listOfMaps = null;
        try {
            QueryRunner queryRunner = new QueryRunner();
            listOfMaps = queryRunner.query(connection, query, new MapListHandler());
        } catch (SQLException se) {
            throw new RuntimeException("Couldn't query the database.", se);
        } finally {
            DbUtils.closeQuietly(connection);
        }
        return new Gson().toJson(listOfMaps);
    }
	
	public static String getResultOfQuery(String website,String user,String password, String query) throws ClassNotFoundException, SQLException
	{
		
		return getResultOfQuery(connectToWebsite(website,user,password), query);
	}
	
	
	public static void executeQuery(String website,String user,String password, String query) throws SQLException, ClassNotFoundException
	{
		Statement stat;
		stat = connectToWebsite(website,user,password).createStatement();
		stat.execute(query);
	}
	
	public static void executeQuery(Connection conn, String query) throws SQLException
	{
		Statement stat;
		stat = conn.createStatement();
		stat.execute(query);
	}
	
	
	/*public static void main(String argv[]) throws Exception
	{
		try{
			Database.startServer();
		}catch (Exception e){}
		
		
		makeNewDatabase("websites/facebook/aeae","aeae","aeae");
		
		Connection conn = Database.connectToWebsite("facebook","aeae","aeae");
		Statement stat = conn.createStatement();
		stat.execute("drop table test if exists");
		conn.close();
		TestWorker t[]= new TestWorker[1000];
		for (int i=0;i<1;i++)
		{
			t[i] = new TestWorker(i);
			
			t[i].start();
			
		}
		
		
		for (int i=0;i<1;i++)
		{
			t[i].join();
		}
		System.out.println("stvarno kraj");
        Thread.sleep(300000);
        try
        {
        	stopServer();
		}
        catch (Exception e){}
	}*/

	public static void setInitialFreeWebData() throws Exception {

		makeNewDatabase("freewebdata", Constants.defaultAdmin, Constants.defaultPassword);
		Connection conn = null;
		for (int i=0;i<10;i++)
		try
		{
			conn = Database.connectToDatabase("freewebdata",Constants.defaultAdmin,Constants.defaultPassword);
			break;
		}
		catch (Exception e)
		{
			
		}
		Database.executeQuery(conn, "CREATE TABLE IF NOT EXISTS `Peers` (" 
									+"`ip` varchar(15)," 
									+"`port` int," 
									+"`time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP," 
									+"PRIMARY KEY(`ip`, `port`, `time`)"
									+");");
		for (int i=0;i<Constants.numberOfCachedPeers;i++)
		{
			while (true)
				try
				{
					if (i<Utilities.min(Constants.initIps.length,Constants.initPorts.length))
					{
						Database.executeQuery(conn,"insert into Peers(ip,port) values('" + Constants.initIps[i%Utilities.min(Constants.initIps.length,Constants.initPorts.length)] + "', " + Constants.initPorts[i%Utilities.min(Constants.initIps.length,Constants.initPorts.length)] + ")");
					}
					else
					{
						Database.executeQuery(conn,"insert into Peers(ip,port) values('255.0.0." + i + "', " + Constants.networklistenPort + " )");
					}
					break;
				}
				catch(Exception e)
				{
					Thread.sleep(100);
				}
		}
		
	}


	public static void startServerOnRandomPort() {
		Server tcpserver=null;
		Server webserver=null;
		Server pgserver=null;
		try
		{
			if (tcpserver==null)
			while (true)
				try
				{
					int i = ThreadLocalRandom.current().nextInt(10000, 20000 + 1);

					tcpserver = Server.createTcpServer( new String[] { "-tcpPort" , ((Integer)i).toString() , "-tcpSSL", "-ifExists" }).start();

					Constants.sqlServerTcpPort = i;

					Database.tcpserver = tcpserver;

					break;
				}
				catch (Exception e)
				{}

			if (webserver==null)
			{
				int i = ThreadLocalRandom.current().nextInt(10000, 20000 + 1);
				webserver = Server.createWebServer( new String[] { "-webPort" , ((Integer)i).toString(), "-ifExists" }).start();
				Constants.sqlServerWebPort = i;
				Database.webserver = webserver;
			}
			if (pgserver==null)
			{
				int i = ThreadLocalRandom.current().nextInt(10000, 20000 + 1);

				pgserver = Server.createPgServer( new String[] { "-pgPort" , ((Integer)i).toString(), "-ifExists" }).start();

				Constants.sqlServerPgPort = i;

				Database.pgserver = pgserver;
			}
		}
		catch (Exception e)
		{

		}
	}
}


//
//
//class TestWorker extends Thread
//{
//	private int i=0;
//	public TestWorker(int s)
//	{
//		i = s;
//	}
//
//	public void run()
//    {
//
//    	Statement stat;
//		try {
//			Connection conn = Database.connectToWebsite("facebook","aeae","aeae");
//			stat = conn.createStatement();
//			stat.execute("create table if not exists test(id int , name varchar(255))");
//	        stat.execute("insert into test values(1, 'Hello')");
//	        System.out.println("op " + i);
//
//	        Database.executeQuery(conn,"insert into test values(1, 'Hello')");
//	        Database.executeQuery(conn,"insert into test values(1, 'Hello')");
//	        Database.executeQuery(conn,"insert into test values(1, 'Hello')");
//	        Database.executeQuery(conn,"insert into test values(1, 'Hello')");
//	        Database.executeQuery(conn,"insert into test values(1, 'Hello')");
//
//	        System.out.println("kraj " + i);
//	        conn.close();
//		} catch (Exception e) {
//			try {
//				PrintStream  p = new PrintStream (new File("log.txt"));
//				e.printStackTrace(p);
//			} catch (FileNotFoundException e1) {
//
//				e1.printStackTrace();
//			}
//			e.printStackTrace();
//		}
//
//    }
//}