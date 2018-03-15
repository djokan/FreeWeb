package DataStructures;

import java.io.Serializable;

public class IPPort implements Serializable{
	
	private static final long serialVersionUID = 1L;
	String ip;
	Integer port;
	
	public IPPort(String ip, int port)
	{
		this.ip = ip;
		this.port = port;
	}
	
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public int hashCode()
	{
		 return ip.hashCode() ^ port.hashCode();
	}
	public boolean equals(Object other)
	{
		if (other == null) return false;
	    if (other == this) return true;
	    if (!(other instanceof IPPort))return false;
	    IPPort otherMyClass = (IPPort)other;
		if (!otherMyClass.ip.equals(this.ip)) return false;
		if (!otherMyClass.port.equals(this.port)) return false;
		return true;
	}
	
	public String toString(){
		return ip + ":" + port;
	}
}
