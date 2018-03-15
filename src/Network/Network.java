package Network;

import java.io.*;
import java.security.KeyPair;
import java.security.PublicKey;
import java.sql.*;
import java.util.Arrays;
import java.util.List;

import BrowserInteraction.Response;
import DataStructures.DomainSpaceTreeData;
import DataStructures.DomainSpaceTreeInfo;
import DataStructures.DomainTreeNodePosition;
import Database.Database;
import DataStructures.IPPort;
import Utilities.*;

import javax.crypto.Cipher;
import javax.xml.bind.DatatypeConverter;

public class Network {
	public static Response post(String url, String host, String data) {
		String html = "<hmtl><head></head><body>dede</body></html>";
		Response a = new Response();
		a.setResponse((html).getBytes());
		String[] s = {"Content-type","text/html"};
		a.setHeaderAttributes(s);
		return a; //TODO blank method
	}
	
	public static Response get(String url, String host) throws FileNotFoundException {
		String link = url.replaceAll("http://" + host + "/", "");
		byte[] b=new byte[4096];
		int size = 0;

		b = DomainSpaceTree.getDomainFile(link, host);

		Response a = new Response();
		if (b==null)
		{
			b = "Could't find file. Please try again later!".getBytes();
		}

		System.out.println(b.length);

		a.setResponse(b);
		if (url.contains("ico"))
		{
			String[] s = {"Content-type","image/jpeg"};
			a.setHeaderAttributes(s);
		}
		else {
			String[] s = {"Content-type","text/html"};
			a.setHeaderAttributes(s);
		}



		return a; //TODO blank method
	}
	
	public static Response database(String url, String host, String data) throws Exception {
		if (!(Utilities.doesHttpDataExist(data, "user") && Utilities.doesHttpDataExist(data, "pass") && Utilities.doesHttpDataExist(data, "sql") && Utilities.doesHttpDataExist(data, "type") ))
				throw new Exception("Error! Passed arguments of database request are not valid");
		
		String user = Utilities.getValueOfHttpData(data, "user");
		String pass = Utilities.getValueOfHttpData(data, "pass");
		String sql = Utilities.getValueOfHttpData(data, "sql");
		String type = Utilities.getValueOfHttpData(data, "type");
		Response a = new Response();
		Connection c = null;
		try{
			if (!Database.doesWebsiteUserDatabaseExist(host, user))
			{
				if (type.equals("create database"))
				{
					Database.makeNewWebsiteUser(host, user, pass);
					a.setResponse("[[success]]".getBytes());
					a.setHeaderAttributes(new String[]{"Content-type","text/html"});
					a.setStatusCode(200);
					return a;
				}
				else
				{
					a.setResponse("[[error]] Database doesn't exist".getBytes());
					a.setHeaderAttributes(new String[]{"Content-type","text/html"});
					a.setStatusCode(403);
					return a;
				}
			}
			else
			{
				if (type.equals("create database"))
				{
					a.setResponse("[[error]] Database already exists".getBytes());
					a.setHeaderAttributes(new String[]{"Content-type","text/html"});
					a.setStatusCode(403);
					return a;
				}
			}
			c = Database.connectToWebsite(host, user, pass);
			
			if (c==null)
			{
				a.setResponse("[[error]] Invalid username/password combination".getBytes());
				a.setHeaderAttributes(new String[]{"Content-type","text/html"});
				a.setStatusCode(403);
				return a;
			}
			if (type.equals("query"))
			{
				try {
					String s = Database.getResultOfQuery(c, sql);
					a.setResponse(("[[success]]"+s).getBytes());
					a.setStatusCode(200);
				}
				catch (Exception e)
				{
					a.setResponse(("[[error]] "+e.getMessage()).getBytes());
					a.setStatusCode(403);
				}
				
			}
			if (type.equals("execute"))
			{
				try{
					Database.executeQuery(c, sql);
					a.setResponse("[[success]]".getBytes());
					a.setStatusCode(200);
				}
				catch (Exception e)
				{
					a.setResponse(("[[error]] "+e.getMessage()).getBytes());
					a.setStatusCode(403);
				}
			}
			c.close(); // TODO this can be optimized by saving active connections to hash table, but must there can be a problem with 2 different threads doing transactions at same time
		}
		catch (Exception e)
		{
			a.setResponse(("[[error]] unknown error: "+e.getMessage()).getBytes());
			a.setHeaderAttributes(new String[]{"Content-type","text/html"});
			a.setStatusCode(403);
			e.printStackTrace();
			return a;
		}
		String[] s = {"Content-type","text/html"};
		a.setHeaderAttributes(s);
		return a; 
	}

	private static void didntConnect(Statement stmt, String ip, int port, String t)
	{
		try
		{
			stmt.execute("UPDATE Peers SET time= TIMESTAMPADD(MINUTE, -1,(SELECT MIN(time) FROM Peers))  WHERE ip='" + ip + "' AND port = " + port + " AND time = '" + t + "'");
		}
		catch(Exception e1)
		{
			e1.printStackTrace();
		}
	}

