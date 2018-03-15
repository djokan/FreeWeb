package Utilities;

import java.awt.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.*;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.sql.rowset.serial.SerialBlob;
import javax.swing.*;

import DataStructures.*;
import FreeWeb.CreatorCLI;
import Network.DomainSpaceTree;
import Network.MessageParser;
import Network.NetworkListener;
import DataStructures.IPPort;
import org.apache.commons.io.IOUtils;

import java.security.interfaces.RSAPrivateKey;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


public class Utilities {

	public static boolean isExternal(IPPort a)
	{
		int firstByte = Integer.parseInt( a.getIp().substring(0, a.getIp().indexOf('.')));
		return !(firstByte > 191 && firstByte < 224);
	}
	
	public static byte[] add(byte[] a, byte[] b)
	{
		byte[] c = new byte[a.length+b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}
	
	public static boolean doesFileExist(String file)
	{
		File f = new File(file);
		return f.exists() && !f.isDirectory();
	}
	
	public static String convertStreamToString(java.io.InputStream is) {
		java.util.Scanner sd = new java.util.Scanner(is);
	    java.util.Scanner s = sd.useDelimiter("\\A");
	    String s1 = s.hasNext() ? s.next() : "";
	    sd.close();
	    return s1;
	}

	public static byte[] PublicKeytoByteArray(PublicKey p)
	{
		return p.getEncoded();

	}

	public static byte[] PrivateKeytoByteArray(PrivateKey p)
	{
		return p.getEncoded();
		
	}

	public static String getValueOfHttpData(String data,String name) throws UnsupportedEncodingException
	{
		String encoded= data.substring(data.indexOf(name+"=")+(name.length()+1));
		encoded =  encoded + "&";
		encoded= encoded.substring(0, encoded.indexOf("&")); 
		return java.net.URLDecoder.decode(encoded,"UTF-8");
	}
	
	public static boolean doesHttpDataExist(String data,String name)
	{
		if (data.indexOf(name+"=")<0) return false;
		return true;
	}
	
	static public void test(KeyPair k)
	{
		String s="test";
		try {
		Cipher c1 = Cipher.getInstance("RSA");
		c1.init(Cipher.ENCRYPT_MODE, k.getPrivate());
		Cipher c2 = Cipher.getInstance("RSA");
		c2.init(Cipher.ENCRYPT_MODE, k.getPublic());
		byte[] chipertext=c1.doFinal(s.getBytes());
		System.out.println("private:");
		System.out.println(new String(chipertext));
		byte[] plaintext=c2.doFinal(s.getBytes());
		System.out.println("public:");
		System.out.println(new String(plaintext));
		}catch (Exception e){e.printStackTrace();};
		
	}

	public static List<String> listAllFiles()
	{
		List<String> list = new LinkedList<String >();
		listf(Paths.get("").toAbsolutePath().toString(),list,Paths.get("").toAbsolutePath().toString()+"\\");
		return list;
	}

	public static byte[] readAllBytes(String s)
	{
		try {
			return IOUtils.toByteArray(new FileInputStream(s));
		}
		catch (Exception e)
		{
			return new byte[0];
		}
	}


	private static void listf(String directoryName, List<String> files, String abs) {
		File directory = new File(directoryName);

		String me = new java.io.File(CreatorCLI.class.getProtectionDomain()
				.getCodeSource()
				.getLocation()
				.getPath())
				.getName();
		// get all the files from a directory
		File[] fList = directory.listFiles();
		for (File file : fList) {
			if (file.isFile()) {
				if (!file.getName().equals("freewebdata.mv.db") && !file.getName().equals(me))
					files.add(file.getPath().replace(abs,""));
			} else if (file.isDirectory()) {
				listf(file.getAbsolutePath(), files,abs);
			}
		}
	}

	public static byte[] cryptWithKey(byte[] source,Key key, int mode)
	{
		byte[] ret = null;
		try {
			Cipher c1 = Cipher.getInstance("RSA");
			c1.init(mode, key);
			ret = c1.doFinal(source);
		}catch (Exception e)
		{e.printStackTrace();}
		return ret;
	}

	public static boolean isValid(KeyPair k)
	{
		String s="test";
		try {
		Cipher c1 = Cipher.getInstance("RSA");
		c1.init(Cipher.ENCRYPT_MODE, k.getPrivate());
		Cipher c2 = Cipher.getInstance("RSA");
		c2.init(Cipher.DECRYPT_MODE, k.getPublic());
		byte[] chipertext=c1.doFinal(s.getBytes());
		byte[] plaintext=c2.doFinal(chipertext);
		if (new String(plaintext).equals("test")) return true;
		}catch(Exception e) {}
		return false;
	}
	
	public static PrivateKey ByteArraytoPrivateKey(byte[] b) throws InvalidKeySpecException, NoSuchAlgorithmException
	{
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(b);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		return keyFactory.generatePrivate(keySpec);
	}

	public static PublicKey ByteArraytoPublicKey(byte[] b) throws InvalidKeySpecException, NoSuchAlgorithmException
	{
		return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(b));
	}


