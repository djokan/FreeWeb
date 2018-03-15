package Utilities;

public class Constants {

	public static final int browserlistenPort = 3344;
    public static final int PUBLICKEYSIZE = 1024;
    public static int networklistenPort = 3347;
	public static int sqlServerTcpPort = 3341;
	public static final String defaultAdmin = "admin";
	public static final String defaultPassword = "password";
	public static int sqlServerWebPort = 3340;
	public static int sqlServerPgPort = 3342;
	public static final String databaseConnectionSuffix = ";CIPHER=AES;TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0";
	public static final int ThreadPoolSize = 5;
	public static final String randomIp = "52.32.216.67";
	public static final int numberOfCachedPeers = 32;
	public static final int keepAliveQueueSize = 50;
	public static final int MTU = 534;
	public static final int tickValue = 500;
	public static final String[] initIps = {"192.168.1.100",
											"192.168.1.2",
											"192.168.1.3",
											"192.168.1.4",
											"192.168.1.5",
											"192.168.1.6",
											"192.168.1.7",
											"192.168.1.8",
											"192.168.1.9",
											"192.168.0.2",
											"192.168.0.3",
											"192.168.0.4",
											"192.168.0.5",
											"192.168.0.6",
											"192.168.0.7",
											"192.168.0.8",
											"192.168.0.9",
											"52.32.216.67"
											};
	public static final String[] initPorts = {"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347",
												"3347"};
	public static final int revconCacheSize = 5;

    public static final int numberOfCommands= 30;
	public static final int intSize = 4;
	public static final int IPPortSize = 8;
	public static final int branchingLevel = 2; // 2^branchingLevel children nodes
	public static final int hashSize= 32;
	public static final int DomainTreeNodePositionSize = intSize+hashSize;
	public static final int numberOfDomainTrys = 5;
	public static final int refreshDomainTreeTimeout=60*1000*2;
	public static final int maxSizePerNode = 32*4*numberOfCachedPeers;
	public static final int numberOfChildren = 1<<(Constants.branchingLevel);
	public static final byte[] nullHash = {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
	public static final int longSize = 8;
}
