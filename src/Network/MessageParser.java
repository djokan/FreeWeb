package Network;

import DataStructures.*;
import Database.Database;
import DataStructures.IPPort;
import Utilities.Constants;
import DataStructures.MailBox;
import Utilities.*;

import java.net.DatagramPacket;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;



public class MessageParser {
    public static final int CONNECT = 0;
    public static final int OK = 1;
    public static final int GETPEERS = 2;
    public static final int GOTPEERS = 3;
    public static final int REVCON = 4;
    public static final int REVEDCON = 5;
    public static final int OPENCONTO = 6;
    public static final int OPENEDCONTO = 7;
    public static final int GETDOMAINSROOT = 8;
    public static final int GOTDOMAINSROOT = 9;
    public static final int KEEPALIVE = 10;
    public static final int GETDOMAINTREENODEINFO = 11;
    public static final int GOTDOMAINTREENODEINFO = 12;
    public static final int SUBMITDOMAINTREENODEINFO = 13;
    public static final int REQUESTDOMAINTREEPOSITION = 14;
    public static final int RECEIVEDOMAINTREEPOSITION = 15;
    public static final int GETDOMAINSPACETREENODEINFO = 16;
    public static final int GOTDOMAINSPACETREENODEINFO = 17;
    public static final int SUBMITDOMAINSPACETREENODEINFO = 18;
    public static final int REQUESTDOMAINSPACETREEPOSITION = 19;
    public static final int RECEIVEDOMAINSPACETREEPOSITION = 20;
    public static final int GETDOMAINSPACETREESETTINGS = 21;
    public static final int GOTDOMAINSPACETREESETTINGS = 22;
    public static final int GETDOMAINSTREEDATA = 23;
    public static final int GETDOMAINSPACETREEDATA = 24;
    private Map<IPPort, Semaphore> ConnectWaiting;
    private Map<IPPort, Semaphore> RevconWaiting;
    private Map<IPPort, MailBox> peerRetreiveBox;


    private Map<IPPort, MailBox> retreiveDomainTreeNodeInfoBox;
    private Map<IPPort, MailBox> domainTreeNodeInfoBox;
    private Map<IPPort, MailBox> domainSpaceTreeSettingsBox;
    private Map<IPPort, MailBox> domainSpaceTreeInfoBox;
    private Map<IPPort, MailBox> domainsRootRetreiveBox;

    private MessageParserAction[] actions;

    private ConcurrentLinkedQueue<IPPort> revcon;

    private boolean success;
    private static MessageParser parser;


    private IPPort myExternalIp=null;
    private IPPort myInternalIp=null;