	public static IPPort getRandomUser() throws Exception //TODO this function can find N more users when found one to be sure that we are connected to network and choose 1 of them
	{
		if (!Database.doesDatabaseExist("freewebdata"))
		{
			Database.setInitialFreeWebData();
		}
		
		Statement stmt = Database.connectToDatabase("freewebdata", Constants.defaultAdmin, Constants.defaultPassword).createStatement();
		while (true)
		{
			ResultSet rs = stmt.executeQuery("SELECT * FROM Peers ORDER BY time DESC LIMIT 1");
			if (rs.next())
			{
				String ip = rs.getString(1);
				int port = rs.getInt(2);
				String t= rs.getString(3);
				if (ip.substring(0, 4).equals("255."))
				{
					try
					{
						stmt.execute("UPDATE Peers SET time= TIMESTAMPADD(MINUTE, -1,(SELECT MIN(time) FROM Peers))  WHERE ip='" + ip + "' AND port = " + port + " AND time = '" + t + "'");
					}
					catch(Exception e1)
					{
						e1.printStackTrace();
					}
					continue;
				}

				try
				{
					IPPort user = new IPPort(ip,port);
					if (FreeWebUser.connect(user))
						return user;
					else
						didntConnect(stmt,ip,port,t);
				}
				catch (Exception e)
				{
					didntConnect(stmt,ip,port,t);
				}
			}
		}
	}


	public static Response save(String url, String host, String data) throws Exception {
		if (!(Utilities.doesHttpDataExist(data, "user") && Utilities.doesHttpDataExist(data, "pass") ))
			throw new Exception("Error! Passed arguments of database request are not valid");

		String user = Utilities.getValueOfHttpData(data, "user");
		String pass = Utilities.getValueOfHttpData(data, "pass");

		Response a = new Response();

		KeyPair kp = getKeyPair(url,host,data,a);
		if (kp==null)
		{
			return a;
		}

		byte[] database = Utilities.readAllBytes("websites/" + host + "/" + user +".mv.db");

		List<DomainSpaceTreeInfo> infos = Utilities.parsePrivateFile(database, kp, DomainSpaceTree.getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(Utilities.toSHA256(host))).getSettings().getMaxSingleDataSize());

