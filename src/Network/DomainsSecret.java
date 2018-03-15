package Network;

import DataStructures.IPPort;
import DataStructures.DomainSpaceTreeInfo;
import Utilities.*;

import javax.crypto.Cipher;
import java.security.*;
import java.util.*;

public class DomainsSecret {

	private Hashtable<String , PublicKey> domainToPublicKey = new Hashtable<String, PublicKey>();
	
	public PublicKey getPublicKey(String domain)
	{
		return domainToPublicKey.get(domain);
	}


    public static boolean verify(byte[] path, IPPort node) {
		if ((node.equals(MessageParser.getMyExternalIp())) && (!node.equals(MessageParser.getMyInternalIp())))
			return true;
		byte[] settb;
		try {
			settb = Utilities.receiveMailBox(Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(MessageParser.GETDOMAINSPACETREESETTINGS), path), node), MessageParser.getDomainSpaceTreeSettingsBox());
		} catch (Exception o) {
			return false;
		}
		if (settb==null)
			return false;
		return true;
    }

	public static boolean verifyData(DomainSpaceTreeInfo data, DomainSpaceTree dst) {
		if (data.getData().getType()!=0 && Arrays.equals(data.getPosition().getPath(),Utilities.toSHA256("fileinfo"))) {
			System.out.println("NISTA1");
			return false;
		}
		if (data.getData().getType()==0)
		{
			if (Arrays.equals(data.getPosition().getPath(),Utilities.toSHA256("fileinfo")))
			{

				if (Utilities.cryptWithKey(data.getData().getData() ,getPublicKey(dst.getDomainHash()), Cipher.DECRYPT_MODE).length!=Constants.hashSize+Constants.longSize*2)
				{
					System.out.println("NISTA2");
					return false;
				}
			}
			else {
				if (!Arrays.equals(Utilities.toSHA256(data.getData().getData()),data.getPosition().getPath()))
				{
					System.out.println("NISTA3");
					return false;
				}
			}
		}
		System.out.println("OK");
		return true;
	}

	public static Key getPublicKey(byte[] domain) {
		byte[] b= Utilities.readAllBytes("keys.cfg");

		int pubsize = Utilities.bytearrayToInt(Arrays.copyOfRange(b,0, Constants.intSize));

		try {
			PublicKey publicKey = Utilities.ByteArraytoPublicKey(Arrays.copyOfRange(b, Constants.intSize, Constants.intSize+pubsize));
			return publicKey;
		}catch (Exception e)
		{}
		return null;

	}
}