    private MessageParser()
    {
        ConnectWaiting = new ConcurrentHashMap<>();
        RevconWaiting = new ConcurrentHashMap<>();
        retreiveDomainTreeNodeInfoBox = new ConcurrentHashMap<IPPort, MailBox>();
        domainTreeNodeInfoBox = new ConcurrentHashMap<IPPort, MailBox>();
        domainSpaceTreeInfoBox = new ConcurrentHashMap<IPPort, MailBox>();
        domainSpaceTreeSettingsBox = new ConcurrentHashMap<IPPort, MailBox>();
        peerRetreiveBox = new ConcurrentHashMap<IPPort, MailBox>();
        domainsRootRetreiveBox = new ConcurrentHashMap<IPPort, MailBox>();
        revcon = new ConcurrentLinkedQueue<>();
        for (int i=0;i< Constants.revconCacheSize ; i++ )
        {
            revcon.add(new IPPort("0.0.0.0",Constants.networklistenPort));
        }
        actions = new MessageParserAction[Constants.numberOfCommands];
        actions[0] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    connect(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[1] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    ok(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[2] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    getpeers(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[3] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    gotpeers(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[4] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    revcon(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[5] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    revedcon(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[6] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    openconto(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[7] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    openedconto(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[8] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    getdomainsroot(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[9] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    gotdomainsroot(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[10] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    keepalive();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[11] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    getdomaintreeinfo(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[12] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    gotdomaintreeinfo(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[13] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    submitdomaintreeinfo(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[14] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    requestdomaintreeposition(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[15] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    receivedomaintreeposition(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[16] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    getdomainspacetreeinfo(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[17] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    gotdomainspacetreeinfo(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[18] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    submitdomainspacetreeinfo(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[19] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    requestdomainspacetreeposition(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[20] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    receivedomainspacetreeposition(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[21] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    getdomainspacetreesettings(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[22] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    gotdomainspacetreesettings(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[23] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    getdomainstreedata(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        actions[24] = new MessageParserAction() {
            @Override
            public void action(DatagramPacket d) {
                try {
                    getdomainspacetreedata(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private void getdomainspacetreedata(DatagramPacket d) {
        byte[] domainHash = Arrays.copyOfRange(d.getData(),Constants.intSize,Constants.intSize+Constants.hashSize);
        DomainSpaceTree dst = DomainSpaceTree.getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(domainHash));

        if (dst==null)
        {
            return;
        }

        System.out.println("TREBA DA SSALJEM PODATKE");
        for (Map.Entry<CustomByteArray, DomainSpaceTreeData> a : dst.domainsInfo.entrySet())
        {
            System.out.println("SSALJEM PODATKE");
            DomainSpaceTreeData array = a.getValue();
            DomainSpaceTreeInfo dti = new DomainSpaceTreeInfo(new DomainTreeNodePosition(Constants.hashSize*8,a.getKey().getArray()),array);
            byte[] send = Utilities.add(Utilities.add(Utilities.intToByteArray(MessageParser.SUBMITDOMAINSPACETREENODEINFO),domainHash),Utilities.domainSpaceTreeInfoToByteArray(dti));
            try{
                DatagramPacket dat = Utilities.createDatagramPacket(send,Utilities.datagramPacketToIPPort(d));
                NetworkListener.getServerSocket().send(dat);
            }
            catch (Exception e)
            {

            }
        }
    }

    private void getdomainstreedata(DatagramPacket d) {
        System.out.println("TREBA DA SALJEM PODATKE");
        for (Map.Entry<CustomByteArray,ConcurrentLinkedQueue<IPPort>> a : DomainsTree.getDomainsTree().domainsInfo.entrySet())
        {
            System.out.println("SALJEM PODATKE");
            IPPort[] array = a.getValue().toArray(new IPPort[0]);
            DomainTreeNodesInfo dti = new DomainTreeNodesInfo(new DomainTreeNodePosition(Constants.hashSize*8,a.getKey().getArray()),array);
            byte[] send = Utilities.add(Utilities.intToByteArray(MessageParser.SUBMITDOMAINTREENODEINFO),Utilities.domainTreeNodesInfoToByteArray(dti));
            try{
                DatagramPacket dat = Utilities.createDatagramPacket(send,Utilities.datagramPacketToIPPort(d));
                NetworkListener.getServerSocket().send(dat);
            }
            catch (Exception e)
            {

            }
        }
    }

    private void getdomainspacetreesettings(DatagramPacket d) {

        byte[] domainHash = Arrays.copyOfRange(d.getData(),Constants.intSize,Constants.intSize+Constants.hashSize);
        DomainSpaceTree dst = DomainSpaceTree.getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(domainHash));

        if (dst==null)
        {
            return;
        }

        byte[] send = Utilities.DomainSpaceTreeSettingsToByteArray(dst.getSettings());

        try {
            NetworkListener.getServerSocket().send(Utilities.createDatagramPacket(Utilities.add(Utilities.add(Utilities.intToByteArray(GOTDOMAINSPACETREESETTINGS),domainHash), send), Utilities.datagramPacketToIPPort(d)));
        }catch (Exception ignore)
        {
        }

    }

    private void gotdomainspacetreesettings(DatagramPacket d) {
        System.out.println("DOBIO NAZAD");
        domainSpaceTreeSettingsBox.get(Utilities.datagramPacketToIPPort(d)).setData(Arrays.copyOfRange(d.getData(), Constants.intSize+Constants.hashSize,d.getLength()));
        domainSpaceTreeSettingsBox.get(Utilities.datagramPacketToIPPort(d)).getSem().release();
    }


    public void getdomainspacetreeinfo(DatagramPacket d) {

        byte[] domainHash = Arrays.copyOfRange(d.getData(),Constants.intSize,Constants.intSize+Constants.hashSize);
        DomainSpaceTree dst = DomainSpaceTree.getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(domainHash));

        if (dst==null)
            return;



        DomainSpaceTreeInfo dti = dst.getInfo(Arrays.copyOfRange(d.getData(),Constants.intSize+Constants.hashSize,d.getLength()));
        byte[] send;
        if (dti==null ||  dti.getData()==null || dti.getData().getData()==null || dti.getData().getData().length==0)
        {
            send = new byte[0];
        }
        else
        {
            send = Utilities.domainSpaceTreeInfoToByteArray(dti);
        }

        try {
            NetworkListener.getServerSocket().send(Utilities.createDatagramPacket(Utilities.add(Utilities.add(Utilities.intToByteArray(GOTDOMAINSPACETREENODEINFO),domainHash), send), Utilities.datagramPacketToIPPort(d)));
        }catch (Exception ignore)
        {}
    }

    public void gotdomainspacetreeinfo(DatagramPacket d) {
        domainSpaceTreeInfoBox.get(Utilities.datagramPacketToIPPort(d)).setData(Arrays.copyOfRange(d.getData(), Constants.intSize+Constants.hashSize,d.getLength()));
        domainSpaceTreeInfoBox.get(Utilities.datagramPacketToIPPort(d)).getSem().release();
    }

    public void submitdomainspacetreeinfo(DatagramPacket d) {


        DomainSpaceTreeInfo dti = Utilities.byteArrayToDomainSpaceTreeInfo(Arrays.copyOfRange(d.getData(), Constants.intSize+Constants.hashSize, d.getLength()));

        if (Arrays.equals(dti.getPosition().getPath(),Utilities.toSHA256("fileinfo")))
        {
            dti=dti;
        }

        byte[] domainHash = Arrays.copyOfRange(d.getData(),Constants.intSize,Constants.intSize+Constants.hashSize);
        DomainSpaceTree dst = DomainSpaceTree.getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(domainHash));

        if (dst==null)
            return;

        if (dti.getData()==null || dti.getData().getData()==null || dti.getData().getData().length==0)
            return;
        dst.setInfo(dti);
        System.out.println("DSDSDSDSA:" + dst.getDomainsData().size());

    }

    public static void requestdomainspacetreeposition(DatagramPacket d) {
        IPPort user = Utilities.datagramPacketToIPPort(d);

        byte[] domainHash = Arrays.copyOfRange(d.getData(),Constants.intSize,d.getLength());
        DomainSpaceTree dst = DomainSpaceTree.getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(domainHash));

        if (dst==null)
            return;

        DomainTreeNodePosition dtp = new DomainTreeNodePosition(dst.getMyDomainSpaceTreeLevel(),dst.getMyDomainSpaceTreePath());

        try {
            DatagramPacket packet = Utilities.createDatagramPacket(Utilities.add(Utilities.add(Utilities.intToByteArray(MessageParser.RECEIVEDOMAINSPACETREEPOSITION),domainHash), Utilities.domainTreeNodePositionToByteArray(dtp)),user);
            NetworkListener.getServerSocket().send(packet);
        }catch (Exception ignore)
        {

        }
    }

    public void receivedomainspacetreeposition(DatagramPacket d) {

        byte[] domainHash = Arrays.copyOfRange(d.getData(),Constants.intSize,Constants.intSize+Constants.hashSize);

        DomainTreeNodePosition dtn = Utilities.byteArrayToDomainTreeNodePosition(Arrays.copyOfRange(d.getData(), Constants.intSize+Constants.hashSize, d.getLength()));

        DomainSpaceTree dst = DomainSpaceTree.getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(domainHash));

        if (dst==null)
            return;

        IPPort user = Utilities.datagramPacketToIPPort(d);

        if (dtn.getLevel()==DomainsTree.getMyDomainTreeLevel() && Utilities.arePathsSame(dtn.getPath(),DomainsTree.getMyDomainTreePath(),DomainsTree.getMyDomainTreeLevel()))
        {
            dst.removeParent(user);
            dst.removeChild(user);
        }
        else
        if (dtn.getLevel()==(DomainsTree.getMyDomainTreeLevel()-Constants.branchingLevel) && Utilities.arePathsSame(dtn.getPath(),DomainsTree.getMyDomainTreePath(),(DomainsTree.getMyDomainTreeLevel()-Constants.branchingLevel) ))
        {
            dst.removeNeighbour(user);
            dst.removeChild(user);
        }
        else
        if (dtn.getLevel()==(DomainsTree.getMyDomainTreeLevel()+Constants.branchingLevel) && Utilities.arePathsSame(dtn.getPath(),DomainsTree.getMyDomainTreePath(),DomainsTree.getMyDomainTreeLevel() ))
        {
            dst.removeParent(user);
            dst.removeNeighbour(user);
        }
        else
        {
            dst.removeParent(user);
            dst.removeNeighbour(user);
            dst.removeChild(user);
        }
        dst.setInfo(new DomainSpaceTreeInfo(dtn,new DomainSpaceTreeData(Utilities.IPPortToByteArray(user),5)));
    }



    public void receivedomaintreeposition(DatagramPacket d) {
        DomainTreeNodePosition dtn = Utilities.byteArrayToDomainTreeNodePosition(Arrays.copyOfRange(d.getData(), Constants.intSize, d.getLength()));

        IPPort user = Utilities.datagramPacketToIPPort(d);

        if (dtn.getLevel()==DomainsTree.getMyDomainTreeLevel() && Utilities.arePathsSame(dtn.getPath(),DomainsTree.getMyDomainTreePath(),DomainsTree.getMyDomainTreeLevel()))
        {
            DomainsTree.removeParent(user);
            DomainsTree.removeChild(user);
        }
        else
        if (dtn.getLevel()==(DomainsTree.getMyDomainTreeLevel()-Constants.branchingLevel) && Utilities.arePathsSame(dtn.getPath(),DomainsTree.getMyDomainTreePath(),(DomainsTree.getMyDomainTreeLevel()-Constants.branchingLevel) ))
        {
            DomainsTree.removeNeighbour(user);
            DomainsTree.removeChild(user);
        }
        else
        if (dtn.getLevel()==(DomainsTree.getMyDomainTreeLevel()+Constants.branchingLevel) && Utilities.arePathsSame(dtn.getPath(),DomainsTree.getMyDomainTreePath(),DomainsTree.getMyDomainTreeLevel() ))
        {
            DomainsTree.removeParent(user);
            DomainsTree.removeNeighbour(user);
        }
        else
        {
            DomainsTree.removeParent(user);
            DomainsTree.removeNeighbour(user);
            DomainsTree.removeChild(user);
        }
        DomainTreeNodeInfo dtni = new DomainTreeNodeInfo(dtn,user);
        DomainsTree.getDomainsTree().setInfo(dtni);
    }

    public static void requestdomaintreeposition(DatagramPacket d) {
        IPPort user = Utilities.datagramPacketToIPPort(d);

        DomainTreeNodePosition dtp = new DomainTreeNodePosition(DomainsTree.getMyDomainTreeLevel(),DomainsTree.getMyDomainTreePath());

        try {
            DatagramPacket packet = Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(MessageParser.RECEIVEDOMAINTREEPOSITION), Utilities.domainTreeNodePositionToByteArray(dtp)),user);
            NetworkListener.getServerSocket().send(packet);
        }catch (Exception ignore)
        {

        }

    }

    private void submitdomaintreeinfo(DatagramPacket d) {
        DomainTreeNodesInfo dtn = Utilities.byteArrayToDomainTreeNodesInfo(Arrays.copyOfRange(d.getData(), Constants.intSize, d.getLength()));
        if (dtn.getNodes()==null)
            return;
        DomainsTree.setInfo(dtn);
    }


    private void gotdomaintreeinfo(DatagramPacket d) {

        domainTreeNodeInfoBox.get(Utilities.datagramPacketToIPPort(d)).setData(Arrays.copyOfRange(d.getData(), Constants.intSize,d.getLength()));
        domainTreeNodeInfoBox.get(Utilities.datagramPacketToIPPort(d)).getSem().release();
    }

    private void getdomaintreeinfo(DatagramPacket d){

        DomainTreeNodesInfo dti = DomainsTree.getInfo(Arrays.copyOfRange(d.getData(),Constants.intSize,d.getLength()));
        byte[] send;
        if (dti==null ||  dti.getNodes().length==0)
        {
            //System.out.println("ne saljem nista");
            send = new byte[0];
        }
        else
        {
            send = Utilities.domainTreeNodesInfoToByteArray(dti);
        }

        try {
            NetworkListener.getServerSocket().send(Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(GOTDOMAINTREENODEINFO), send), Utilities.datagramPacketToIPPort(d)));
        }catch (Exception ignore)
        {}
    }

    private void gotdomainsroot(DatagramPacket d) {
        domainsRootRetreiveBox.get(Utilities.datagramPacketToIPPort(d)).setData(Arrays.copyOfRange(d.getData(), Constants.intSize,d.getLength()));
        domainsRootRetreiveBox.get(Utilities.datagramPacketToIPPort(d)).getSem().release();
    }

    private void getdomainsroot(DatagramPacket d) {
        IPPort[] roots = DomainsTree.getDomainsRoots();
        if (DomainsTree.getMyDomainTreeLevel()==0)
        {
            if (roots==null)
            {
                roots = new IPPort[1];
                if (Utilities.isExternal(Utilities.datagramPacketToIPPort(d)))
                {
                    roots[0] = myExternalIp;
                }
                else {
                    roots[0] = myInternalIp;
                }
            }
            else
            {
                IPPort[] roots1 = new  IPPort[roots.length+1];
                for (int iiia = 0;iiia<roots.length;iiia++)
                {
                    roots1[iiia] = roots[iiia];
                }
                if (Utilities.isExternal(Utilities.datagramPacketToIPPort(d))) {
                    roots1[roots.length] = myExternalIp;
                }
                else {
                    roots1[roots.length] = myInternalIp;
                }
                roots= roots1;
            }
        }
        if (myExternalIp==null) return;
        try {
            if (roots!=null)
                NetworkListener.getServerSocket().send(Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(GOTDOMAINSROOT),Utilities.IPPortArrayToByteArray(roots)), Utilities.datagramPacketToIPPort(d)));
            else
                NetworkListener.getServerSocket().send(Utilities.createDatagramPacket(Utilities.intToByteArray(GOTDOMAINSROOT), Utilities.datagramPacketToIPPort(d)));

        } catch (Exception ignore)
        {}
    }


    public static IPPort getMyInternalIp() {
        return getParser().myInternalIp;
    }

    private static void setMyInternalIp(IPPort myInternalIp) {
        getParser().myInternalIp = myInternalIp;
        try {
            Statement stmt = Database.connectToDatabase("freewebdata", Constants.defaultAdmin, Constants.defaultPassword).createStatement();
            for (int temp=0;temp<10;temp++) {
                try {
                    stmt.execute("UPDATE Peers SET ip='255.0.0." + ThreadLocalRandom.current().nextInt(50, 255 + 1) +"', time= TIMESTAMPADD(MINUTE, -1,(SELECT MIN(time) FROM Peers))  WHERE ip='" + myInternalIp + "'");
                }catch (Exception ignored)
                {

                }
            }
        }catch (Exception ignored)
        {

        }
    }

    public static Map<IPPort, Semaphore> getRevconWaiting() {
        return getParser().RevconWaiting;
    }

    public static Map<IPPort, MailBox> getDomainTreeNodeInfoBox() {
        return getParser().domainTreeNodeInfoBox;
    }

    public static Map<IPPort, MailBox> getDomainSpaceTreeInfoBox() {
        return getParser().domainSpaceTreeInfoBox;
    }

    public static Map<IPPort, MailBox> getDomainsRootRetreiveBox() {
        return getParser().domainsRootRetreiveBox;
    }

    public static Map<IPPort, MailBox> getPeerRetreiveBox() {
        return getParser().peerRetreiveBox;
    }

    public static Map<IPPort, MailBox> getDomainSpaceTreeSettingsBox() {
        return getParser().domainSpaceTreeSettingsBox;
    }

    public static Map<IPPort, Semaphore> getConnectionRequests() {
        return getParser().ConnectWaiting;
    }

    public static MessageParser getParser()
    {
        if (parser==null)
        {
            parser = new MessageParser();
        }
        return parser;
    }


    static{
        parser = null;
    }

    public static void parse(DatagramPacket d)
    {
        //System.out.println("Request: "+Utilities.bytearrayToInt(d.getData()) + ", length: " + d.getLength());
        getParser().success = true;
        getParser().actions[Utilities.bytearrayToInt(d.getData())].action(d);
        if (getParser().success) {
            refreshConnection(d);
        }
    }

    private  void keepalive()
    {
        success = false;
    }

    private void gotpeers(DatagramPacket d) {
        IPPort[] users = Utilities.byteArrayToIPPortArray(Arrays.copyOfRange(d.getData(),Constants.intSize,d.getLength()));
        try {
            peerRetreiveBox.get(Utilities.datagramPacketToIPPort(d)).setData(Utilities.IPPortArrayToByteArray(users));
            peerRetreiveBox.get(Utilities.datagramPacketToIPPort(d)).getSem().release();
        } catch (Exception ignored) {
        }

    }

    private void getpeers(DatagramPacket d) {
        Statement stmt;
        try {
            stmt = Database.connectToDatabase("freewebdata", Constants.defaultAdmin, Constants.defaultPassword).createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = stmt.executeQuery("SELECT * FROM Peers WHERE ip NOT LIKE '255.%' ORDER BY time DESC");
            NetworkListener.getServerSocket().send(Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(GOTPEERS), Utilities.IPPortArrayToByteArray(Utilities.resultSetToIPPortArray(rs))), Utilities.datagramPacketToIPPort(d)));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void openconto(DatagramPacket d) {
        try {
            IPPort user = Utilities.byteArrayToIPPort(Arrays.copyOfRange(d.getData(), Constants.intSize, d.getLength()));
            DatagramPacket s = Utilities.createDatagramPacket(Utilities.intToByteArray(CONNECT), user);
            NetworkListener.getServerSocket().send(s);
            NetworkListener.getServerSocket().send(Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(OPENEDCONTO),Arrays.copyOfRange(d.getData(), Constants.intSize, d.getLength())), Utilities.datagramPacketToIPPort(d)));
            // detect self by knowing external and internal ip address
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void revcon(DatagramPacket d) {

        try
        {
            IPPort src = Utilities.datagramPacketToIPPort(d);
            boolean isExternal = Utilities.isExternal(src);
            if (isExternal)
            {
                if (myExternalIp== null)
                {
                    success = false;
                    return;
                }
            }
            else
            {
                if (myInternalIp== null)
                {
                    success = false;
                    return;
                }
            }
            IPPort user = Utilities.byteArrayToIPPort(Arrays.copyOfRange(d.getData(), Constants.intSize, d.getLength()));
            if (user.equals(isExternal?myExternalIp:myInternalIp))
            {
                success = false;
                return;
            }
            DatagramPacket s = Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(OPENCONTO), Utilities.IPPortToByteArray(Utilities.datagramPacketToIPPort(d))), user);
            revcon.add(Utilities.datagramPacketToIPPort(d));
            revcon.poll();
            NetworkListener.getServerSocket().send(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static IPPort getMyExternalIp() {
        return getParser().myExternalIp;
    }


    private static void setMyExternalIp(IPPort myExternalIp) {
        getParser().myExternalIp = myExternalIp;
        try {
            Statement stmt = Database.connectToDatabase("freewebdata", Constants.defaultAdmin, Constants.defaultPassword).createStatement();
            for (int temp=0;temp<10;temp++) {
                try {
                    stmt.execute("UPDATE Peers SET ip='255.0.0." + ThreadLocalRandom.current().nextInt(50, 255 + 1) +"', time= TIMESTAMPADD(MINUTE, -1,(SELECT MIN(time) FROM Peers))  WHERE ip='" + myExternalIp + "'");
                }catch (Exception ignored)
                {

                }
            }
        }catch (Exception ignored)
        {

        }
    }



    private void openedconto(DatagramPacket d) {
        try {
            IPPort user = Utilities.byteArrayToIPPort(Arrays.copyOfRange(d.getData(), Constants.intSize, d.getLength()));
            if (revcon.contains(user))
            {
                NetworkListener.getServerSocket().send(Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(REVEDCON),Utilities.IPPortToByteArray(Utilities.datagramPacketToIPPort(d))), user));

            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void revedcon(DatagramPacket d) {
        try {
            RevconWaiting.get(Utilities.datagramPacketToIPPort(d)).release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    private static void  refreshConnection(DatagramPacket d)
    {
        IPPort user = Utilities.datagramPacketToIPPort(d);
        if (!KeepAlive.getKeepAliveQueue().contains(user))
            KeepAlive.getKeepAliveQueue().add(user);
    }

    private void  ok(DatagramPacket d) throws Exception
    {
        if (ConnectWaiting.containsKey(Utilities.datagramPacketToIPPort(d)))
            ConnectWaiting.get(Utilities.datagramPacketToIPPort(d)).release();
        IPPort u = Utilities.byteArrayToIPPort(Arrays.copyOfRange(d.getData(), Constants.intSize, d.getLength()));
        if (Utilities.isExternal(u) )
        {
            if (myExternalIp==null)
                setMyExternalIp(u);
        }
        else
        {
            if (myInternalIp==null)
                setMyInternalIp(u);
        }
    }

    private void connect(DatagramPacket d) throws Exception
    {
        IPPort destination = Utilities.byteArrayToIPPort(Arrays.copyOfRange(d.getData(), Constants.intSize, d.getLength()));
        if (!(destination.equals(Utilities.datagramPacketToIPPort(d))))
        {
            NetworkListener.getServerSocket().send(Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(OK),Utilities.IPPortToByteArray(Utilities.datagramPacketToIPPort(d))), Utilities.datagramPacketToIPPort(d)));
            addtoPeersDatabase(d); // TODO add filter
        }
        else
        {
            success = false;
            if (Utilities.isExternal(destination))
                setMyExternalIp(destination);
            else
                setMyInternalIp(destination);
        }
    }

    private void addtoPeersDatabase(DatagramPacket d) throws Exception
    {
        if (!Database.doesDatabaseExist("freewebdata"))
        {
            Database.setInitialFreeWebData();
        }
        IPPort user = Utilities.datagramPacketToIPPort(d);
        Statement stmt = Database.connectToDatabase("freewebdata", Constants.defaultAdmin, Constants.defaultPassword).createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM Peers WHERE ip = '"+ user.getIp() +"' AND port = " + user.getPort());
        if (rs.next()) return;
        stmt.execute("UPDATE Peers SET ip = '" + user.getIp() + "', port = " + user.getPort() + ", time = NOW() WHERE time IN (SELECT MIN(time) FROM Peers) ");
        System.out.println("Added to Database: " + Utilities.datagramPacketToIPPort(d).toString());
    }

    private void error(DatagramPacket d) throws Exception
    {
        NetworkListener.getServerSocket().send(Utilities.createDatagramPacket("nn".getBytes(),Utilities.datagramPacketToIPPort(d)));
    }



    public interface MessageParserAction {
        void action(DatagramPacket d);
    }




}