		DomainSpaceTree.getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(Utilities.toSHA256(host))).advertise(infos);

		a.setResponse("[[success]]".getBytes());
		a.setHeaderAttributes(new String[]{"Content-type","text/html"});
		a.setStatusCode(200);
		return a;

	}

	public static Response share(String url, String host, String httpdata) throws Exception {
		if (!(Utilities.doesHttpDataExist(httpdata, "user") && Utilities.doesHttpDataExist(httpdata, "pass") && Utilities.doesHttpDataExist(httpdata, "path") && Utilities.doesHttpDataExist(httpdata, "data")  ))
			throw new Exception("Error! Passed arguments of database request are not valid");

		String path = Utilities.getValueOfHttpData(httpdata, "path");
		String data = Utilities.getValueOfHttpData(httpdata, "data");

		byte[] decodeddata = DatatypeConverter.parseBase64Binary(data);

		byte[] decodedpath = DatatypeConverter.parseBase64Binary(path);
		decodedpath = Arrays.copyOfRange(decodedpath,0,Constants.hashSize);

		if (decodedpath.length!=Constants.hashSize)
		{
			return null;
		}


		Response a = new Response();

		KeyPair kp = getKeyPair(url,host,httpdata,a);
		if (kp==null)
		{
			return a;
		}
		try {
			decodeddata = Utilities.add(Utilities.cryptWithKey(decodeddata, kp.getPrivate(), Cipher.ENCRYPT_MODE), Utilities.PublicKeytoByteArray(kp.getPublic()));

			System.out.println(decodeddata.length);
			DomainTreeNodePosition dtnp = new DomainTreeNodePosition(Constants.hashSize * 8, decodedpath);
			DomainSpaceTreeData dstd = new DomainSpaceTreeData(decodeddata, 2);
			DomainSpaceTreeInfo dsti = new DomainSpaceTreeInfo(dtnp, dstd);

			DomainSpaceTree.getDomainSpaceTree(host).advertise(dsti);


			return a;
		}catch (Exception e)
		{
			a = new Response();
			a.setResponse(("[[error]] unknown error: ").getBytes());
			a.setHeaderAttributes(new String[]{"Content-type", "text/html"});
			a.setStatusCode(403);
			return a;
		}
	}

	public static Response getshared(String url, String host, String httpdata) throws Exception {
		if (!(Utilities.doesHttpDataExist(httpdata, "user") && Utilities.doesHttpDataExist(httpdata, "pass") && Utilities.doesHttpDataExist(httpdata, "path") && Utilities.doesHttpDataExist(httpdata, "data")  ))
			throw new Exception("Error! Passed arguments of database request are not valid");

		try {


			String user = Utilities.getValueOfHttpData(httpdata, "user");
			String pass = Utilities.getValueOfHttpData(httpdata, "pass");
			String path = Utilities.getValueOfHttpData(httpdata, "path");

			byte[] decodedpath = DatatypeConverter.parseBase64Binary(path);

			decodedpath = Arrays.copyOfRange(decodedpath,0,Constants.hashSize);

			Response a = new Response();

			KeyPair kp = getKeyPair(url, host, httpdata, a);

			if (kp==null)
			{
				return a;
			}

			byte[] dti = DomainSpaceTree.getDomainData(decodedpath, Utilities.toSHA256(host));

			String data = DatatypeConverter.printBase64Binary(dti);
			a.setResponse(("[[success]] " + data).getBytes());
			a.setHeaderAttributes(new String[]{"Content-type","text/html"});
			a.setStatusCode(200);
			return a;
		}catch (Exception e)
		{
			Response a = new Response();
			a.setResponse(("[[error]] unknown error: ").getBytes());
			a.setHeaderAttributes(new String[]{"Content-type", "text/html"});
			a.setStatusCode(403);
			return a;
		}
	}

	public static Response getsaved(String url, String host, String httpdata) throws Exception {
		if (!(Utilities.doesHttpDataExist(httpdata, "user") && Utilities.doesHttpDataExist(httpdata, "pass") && Utilities.doesHttpDataExist(httpdata, "path") && Utilities.doesHttpDataExist(httpdata, "data")  ))
			throw new Exception("Error! Passed arguments of database request are not valid");

		try {
			String user = Utilities.getValueOfHttpData(httpdata, "user");
			String path = Utilities.getValueOfHttpData(httpdata, "public");

			byte[] decodedpublic = DatatypeConverter.parseBase64Binary(path);

			PublicKey pk = Utilities.ByteArraytoPublicKey(decodedpublic);

			Response a = new Response();

			byte[] dti = DomainSpaceTree.getDomainSpaceTree(host).getDomainData(Utilities.toSHA256(decodedpublic), Utilities.toSHA256(host));

			byte[] coded = Utilities.cryptWithKey(dti,pk,Cipher.DECRYPT_MODE);

			byte[] hash = Arrays.copyOfRange(coded,0,Constants.hashSize);

			byte[] file = DomainSpaceTree.getPrivateFile(hash,Utilities.toSHA256(host));

			FileOutputStream fos = new FileOutputStream("websites/" + host + "/" + user +".mv.db");

			fos.write(file);

			fos.close();

			String data = DatatypeConverter.printBase64Binary(dti);
			a.setResponse(("[[success]] " + data).getBytes());
			a.setHeaderAttributes(new String[]{"Content-type","text/html"});
			a.setStatusCode(200);
			return a;
		}catch (Exception e)
		{
			Response a = new Response();
			a.setResponse(("[[error]] unknown error: ").getBytes());
			a.setHeaderAttributes(new String[]{"Content-type", "text/html"});
			a.setStatusCode(403);
			return a;
		}
	}

	private static KeyPair getKeyPair(String url, String host, String httpdata, Response a) throws UnsupportedEncodingException {
		String user = Utilities.getValueOfHttpData(httpdata, "user");
		String pass = Utilities.getValueOfHttpData(httpdata, "pass");
		Connection c = null;
		try {
			if (!Database.doesWebsiteUserDatabaseExist(host, user)) {
				a.setResponse("[[error]] Database doesn't exist".getBytes());
				a.setHeaderAttributes(new String[]{"Content-type", "text/html"});
				a.setStatusCode(403);
				return null;
			} else {
				byte[] database = Utilities.readAllBytes("websites/" + host + "/" + user +".mv.db");

				if (database==null || database.length==0)
					throw new Exception();

				c = Database.connectToWebsite(host, user, pass);

				try {


					if (c == null) {
						a.setResponse("[[error]] Invalid username/password combination".getBytes());
						a.setHeaderAttributes(new String[]{"Content-type", "text/html"});
						a.setStatusCode(403);
						return null;
					}

					Statement stat = c.createStatement();

					ResultSet rs = stat.executeQuery("SELECT private,public FROM Keys");

					if (rs.next()) {
						Blob priv = rs.getBlob(1);
						Blob pub = rs.getBlob(2);

						KeyPair kp = new KeyPair(Utilities.ByteArraytoPublicKey(Utilities.blobToByteArray(pub)), Utilities.ByteArraytoPrivateKey(Utilities.blobToByteArray(priv)));



						return kp;

					} else {
						a.setResponse(("[[error]] unknown error: ").getBytes());
						a.setHeaderAttributes(new String[]{"Content-type", "text/html"});
						a.setStatusCode(403);
						return null;
					}

				}catch (Exception e)
				{
					a.setResponse(("[[error]] unknown error: ").getBytes());
					a.setHeaderAttributes(new String[]{"Content-type", "text/html"});
					a.setStatusCode(403);
					return null;
				}
				finally {
					c.close();
				}

			}
		}catch (Exception e)
		{
			a.setResponse(("[[error]] unknown error: "+e.getMessage()).getBytes());
			a.setHeaderAttributes(new String[]{"Content-type","text/html"});
			a.setStatusCode(403);
			e.printStackTrace();
			return null;
		}
	}
}