	public static SecretKey StringtoSecretKey(char[] s) throws NoSuchAlgorithmException
	{
		
		byte[] decodedKey = Charset.forName("UTF-8").encode(CharBuffer.wrap(s)).array();
		MessageDigest sha = MessageDigest.getInstance("SHA-1");
		decodedKey = sha.digest(decodedKey);
		decodedKey = Arrays.copyOf(decodedKey, 16);
		SecretKey sec=new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES"); 
		for (int i=0;i<decodedKey.length;i++)
		decodedKey[i]=0x00;
		return sec;
	}
	
	public static byte[] AesEncrypt(byte[] plaintext,char[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
	{	
		Cipher c1 = Cipher.getInstance("AES");
		SecretKey s=StringtoSecretKey(key);
		c1.init(Cipher.ENCRYPT_MODE, s);
		byte[] chipertext=c1.doFinal(plaintext);
		try{
			//s.destroy();
		}catch(Exception ignored){}
		return chipertext;
	}

	public static CipherOutputStream AesEncryptFile(FileOutputStream file,char[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
	{	
		Cipher c1 = Cipher.getInstance("AES");
		SecretKey s=StringtoSecretKey(key);
		c1.init(Cipher.ENCRYPT_MODE, s);
		return new CipherOutputStream(file, c1);
	}

	public static PrivateKey DecryptPrivateKey(PrivateKey enc,char[] password) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException
	{
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(new RSAPrivateKeySpec(   new BigInteger(Utilities.AesDecrypt(((RSAPrivateKey)enc).getModulus().toByteArray(),password)),((RSAPrivateKey)enc).getPrivateExponent()));
	}

	public static PrivateKey EncryptPrivateKey(PrivateKey p,char[] password) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException
	{
		KeyFactory kf = KeyFactory.getInstance("RSA");
		byte[] cryptedmodulus=Utilities.AesEncrypt(((RSAPrivateKey)p).getModulus().toByteArray(),password);
		return kf.generatePrivate(new RSAPrivateKeySpec(   new BigInteger(cryptedmodulus),((RSAPrivateKey)p).getPrivateExponent()));
		
	}
	
	public static CipherInputStream AesDecryptFile(FileInputStream file,char[] key) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException
	{
		Cipher c1 = Cipher.getInstance("AES");
		SecretKey s=StringtoSecretKey(key);
		c1.init(Cipher.DECRYPT_MODE, s);
		return new CipherInputStream(file,c1);
	}

	public static byte[] AesDecrypt(byte[] chipertext,char[] key) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException
	{
		Cipher c1 = Cipher.getInstance("AES");
		SecretKey s=StringtoSecretKey(key);
		c1.init(Cipher.DECRYPT_MODE, s);
		byte[] plaintext;
		if (chipertext.length%16==1) plaintext=c1.doFinal(Arrays.copyOfRange(chipertext,1,chipertext.length));
		else {
			plaintext=c1.doFinal(chipertext);
			
		}
		try{
			//s.destroy();
		}catch(Exception ignored){}
		return plaintext;
	}
	
	public static void centerScreen(JFrame a)
	{
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		a.setLocation(dim.width/2-a.getWidth()/2, dim.height/2-a.getHeight()/2);
		
	}
	
	public static void centerScreen(JDialog a)
	{
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		a.setLocation(dim.width/2-a.getWidth()/2, dim.height/2-a.getHeight()/2);
		
	}

	public static void changeSpaceSize(long size) {
		File f = new File("space.fwf");
		f.delete(); 
	}

	public static void createSpace(long size) throws FileNotFoundException  {
		FileOutputStream fout = new FileOutputStream("space.fwf");
		int i;
		byte[] b= new byte[1000000];
		byte[] a= new byte[1];
		try{
			for ( i=0;i<size/1000000;i++)
			{
				fout.write(b);
			}
			for ( i=0;i<size%1000000;i++)
			{
				fout.write(a);
			}
			fout.close();
		}
		catch (Exception e)
		{
			try {
				fout.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			File f= new File ("space.fwf");
			f.delete();
		}
	}
	
	public static String getString(byte[] b, int start, int end)
	{
		return new String(Arrays.copyOfRange(b, start, end));
	}
	
	public static void updateSpace(long size) throws FileNotFoundException {
		//System.out.println(size);
		boolean temp = false;
		File space = new File("space.fwf");

		if(space.exists() && !space.isDirectory()) {
			if (space.length()!=size)
			{
				Utilities.changeSpaceSize(size);
				temp = true;
				//System.out.println("promenjena velicina");
			}
		}

		space = new File("space.fwf");

		if(!space.exists() || space.isDirectory() || temp) {
			Utilities.createSpace(size);
		}
		
	}

	public static int min(int a, int b) {
		return a<b?a:b;
	}
	
	public static byte[] IPPortToByteArray(IPPort i)
	{
        ByteBuffer bytes = ByteBuffer.allocate(Constants.IPPortSize);
        String ip = i.getIp();
        String temp = ip.substring(0, ip.indexOf('.'));
        ip = ip.substring(ip.indexOf('.')+1);
        bytes.put((byte)Integer.parseInt(temp));
        temp = ip.substring(0, ip.indexOf('.'));
        ip = ip.substring(ip.indexOf('.')+1);
        bytes.put((byte)Integer.parseInt(temp));
        temp = ip.substring(0, ip.indexOf('.'));
        ip = ip.substring(ip.indexOf('.')+1);
        bytes.put((byte)Integer.parseInt(temp));
        bytes.put((byte)Integer.parseInt(ip));
        int port = i.getPort();
        bytes.putInt(port);
		return bytes.array();
	}
	
	public static IPPort byteArrayToIPPort(byte[] a) {
	    if (a == null || a.length<Constants.IPPortSize) return null;
        ByteBuffer b = ByteBuffer.wrap(a);
        IPPort ipp;
        int b1= b.get() & 0xFF;
        int b2= b.get() & 0xFF;
        int b3= b.get() & 0xFF;
        int b4= b.get() & 0xFF;
        int port= b.getInt();
        String ip = b1 + "." + b2 + "." + b3 + "." + b4;
        return new IPPort(ip,port);
    }

	public static IPPort[] resultSetToIPPortArray(ResultSet rs)
    {
        int iter=0;
        IPPort[] i= null;
        try {
            rs.last();

            i = new IPPort[rs.getRow()];
            rs.beforeFirst();
            while (rs.next() && iter < Constants.numberOfCachedPeers) {
                String ip = rs.getString(1);

                int port = rs.getInt(2);
                i[iter] = new IPPort(ip, port);
                iter++;
            }
        } catch (SQLException e) {
            return null;
        }
        return i;
    }
	
	public static byte[] IPPortArrayToByteArray(IPPort[] o)
	{
		if (o == null || o.length == 0) return new byte[0];
        ByteBuffer bytes = ByteBuffer.allocate(o.length*(Constants.IPPortSize));
        for (IPPort i : o )
        {
            String ip = i.getIp();
            String temp = ip.substring(0, ip.indexOf('.'));
            ip = ip.substring(ip.indexOf('.')+1);
            bytes.put((byte)Integer.parseInt(temp));
            temp = ip.substring(0, ip.indexOf('.'));
            ip = ip.substring(ip.indexOf('.')+1);
            bytes.put((byte)Integer.parseInt(temp));
            temp = ip.substring(0, ip.indexOf('.'));
            ip = ip.substring(ip.indexOf('.')+1);
            bytes.put((byte)Integer.parseInt(temp));
            bytes.put((byte)Integer.parseInt(ip));
            int port = i.getPort();
            bytes.putInt(port);
        }
		return bytes.array();
	}

	public static IPPort[] byteArrayToIPPortArray(byte[] a)
	{
        if (a == null || a.length< Constants.IPPortSize) return new IPPort[0];
        ByteBuffer b = ByteBuffer.wrap(a);
        IPPort[] ipp = new IPPort[a.length/ Constants.IPPortSize];
        for (int i=0;i<a.length/Constants.IPPortSize;i++)
        {
            int b1= b.get() & 0xFF;
            int b2= b.get() & 0xFF;
            int b3= b.get() & 0xFF;
            int b4= b.get() & 0xFF;
            int port= b.getInt();
            String ip = b1 + "." + b2 + "." + b3 + "." + b4;
            ipp[i] = new IPPort(ip,port);
        }
        return ipp;
	}


	public static DomainSpaceTree.DomainSpaceTreeSettings ByteArrayToDomainSpaceTreeSettings(byte[] bytes)
	{
		if (bytes.length<Constants.intSize*5)
			return null;
		return new DomainSpaceTree.DomainSpaceTreeSettings(Utilities.bytearrayToInt(Arrays.copyOfRange(bytes,0,Constants.intSize)),Utilities.bytearrayToInt(Arrays.copyOfRange(bytes,Constants.intSize,2*Constants.intSize)),Utilities.bytearrayToInt(Arrays.copyOfRange(bytes,2*Constants.intSize,3*Constants.intSize)),Utilities.bytearrayToInt(Arrays.copyOfRange(bytes,3*Constants.intSize,4*Constants.intSize)),Utilities.bytearrayToInt(Arrays.copyOfRange(bytes,4*Constants.intSize,5*Constants.intSize)));

	}

	public static byte[] DomainSpaceTreeSettingsToByteArray(DomainSpaceTree.DomainSpaceTreeSettings settings)
	{
		return Utilities.add(Utilities.add(Utilities.intToByteArray(settings.getBranchingLevel()),Utilities.add(Utilities.intToByteArray(settings.getRefreshTimeout()),Utilities.intToByteArray(settings.getNumberOfCachedPeers()))),Utilities.add(Utilities.intToByteArray(settings.getMaxDataSizePerNode()),Utilities.intToByteArray(settings.getMaxSingleDataSize())));

	}



	public static boolean IPPortArrayContains(IPPort[] array,IPPort i)
	{
		int iter=0;
		while (iter<array.length)
		{
			if (array[iter].equals(i))
				return true;
		}
		return false;
	}
	
	public static IPPort datagramPacketToIPPort(DatagramPacket d)
	{
		return new IPPort(d.getAddress().toString().substring(1),d.getPort());
	}
	
	public static DatagramPacket createDatagramPacket(byte[] a, IPPort b) throws Exception
	{
		return new DatagramPacket(a, a.length, InetAddress.getByName(b.getIp()), b.getPort());
	}

	public static byte[] intToByteArray(int i) {
		ByteBuffer bytes = ByteBuffer.allocate(Constants.intSize);
		bytes.putInt(i);
		return bytes.array();
	}

	public static int bytearrayToInt(byte[] bytes) {
		if (bytes.length<Constants.intSize) return 0;
		byte[] rcv = Arrays.copyOfRange(bytes,0, Constants.intSize);
		ByteBuffer b = ByteBuffer.wrap(rcv);
		return b.getInt();
	}

	public static byte[] receiveMailBox(DatagramPacket send, IPPort user, Map<IPPort, MailBox> receiveBox) throws Exception {
		Semaphore sem = new Semaphore(0);
		MailBox m= new MailBox(sem);
		receiveBox.put(user, m);
		try {
			NetworkListener.getServerSocket().send(send);

			if (!sem.tryAcquire(5, TimeUnit.SECONDS)) {
				throw new Exception();
			}
		}
		catch(Exception e)
		{
			throw e;
		}
		finally {
			receiveBox.remove(user);
		}
		return m.getData();
	}

    public static byte[] receiveMailBox(DatagramPacket send, Map<IPPort, MailBox> receiveBox) throws Exception {
        return receiveMailBox(send,Utilities.datagramPacketToIPPort(send),receiveBox);
    }

    public static boolean waitFor(DatagramPacket send,IPPort user, Map<IPPort,Semaphore> map)
	{
		Semaphore sem = new Semaphore(0);
		map.put(user, sem);
		try {
			NetworkListener.getServerSocket().send(send);
			if (!sem.tryAcquire(5, TimeUnit.SECONDS)) {
				return false;
			}
		}catch (Exception e)
		{
			return false;
		}
		finally {
			map.remove(user);
		}
		return true;
	}

	public static boolean waitFor(DatagramPacket send, Map<IPPort,Semaphore> map)
	{
		return waitFor(send, Utilities.datagramPacketToIPPort(send), map);
	}

	public static byte[] toSHA256(String input)
	{
		return toSHA256(input.getBytes(StandardCharsets.UTF_8));
	}

	public static IPPort[] eraseMyIp(IPPort[] arr)
	{
		IPPort[] newarr=null;
		if (arr!=null) {
			int myNum = 0;
			for (IPPort a : arr) {
				if (a.equals(MessageParser.getMyInternalIp()) || a.equals(MessageParser.getMyExternalIp())) {
					myNum++;
				}
			}
			if (myNum > 0 ) {
				if (arr.length - myNum == 0) {
					newarr = new IPPort[0];
				} else {
					newarr = new IPPort[arr.length - myNum];
					int tmpcnt = 0;
					for (IPPort a : arr) {
						if (!(a.equals(MessageParser.getMyInternalIp()) || a.equals(MessageParser.getMyExternalIp()))) {
							newarr[tmpcnt++] = a;
						}
					}
				}
			}
			else {
				newarr = arr;
			}

		}
		return newarr;
	}

	public static byte[] toSHA256(byte[] input)
	{
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			System.out.print("SHA-256 instance doesn't exist!");
			System.exit(1);
		}
		return digest.digest(input);
	}

	public static byte[] domainTreeNodePositionToByteArray(DomainTreeNodePosition info) {
		return add(intToByteArray(info.getLevel()),info.getPath());
	}

	public static DomainTreeNodePosition byteArrayToDomainTreeNodePosition(byte[] bytes)
	{
		if (bytes.length<Constants.DomainTreeNodePositionSize)
			return null;
		return new DomainTreeNodePosition(Utilities.bytearrayToInt(Arrays.copyOfRange(bytes,0,Constants.intSize)),Arrays.copyOfRange(bytes,Constants.intSize,Constants.DomainTreeNodePositionSize));
	}

	public static byte[] domainTreeNodeInfoToByteArray(DomainTreeNodeInfo info) {
		return add(Utilities.domainTreeNodePositionToByteArray(info.getPosition()),IPPortToByteArray(info.getNode()));
	}

	public static DomainTreeNodeInfo byteArrayToDomainTreeNodeInfo(byte[] bytes)
	{
		if (bytes.length<Constants.intSize+Constants.hashSize+Constants.IPPortSize)
			return null;
		return new DomainTreeNodeInfo(Utilities.byteArrayToDomainTreeNodePosition(Arrays.copyOfRange(bytes,0,Constants.DomainTreeNodePositionSize)),Utilities.byteArrayToIPPort(Arrays.copyOfRange(bytes,Constants.intSize+Constants.hashSize,Constants.DomainTreeNodePositionSize+Constants.IPPortSize)));
	}

	public static byte[] domainTreeNodesInfoToByteArray(DomainTreeNodesInfo info) {
		return add(Utilities.domainTreeNodePositionToByteArray(info.getPosition()),IPPortArrayToByteArray(info.getNodes()));
	}

	public static DomainTreeNodesInfo byteArrayToDomainTreeNodesInfo(byte[] bytes)
	{
		if (bytes.length<Constants.intSize+Constants.hashSize+Constants.IPPortSize)
			return null;
		return new DomainTreeNodesInfo(Utilities.byteArrayToDomainTreeNodePosition(Arrays.copyOfRange(bytes,0,Constants.DomainTreeNodePositionSize)),Utilities.byteArrayToIPPortArray(Arrays.copyOfRange(bytes,Constants.intSize+Constants.hashSize,bytes.length)));
	}

	public static DomainSpaceTreeData byteArrayToDomainSpaceTreeData(byte[] bytes)
	{
		if (bytes.length<Constants.intSize)
			return null;
		return new DomainSpaceTreeData(Arrays.copyOfRange(bytes,0,bytes.length-Constants.intSize),Utilities.bytearrayToInt(Arrays.copyOfRange(bytes,bytes.length-Constants.intSize,bytes.length)));
	}

	public static DomainSpaceTreeInfo byteArrayToDomainSpaceTreeInfo(byte[] bytes)
	{
		if (bytes.length<Constants.intSize+Constants.hashSize+Constants.IPPortSize)
			return null;
		return new DomainSpaceTreeInfo(Utilities.byteArrayToDomainTreeNodePosition(Arrays.copyOfRange(bytes,0,Constants.DomainTreeNodePositionSize)),Utilities.byteArrayToDomainSpaceTreeData(Arrays.copyOfRange(bytes,Constants.DomainTreeNodePositionSize,bytes.length)));
	}

	public static byte[] domainSpaceTreeDataToByteArray(DomainSpaceTreeData d)
	{
		return Utilities.add(d.getData(),Utilities.intToByteArray(d.getType()));
	}

	public static byte[] domainSpaceTreeInfoToByteArray(DomainSpaceTreeInfo d)
	{
		return Utilities.add(Utilities.domainTreeNodePositionToByteArray(d.getPosition()),Utilities.domainSpaceTreeDataToByteArray(d.getData()));
	}


	public static void printByteArray(byte[] a)
	{
		for (int i=0;i<a.length;i++)
		{
			System.out.print(((int)a[i]) +" ");
		}
	}

	public static int getBranchValue(byte[] path, int myDomainTreeLevel) {
	    BigInteger b = new BigInteger(path);
	    b=b.shiftRight(Constants.hashSize*8-myDomainTreeLevel-Constants.branchingLevel);
	    Integer i=(1<<(Constants.branchingLevel))-1;
		b=b.and(new BigInteger(i.toString()));
        return b.intValue();
	}
	public static Byte[] byteArraytoByteArray(byte[] b)
	{
		Byte[] byteObjects = new Byte[b.length];
		int i=0;
		for(byte b1: b)
			byteObjects[i++] = b1;
		return byteObjects;
	}

	public static byte[] byteArraytobyteArray(Byte[] b)
	{
		byte[] byteObjects = new byte[b.length];
		int i=0;
		for(Byte b1: b)
			byteObjects[i++] = b1;
		return byteObjects;
	}

    public static CustomByteArray byteArraytoCustomByteArray(byte[] b)
    {
        return new CustomByteArray(b);
    }

    public static byte[] customByteArraytobyteArray(CustomByteArray b)
    {
        return b.getArray();
    }

    public static byte[] longToByteArray(long l)
	{
		ByteBuffer bytes = ByteBuffer.allocate(Constants.longSize);
		bytes.putLong(l);
		return bytes.array();
	}


	public static long bytearrayToLong(byte[] bytes) {
		if (bytes.length<Constants.longSize) return 0;
		byte[] rcv = Arrays.copyOfRange(bytes,0, Constants.longSize);
		ByteBuffer b = ByteBuffer.wrap(rcv);
		return b.getLong();
	}


    public static long getTime()
	{
		Calendar d = Calendar.getInstance();
		d.setTimeZone(TimeZone.getTimeZone("GMT"));
		return d.getTime().toInstant().getEpochSecond();
	}

	public static boolean arePathsSame(byte[] path1, byte[] path2, int level)
	{
		boolean f=true,r=true;
		for (int i = 0; f; i++)
		{
			if (i*8==level)
			{
				f=false;
				break;
			}

			byte b1 = path1[i];
			byte b2 = path2[i];
			byte mask = 0x40;
			mask <<= 1;
			for (int j = 0; f; j++)
			{
		if (i*8+j==level)
		{
			f=false;
			break;
		}
		if ((b1&mask)!=(b2&mask)) {
			r = false;
			f = false;
		}
		mask >>= 1;
	}
}
		return r;
	}

	public static byte[] makeChildPath(byte[] myDomainTreePath, int myDomainTreeLevel, int p) {
		byte[] cp = Arrays.copyOfRange(myDomainTreePath,0,Constants.hashSize);
		int start= myDomainTreeLevel;
		int i= 1;

		i = i<<(Constants.branchingLevel-1);
		int v;
		for (int ii=0;ii<Constants.branchingLevel;ii++) {
			v = 0;
			if ((p & i) != 0) {
				v = 1;
			}

			cp=setBit(cp, start, v);
			start++;
			i = i >> 1;
		}
		return cp;
	}

	public static byte[] setBit(byte[] bytes,int number, int value)
	{
		int b = number/8;
		int off = number%8;
		int b1 = 0x80;
		for (int i=0;i<off;i++)
			b1= b1>>1;
		if (value==0)
		{
			b1 = ~b1&0xFF;
		}
		byte bb = (byte)b1;
		if (value==0)
		{
			bytes[b] &=bb;
		}
		else
		{
			bytes[b]|=bb;
		}
		return bytes;
	}

	public static void addChecksumAtEnd(byte[] part, byte[] lasthash) {
		for (int i=0;i<Constants.hashSize;i++)
		{
			part[part.length-Constants.hashSize+i] = lasthash[i];
		}

	}

	public Blob byteArrayToBlob(byte[] b)
	{
		try {
			return new SerialBlob(b);
		} catch (SQLException e) {
			return null;
		}
	}

	public static byte[] blobToByteArray(Blob blob)
	{
		int blobLength = 0;
		try {
			blobLength = (int) blob.length();

			byte[] blobAsBytes = blob.getBytes(1, blobLength);

			blob.free();
			return blobAsBytes;
		} catch (SQLException e) {
			return null;
		}
	}

	public static List<DomainSpaceTreeInfo> parsePrivateFile(byte[] database,KeyPair kp,int maxSingleDataSize) {



		database = Utilities.add(Utilities.intToByteArray(database.length),database);

		database = Utilities.add(database,Utilities.PublicKeytoByteArray(kp.getPublic()));

		database = Utilities.add(database, Constants.nullHash);

		List<byte[]> parts = new LinkedList<>();
		List<DomainSpaceTreeInfo> queue = new LinkedList<>();
		int index = 0;

		while (database.length-index>maxSingleDataSize)
		{
			parts.add(0,Arrays.copyOfRange(database,index,index+maxSingleDataSize));
			index+= maxSingleDataSize-Constants.hashSize;
		}
		parts.add(0,Arrays.copyOfRange(database,index,database.length));

		byte[] lasthash;
		byte[] part= parts.remove(0);

		addToPrivateQueue(part,queue);
		lasthash = Utilities.toSHA256(part);
		while (true)
			try {
				part = parts.remove(0);
				Utilities.addChecksumAtEnd(part,lasthash);
				addToPrivateQueue(part,queue);
				lasthash = Utilities.toSHA256(part);
			}catch (Exception e){
				break;
			}

		part = Utilities.add(lasthash,Utilities.longToByteArray(Utilities.getTime()));
		part = Utilities.cryptWithKey(part,kp.getPrivate(),Cipher.ENCRYPT_MODE);

		DomainSpaceTreeData dstd = new DomainSpaceTreeData(part,3);
		DomainTreeNodePosition dtnp = new DomainTreeNodePosition(Constants.hashSize*8,Utilities.toSHA256(Utilities.PublicKeytoByteArray(kp.getPublic())));
		DomainSpaceTreeInfo dsti = new DomainSpaceTreeInfo(dtnp,dstd);
		queue.add(dsti);

		return queue;

	}

	private static void addToPrivateQueue(byte[] part, List<DomainSpaceTreeInfo> queue) {
		DomainSpaceTreeData dstd = new DomainSpaceTreeData(part,3);
		DomainTreeNodePosition dtnp = new DomainTreeNodePosition(Constants.hashSize*8,Utilities.toSHA256(part));
		DomainSpaceTreeInfo dsti = new DomainSpaceTreeInfo(dtnp,dstd);
		queue.add(dsti);

	}
}