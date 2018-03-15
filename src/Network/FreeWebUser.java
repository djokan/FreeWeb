package Network;

import DataStructures.IPPort;
import Utilities.Utilities;

import java.net.DatagramPacket;

import static Utilities.Utilities.waitFor;

public class FreeWebUser {

	public static boolean connect(IPPort user)
	{
		if (user.equals(MessageParser.getMyExternalIp()) || user.equals(MessageParser.getMyInternalIp()))
			return false;
		DatagramPacket sendPacket;
		try {
		    return Utilities.waitFor(Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(MessageParser.CONNECT), Utilities.IPPortToByteArray(user)), user),MessageParser.getConnectionRequests());
		}
		catch (Exception e)
		{
			return false;
		}
    }

	public static boolean connectVia(IPPort server,IPPort user)
	{
		if (server.equals(user))
		{
			return connect(server);
		}
		if (user.equals(MessageParser.getMyExternalIp()) || server.equals(user) || user.equals(MessageParser.getMyInternalIp()) || (Utilities.isExternal(server) && !Utilities.isExternal(user)))
			return false;
		if (connect(user))
            return true;
		try
		{
			if (waitFor(Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(MessageParser.REVCON),Utilities.IPPortToByteArray(user)),server),user,MessageParser.getRevconWaiting()))
			{
				return connect(user);
			}
			return false;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public static IPPort[] getPeersList(IPPort user)
	{
		if (user.equals(MessageParser.getMyExternalIp()) || user.equals(MessageParser.getMyInternalIp()))
			return null;
		try
		{
			byte[] r = Utilities.receiveMailBox(Utilities.createDatagramPacket(Utilities.intToByteArray(MessageParser.GETPEERS),user),user,MessageParser.getPeerRetreiveBox());
			if (r==null)
				return null;
			return Utilities.byteArrayToIPPortArray(r);
		}
		catch (Exception e)
		{
			return null;
		}
		finally
		{
			MessageParser.getPeerRetreiveBox().remove(user);
		}
	}
}
