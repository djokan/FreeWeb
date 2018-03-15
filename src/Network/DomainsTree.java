package Network;

import DataStructures.CustomByteArray;
import DataStructures.DomainTreeNodeInfo;
import DataStructures.DomainTreeNodePosition;
import DataStructures.DomainTreeNodesInfo;
import Network.NetworkSynchronizer.*;
import DataStructures.IPPort;
import Utilities.*;

import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

public class DomainsTree implements RepeatingTask { // TODO what if branch levels arent same?


    private static DomainsTree domainsTree;
    private ConcurrentLinkedQueue<IPPort> domainsRoots;

    public static int getMyDomainTreeLevel() {
        return getDomainsTree().myDomainTreeLevel;
    }

    private static Semaphore sem;

    public static Map<CustomByteArray, ConcurrentLinkedQueue<IPPort>> getDomainsInfo() {
        return getDomainsTree().domainsInfo;
    }

    public Map<CustomByteArray, ConcurrentLinkedQueue<IPPort>> domainsInfo;
    private int myDomainTreeLevel;
    private byte[] myDomainTreePath;
    private int actionSwitch=0; // 0-4= refresh  5= change
    private int timer=0;
    private ConcurrentLinkedQueue<IPPort> myParents;
    private ConcurrentLinkedQueue<IPPort> myNeighbours;
    private ConcurrentLinkedQueue<IPPort>[] myChildren;

    // utility function for getNodesInfo
    private IPPort startSettingsforGetNodesInfo(DomainTreeNodePosition d, DomainTreeNodesInfo iii)
    {
        IPPort domainsRoot;
        if (iii==null) {
            boolean b=false;
            for(ConcurrentLinkedQueue<IPPort> iiaa :getMyChildren())
            {
                if (iiaa.size()>0) {
                    b=true;
                    break;
                }
            }
            if ((getMyDomainTreeLevel()<=d.getLevel()) && Utilities.arePathsSame (d.getPath(),getMyDomainTreePath(), getMyDomainTreeLevel()) && (getParents().length !=0 || getNeighbours().length != 0 || b)) {
                if (getChildren(d.getPath()).length == 0)
                    return null;
                domainsRoot = getChildren(d.getPath())[ThreadLocalRandom.current().nextInt(0, getChildren(d.getPath()).length)];
            } else {
                domainsRoot = getDomainsRoot();
            }
        }
        else
            domainsRoot = iii.getNodes()[ThreadLocalRandom.current().nextInt(0,iii.getNodes().length)];
        return domainsRoot;
    }

    private DomainTreeNodesInfo getNodesInfo(DomainTreeNodePosition d  ,DomainTreeNodesInfo iii) // finds nodes that host given position on tree starting from second argument
    {
        if (d.getLevel()==256)
        {
            d=d;
        }
        IPPort domainsRoot = startSettingsforGetNodesInfo(d,iii);

        if (domainsRoot==null)
            return getInfo(d.getPath());

        IPPort user = domainsRoot;
        int maxlevel = 0;
        byte[] hash = d.getPath();
        DomainTreeNodesInfo lastlastdti = null;
        DomainTreeNodesInfo lastdti = null;
        DomainTreeNodesInfo dti=null;

        if (d.getLevel()==0)
        {
            return findRoots();
        }

        for (int i=0;i<Constants.numberOfDomainTrys;i++) {
            byte[] receive;
            try {
                byte[] send = Utilities.add(Utilities.intToByteArray(MessageParser.GETDOMAINTREENODEINFO), hash);

                receive = Utilities.receiveMailBox(Utilities.createDatagramPacket(send , user), MessageParser.getDomainTreeNodeInfoBox());
                dti = Utilities.byteArrayToDomainTreeNodesInfo(receive);

                if (dti!=null && dti.getPosition().getLevel() < Constants.hashSize * 8)
                    dti.setNode(Utilities.eraseMyIp(dti.getNodes()));

            } catch (Exception ea) {
                dti = null;
            }
            if (dti !=null && dti.getPosition().getLevel() == d.getLevel() && dti.getNodes()!=null && dti.getNodes().length>0 && Utilities.arePathsSame(hash,dti.getPosition().getPath(),d.getLevel())) {
                System.out.println("FOUND!" + dti.getNodes()[0].toString());
                return dti;
            }

            if (((dti == null || dti.getNodes()==null || dti.getNodes().length == 0) && user.equals( domainsRoot))|| dti.getPosition().getLevel()>d.getLevel() || dti.getPosition().getLevel()%Constants.branchingLevel!=0) {
                System.out.println("FIRST FAIL!" + user.toString());
                removeFromDomainsRoots(user);

                try{
                    MessageParser.requestdomaintreeposition(Utilities.createDatagramPacket("a".getBytes(),user));

                    NetworkListener.getServerSocket().send(Utilities.createDatagramPacket(Utilities.intToByteArray(MessageParser.REQUESTDOMAINTREEPOSITION),user));
                }
                catch (Exception e)
                {

                }
                domainsRoot = startSettingsforGetNodesInfo(d,iii);
                if (domainsRoot==null)
                    return null;
                user = domainsRoot;
            } else {
                if (dti == null || dti.getNodes()==null || dti.getNodes().length == 0 || (!(Arrays.equals(dti.getPosition().getPath(),hash))) || dti.getPosition().getLevel()<=maxlevel ) {
                    System.out.println("FAIL!" + user.toString());
                    try{
                        MessageParser.requestdomaintreeposition(Utilities.createDatagramPacket("a".getBytes(),user));
                        NetworkListener.getServerSocket().send(Utilities.createDatagramPacket(Utilities.intToByteArray(MessageParser.REQUESTDOMAINTREEPOSITION),user));
                    }
                    catch (Exception e)
                    {

                    }
                    dti = lastdti;
                    try {
                        user = dti.getNodes()[ThreadLocalRandom.current().nextInt(0, dti.getNodes().length)];
                    }catch (Exception e)
                    {
                        int iiia = 2;
                    }
                } else {
                    System.out.println("LEVEL DEEPER!" + user.toString());
                    lastdti = dti;

                    maxlevel= dti.getPosition().getLevel();
                    user = dti.getNodes()[ThreadLocalRandom.current().nextInt(0, dti.getNodes().length )];
                }
            }
        }
        if (dti==null)
        {
            if (lastdti==null)
            {
                return findRoots();
            }
            return lastdti;
        }
        return dti;
    }

    private void removeFromDomainsRoots(IPPort user) {
        if ((!user.equals(MessageParser.getMyExternalIp())) && (!user.equals(MessageParser.getMyInternalIp()))) {
            getDomainsTree().domainsRoots.remove(user);
        }
    }

    private DomainTreeNodesInfo getNodesInfo(DomainTreeNodePosition d) // finds nodes that host given position on tree starting from root
    {
        return getNodesInfo(d,null);
    }

    private void changeDomainTreePosition() {//TODO test


        System.out.println("CHG");

        byte[] hash = new byte[Constants.hashSize];

        ThreadLocalRandom.current().nextBytes(hash);

        DomainTreeNodesInfo dti = getNodesInfo(new DomainTreeNodePosition(256,hash));

        int maxlevel ;



        if (dti!=null )
            maxlevel= dti.getPosition().getLevel() - dti.getPosition().getLevel()%Constants.branchingLevel;
        else
            maxlevel=0;

        int myLevel = ThreadLocalRandom.current().nextInt(0,maxlevel/Constants.branchingLevel+1)*Constants.branchingLevel;

        myDomainTreeLevel = myLevel;
        myDomainTreePath = hash;

        getMyNeighbours().clear();
        getMyParents().clear();

        for (int i=0;i<Constants.numberOfChildren;i++)
        {
            try {
                myChildren[i].clear();
            }catch (Exception ignored)
            {
                break;
            }

        }

        refreshDomainTreePosition();
    }


    void refreshDomainTreePosition() // finds neighbours, parents and children
    {//TODO test
        System.out.println("REF");

        DomainTreeNodesInfo dti;
        if (myDomainTreeLevel>=Constants.branchingLevel*2)
        {
//            System.out.println("OVO NE SME DA SE DESI SA MALOM MREZOM!");
//            System.exit(1);
            dti = getNodesInfo(new DomainTreeNodePosition(myDomainTreeLevel-Constants.branchingLevel,myDomainTreePath));
            if (dti.getPosition().getLevel()!=myDomainTreeLevel-Constants.branchingLevel|| dti.getNodes()==null || dti.getNodes().length==0)
                return;
            while (true) {
                try {
                    myParents.remove();
                }catch (Exception ignored)
                {
                    break;
                }
            }
            for (IPPort node : dti.getNodes()) {
                boolean skip=false;
                IPPort[] ipp = new IPPort[1];
                DomainTreeNodesInfo dtni;
                if (Utilities.isExternal(node)) {
                    if (MessageParser.getMyExternalIp()==null)
                        skip=true;
                    ipp[0] =  MessageParser.getMyExternalIp();
                    dtni = new DomainTreeNodesInfo(new DomainTreeNodePosition(getMyDomainTreeLevel(), getMyDomainTreePath()),ipp);
                }
                else {
                    if (MessageParser.getMyInternalIp()==null)
                        skip=true;
                    ipp[0]=MessageParser.getMyInternalIp();
                    dtni = new DomainTreeNodesInfo(new DomainTreeNodePosition(getMyDomainTreeLevel(), getMyDomainTreePath()), ipp);
                }
                if (!skip)
                    try {
                        DatagramPacket dtp = Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(MessageParser.SUBMITDOMAINTREENODEINFO),Utilities.domainTreeNodesInfoToByteArray(dtni)),node);
                        NetworkListener.getServerSocket().send(dtp);
                    } catch (Exception e) {
                    }

                addParent(dti.getPosition(),node);
            }
            dti = getNodesInfo(new DomainTreeNodePosition(myDomainTreeLevel,myDomainTreePath),dti);
            if (dti.getPosition().getLevel()!=myDomainTreeLevel)
                return;
            while (true) {
                try {
                    myNeighbours.remove();
                }catch (Exception ignored)
                {
                    break;
                }
            }
            for (IPPort node : dti.getNodes()) {
                addNeighbour(dti.getPosition(),node);
            }
            findChildrenAndDomains();
        }
        else if (myDomainTreeLevel==Constants.branchingLevel)
        {
            dti = findRoots();
            if ((dti.getPosition().getLevel()+Constants.branchingLevel!=myDomainTreeLevel) || (dti.getNodes()==null) || dti.getNodes().length==0 )
                return;
            while (true) {
                try {
                    myParents.remove();
                }catch (Exception ignored)
                {
                    break;
                }
            }
            for (IPPort node : dti.getNodes()) {

                boolean skip=false;
                IPPort[] ipp = new IPPort[1];
                DomainTreeNodesInfo dtni;
                if (Utilities.isExternal(node)) {
                    if (MessageParser.getMyExternalIp()==null)
                        skip=true;
                    ipp[0] =  MessageParser.getMyExternalIp();
                    dtni = new DomainTreeNodesInfo(new DomainTreeNodePosition(getMyDomainTreeLevel(), getMyDomainTreePath()),ipp);
                }
                else {
                    if (MessageParser.getMyInternalIp()==null)
                        skip=true;
                    ipp[0]=MessageParser.getMyInternalIp();
                    dtni = new DomainTreeNodesInfo(new DomainTreeNodePosition(getMyDomainTreeLevel(), getMyDomainTreePath()), ipp);
                }
                if (!skip)
                    try {
                        DatagramPacket dtp = Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(MessageParser.SUBMITDOMAINTREENODEINFO),Utilities.domainTreeNodesInfoToByteArray(dtni)),node);
                        NetworkListener.getServerSocket().send(dtp);
                    } catch (Exception e) {
                    }

                addParent(dti.getPosition(),node);
            }
            dti = getNodesInfo(new DomainTreeNodePosition(myDomainTreeLevel,myDomainTreePath),dti);
            if (dti.getPosition().getLevel()!=myDomainTreeLevel)
                return;
            while (true) {
                try {
                    myNeighbours.remove();
                }catch (Exception ignored)
                {
                    break;
                }
            }
            for (IPPort node : dti.getNodes()) {
                addNeighbour(dti.getPosition(),node);
            }
            findChildrenAndDomains();
        }
        else
        {
            while (true) {
                try {
                    myParents.remove();
                }catch (Exception ignored)
                {
                    break;
                }
            }
            dti = findRoots();
            if (dti.getPosition().getLevel()!=myDomainTreeLevel || dti.getNodes()==null || dti.getNodes().length==0)
                return;
            while (true) {
                try {
                    myNeighbours.remove();
                }catch (Exception ignored)
                {
                    break;
                }
            }
            if (dti.getNodes()!=null)
            for (IPPort node : dti.getNodes()) {
                addNeighbour(dti.getPosition(),node);
            }
            findChildrenAndDomains();
        }

        System.out.println("LVL: " + myDomainTreeLevel);
        System.out.println("PATH: " + new String(myDomainTreePath));
    }

    DomainTreeNodesInfo findRoots()//TODO test
    {
        getDomainsRoot();
        DomainTreeNodesInfo dti = new DomainTreeNodesInfo(new DomainTreeNodePosition(0,myDomainTreePath),getDomainsRoots());
        IPPort[] ipp = dti.getNodes();
        IPPort[] ipp1;
        if (getMyDomainTreeLevel()==0) {
            if (ipp==null)
            {
                ipp1 = new IPPort[1];
                ipp1[0]= MessageParser.getMyExternalIp();
            }
            else {
                ipp1 = new IPPort[ipp.length + 1];
                for (int i=0;i<ipp.length;i++)
                    ipp1[i]=ipp[i];
                ipp1[ipp.length]= MessageParser.getMyExternalIp();
            }

            dti.setNode(ipp1);
        }
        return dti;
    }

    void findChildrenAndDomains()//TODO test
    {
        if (getNeighbours().length==0)
            return;
        IPPort randomnei=null;
        DomainTreeNodesInfo dti=null;
        IPPort[] neigh = getNeighbours();

        try {
            NetworkListener.getServerSocket().send(Utilities.createDatagramPacket(Utilities.intToByteArray(MessageParser.GETDOMAINSTREEDATA), neigh[ThreadLocalRandom.current().nextInt(0, neigh.length)]));
        }
        catch (Exception e)
        {}

        for (int p=0;p<1<<Constants.branchingLevel;p++)
        {
            byte[] receive;
            for (int i=0;i<Constants.numberOfDomainTrys;i++) {
                try {
                    byte[] cp = Utilities.makeChildPath(myDomainTreePath, myDomainTreeLevel, p);
                    byte[] send = Utilities.add(Utilities.intToByteArray(MessageParser.GETDOMAINTREENODEINFO), cp);
                    randomnei = neigh[ThreadLocalRandom.current().nextInt(0, neigh.length)];
                    receive = Utilities.receiveMailBox(Utilities.createDatagramPacket(send, randomnei), MessageParser.getDomainTreeNodeInfoBox());
                    dti = Utilities.byteArrayToDomainTreeNodesInfo(receive);
                    if (dti != null && dti.getNodes().length>0) break;
                } catch (Exception e) {
                }
            }

            if (dti!=null && dti.getNodes()!=null)
            {
                if (dti.getPosition().getLevel()==getMyDomainTreeLevel()+Constants.branchingLevel)
                {
                    for (IPPort node : dti.getNodes()) {
                        addChild(dti.getPosition(),node);
                    }
                }
                else if (dti.getPosition().getLevel()==Constants.hashSize*8)
                {
                    for (IPPort node : dti.getNodes()) {
                        setInfo(dti);
                    }
                }
            }
        }


    }




    static
    {
        domainsTree = null;
    }

    private DomainsTree()
    {
        domainsRoots = new ConcurrentLinkedQueue<>();
        myParents = new ConcurrentLinkedQueue<>();
        myNeighbours = new ConcurrentLinkedQueue<>();
        myChildren = new ConcurrentLinkedQueue[1<<Constants.branchingLevel];
        domainsInfo = new ConcurrentHashMap<CustomByteArray, ConcurrentLinkedQueue<IPPort>>();
        for (int i=0;i<(1<<Constants.branchingLevel);i++)
        {
            myChildren[i] = new ConcurrentLinkedQueue<>();
        }
        myDomainTreeLevel = 0;
        myDomainTreePath = new byte[Constants.hashSize];
        ThreadLocalRandom.current().nextBytes(myDomainTreePath);

        sem = new Semaphore(1);
    }

    public static ConcurrentLinkedQueue<IPPort>[] getMyChildren() {
        return getDomainsTree().myChildren;
    }

    public static byte[] getMyDomainTreePath() {
        return getDomainsTree().myDomainTreePath;
    }

    public static void addChild(DomainTreeNodePosition pos, IPPort user) {

        if ((!user.equals(MessageParser.getMyExternalIp())) && (!user.equals(MessageParser.getMyInternalIp()))) {
            int branchValue = Utilities.getBranchValue(pos.getPath(), getMyDomainTreeLevel());


            for (CustomByteArray b : getDomainsInfo().keySet()) {
                if (Utilities.getBranchValue(b.getArray(), getMyDomainTreeLevel()) == branchValue) {
                    System.out.println("IZBACIO SAM DIJETETOVO!");
                    getDomainsInfo().remove(b);
                }
            }

            if (!getMyChildren()[branchValue].contains(user)) {
                getMyChildren()[branchValue].add(user);
                if (getMyChildren()[branchValue].size() > Constants.numberOfCachedPeers) {
                    getMyChildren()[branchValue].poll();
                }
            }
        }
    }

    public static void removeChild(IPPort user)
    {
        for (int i=0;i<Constants.numberOfChildren;i++)
            getMyChildren()[i].remove(user);

    }

    public static IPPort[] getChildren(byte[] path)
    {
        return getMyChildren()[Utilities.getBranchValue(path,getMyDomainTreeLevel())].toArray(new IPPort[0]);
    }

    public static IPPort[] getNeighbours()
    {
        return getMyNeighbours().toArray(new IPPort[0]);
    }

    public static IPPort[] getParents()
    {
        return getMyParents().toArray(new IPPort[0]);
    }


    public static ConcurrentLinkedQueue<IPPort> getMyNeighbours() {
        return getDomainsTree().myNeighbours;
    }

    public static void addNeighbour(DomainTreeNodePosition pos, IPPort user) {
        if ((!user.equals(MessageParser.getMyExternalIp())) && (!user.equals(MessageParser.getMyInternalIp())))
            if (!getMyNeighbours().contains(user)) {
                getMyNeighbours().add(user);
                if (getMyNeighbours().size() > Constants.numberOfCachedPeers) {
                    getMyNeighbours().poll();
                }
            }
    }

    public static void addParent(DomainTreeNodePosition pos, IPPort user) {
        if ((!user.equals(MessageParser.getMyExternalIp())) && (!user.equals(MessageParser.getMyInternalIp()))) {
            if (!getMyParents().contains(user)) {
                getMyParents().add(user);
                if (getMyParents().size() > Constants.numberOfCachedPeers) {
                    getMyParents().poll();
                }
            }
        }
    }

    public static void removeNeighbour(IPPort user)
    {
        getMyNeighbours().remove(user);

    }

    public static IPPort[] getDomainsRoots()
    {
        if (getDomainsTree().domainsRoots.size() == 0)
            return null;
        return getDomainsTree().domainsRoots.toArray(new IPPort[0]);
    }

    public static void addToDomainsRoots(IPPort user)
    {
        if ((!user.equals(MessageParser.getMyExternalIp())) && (!user.equals(MessageParser.getMyInternalIp()))) {
            if (!getDomainsTree().domainsRoots.contains(user)) {
                getDomainsTree().domainsRoots.add(user);
                if (getDomainsTree().domainsRoots.size() >= Constants.numberOfCachedPeers) {
                    getDomainsTree().domainsRoots.poll();
                }
            }
        }
    }


    public static DomainsTree getDomainsTree()
    {
        if (domainsTree == null)
        {
            domainsTree = new DomainsTree();
        }
        return domainsTree;
    }

    private static IPPort getDomainsRoot()
    {
            IPPort domainsRoot=null;

            while (getDomainsTree().domainsRoots.size()!=0)
            {
                domainsRoot = getDomainsTree().domainsRoots.poll();
                if (FreeWebUser.connect(domainsRoot)) {
                    addToDomainsRoots(domainsRoot);
                    break;
                }
                else
                    domainsRoot = null;
            }
            int ii=0;
            if (getDomainsTree().domainsRoots.size()<Constants.numberOfCachedPeers || domainsRoot==null)
            while (domainsRoot==null && ii<Constants.numberOfDomainTrys) {
                try {
                    ii++;
                    IPPort user = Network.getRandomUser();
                    byte[] rec = Utilities.receiveMailBox(Utilities.createDatagramPacket(Utilities.intToByteArray(MessageParser.GETDOMAINSROOT), user), user, MessageParser.getParser().getDomainsRootRetreiveBox());
                    IPPort[] i = Utilities.byteArrayToIPPortArray(rec);
                    if (i != null) {
                        if (i.length > 0)
                            for (IPPort ipp : i)
                                if (FreeWebUser.connectVia(user, ipp)) {
                                    addToDomainsRoots(ipp);
                                    domainsRoot = ipp;
                                }
                    }
                } catch (Exception e)
                {}
            }
            return domainsRoot;

    }

    public static IPPort[] getDomainRoots(byte[] hash)
    {
        DomainTreeNodesInfo dti = getDomainsTree().getNodesInfo(new DomainTreeNodePosition(Constants.hashSize*8,hash));

        if (dti!=null)
        {
            if (dti.getPosition().getLevel()==Constants.hashSize*8)
            {
                return dti.getNodes();
            }
        }
        return null;
    }

    public static IPPort[] getDomainRoots(String domain) //TODO multithreaded is more efficient
    {
        byte[] hash = Utilities.toSHA256(domain);

        return getDomainRoots(hash);
    }

    public static DomainTreeNodesInfo getInfo(byte[] path) {
        if (!Utilities.arePathsSame(path,getMyDomainTreePath(),getMyDomainTreeLevel()))
        {
            return null;
        }
        IPPort[] children = getChildren(path);
        if (children.length==0)
        {
            return getData(path);
        }
        else
        {
            return new DomainTreeNodesInfo(new DomainTreeNodePosition(getMyDomainTreeLevel()+Constants.branchingLevel,path),children);
        }
    }

    private void becomeChild()
    {
        myDomainTreeLevel += Constants.branchingLevel;
    }


    public static void printNodeInfo()
    {
        System.out.println("My branch level is:" + getMyDomainTreeLevel());
        if (getMyDomainTreeLevel()>0)
            System.out.println("My branch value is:" + Utilities.getBranchValue(getMyDomainTreePath(),getMyDomainTreeLevel()-Constants.branchingLevel));


        System.out.println("Deca:");

        for (int i=0;i<Constants.numberOfChildren;i++) {
            boolean b1=true;
            for (IPPort b : getMyChildren()[i]) {
                if (b1)
                {
                    System.out.println("Deca [" + i + "]:");
                    b1=false;
                }
                System.out.println(b.toString());
            }
        }
        System.out.println("Komsije:");

        for (IPPort b : getMyNeighbours()) {
            System.out.println(b.toString());
        }

        System.out.println("Roditelji:");

        for (IPPort b : getMyParents()) {
            System.out.println(b.toString());
        }

    }

    public static void setInfo(DomainTreeNodesInfo data) {
        if (Utilities.arePathsSame(data.getPosition().getPath(),getMyDomainTreePath(),getMyDomainTreeLevel()))
            if (data.getPosition().getLevel()==Constants.hashSize*8 && data.getPosition().getPath().length== Constants.hashSize)
            {//Domain Info
                    int branchValue = Utilities.getBranchValue(data.getPosition().getPath(), getMyDomainTreeLevel());
                    if (getMyChildren()[branchValue].size()==0) {

                        if (getDomainsInfo().size() * 32 * 4 > Constants.maxSizePerNode) {
                            System.out.println("OVERFLOW!");
                            if (Utilities.getBranchValue(getMyDomainTreePath(),getMyDomainTreeLevel()+Constants.branchingLevel)%2==0) {
                                System.out.println("BECAME CHILD!");

                                getDomainsTree().myParents = getMyNeighbours();

                                getDomainsTree().myNeighbours = getMyChildren()[Utilities.getBranchValue(getMyDomainTreePath(),getMyDomainTreeLevel())];

                                getDomainsTree().becomeChild();


                                for (CustomByteArray b : getDomainsInfo().keySet()) {
                                    if (!Utilities.arePathsSame(getDomainsTree().myDomainTreePath,b.getArray(),getDomainsTree().myDomainTreeLevel))
                                    {
                                        getDomainsInfo().remove(b);
                                    }
                                }




                                getDomainsTree().timer=30000;
                            }
                        }
                        CustomByteArray key = Utilities.byteArraytoCustomByteArray(data.getPosition().getPath());

                        for (IPPort node : data.getNodes()) {
                            if (DomainsSecret.verify(data.getPosition().getPath(), node)) {
                                if (getDomainsInfo().putIfAbsent(key, new ConcurrentLinkedQueue<IPPort>())==null)
                                {
                                    System.out.println("ADVERTISING TO NEIGHBOURS");
                                    for (IPPort neighbour :
                                            getMyNeighbours()) {
                                        try {
                                            DatagramPacket dp = Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(MessageParser.SUBMITDOMAINTREENODEINFO),Utilities.domainTreeNodesInfoToByteArray(data)),neighbour);
                                            NetworkListener.getServerSocket().send(dp);
                                        } catch (Exception e) {

                                        }
                                    }
                                }

                                ConcurrentLinkedQueue<IPPort> queue = getDomainsInfo().get(key);

                                if (!queue.contains(node)) {
                                    queue.add(node);
                                    if (queue.size() > Constants.numberOfCachedPeers) {
                                        queue.poll();
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        try {
                            for (IPPort child: getMyChildren()[branchValue]) {
                                DatagramPacket dp = Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(MessageParser.SUBMITDOMAINTREENODEINFO), Utilities.domainTreeNodesInfoToByteArray(data)), child);
                                NetworkListener.getServerSocket().send(dp);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
            }
            else
            {// Tree Node Info

                if ((data.getPosition().getLevel() == getMyDomainTreeLevel() - Constants.branchingLevel)) {
                    for (IPPort node: data.getNodes())
                        if ((!node.equals(MessageParser.getMyExternalIp())) && (!node.equals(MessageParser.getMyInternalIp())))
                            addParent(data.getPosition(), node);
                }

                if ((data.getPosition().getLevel() == getMyDomainTreeLevel())) {
                    for (IPPort node: data.getNodes())
                        addNeighbour(data.getPosition(), node);
                }
                if ((data.getPosition().getLevel() == getMyDomainTreeLevel() + Constants.branchingLevel)) {
                    for (IPPort node : data.getNodes()) {
                        System.out.println("CHILD ADDED!");
                        if ((!node.equals(MessageParser.getMyExternalIp())) && (!node.equals(MessageParser.getMyInternalIp())))
                            addChild(data.getPosition(), node);
                    }
                }

            }
    }

    public static void setInfo(DomainTreeNodeInfo data) {
        IPPort[] ipp = new IPPort[1];
        ipp[0] = data.getNode();
        DomainTreeNodesInfo dtni = new DomainTreeNodesInfo(data.getPosition(),ipp);
        setInfo(dtni);
    }

    public static DomainTreeNodesInfo getData(byte[] path)
    {
        if (!Utilities.arePathsSame(path,getMyDomainTreePath(),getMyDomainTreeLevel()))
        {
            return null;
        }

        CustomByteArray key = Utilities.byteArraytoCustomByteArray(path);

        ConcurrentLinkedQueue<IPPort> queue = getDomainsInfo().get(key);
        if (queue==null || queue.size()==0)
        {
            return null;
        }
        return new DomainTreeNodesInfo(new DomainTreeNodePosition(Constants.hashSize*8, path),queue.toArray(new IPPort[0]));
    }

    public static ConcurrentLinkedQueue<IPPort> getMyParents() {
        return getDomainsTree().myParents;
    }

    public static void removeParent(IPPort user) {
        getMyParents().remove(user);
    }



    @Override
    public void action() {
        if (sem.tryAcquire()) {
            if (actionSwitch < 5) {
                System.out.println("REFRESH");
                refreshDomainTreePosition();
            } else {
                System.out.println("CHANGE");
                changeDomainTreePosition();
                actionSwitch = 0;
            }
            actionSwitch++;
            sem.release();
        }
    }



    @Override
    public boolean tick(int i) {
        timer-=i;
        if (timer<=0)
        {
            timer = Constants.refreshDomainTreeTimeout;
            return true;
        }
        return false;
    }


    public static void advertise(byte[] domainHash, IPPort myExternalIp) {
        IPPort[] ipp = new IPPort[1];
        ipp[0] = myExternalIp;
        DomainTreeNodesInfo info = new DomainTreeNodesInfo(new DomainTreeNodePosition(Constants.hashSize*8,domainHash),ipp);
        int size=0;
        for (IPPort node : getDomainsTree().findRoots().getNodes())
        {
            byte[] send = Utilities.add(Utilities.intToByteArray(MessageParser.SUBMITDOMAINTREENODEINFO), Utilities.domainTreeNodesInfoToByteArray(info));
            try {
                DatagramPacket dp = Utilities.createDatagramPacket(send, node);
                NetworkListener.getServerSocket().send(dp);
            }catch (Exception e){

            }
            size+= send.length;
            if (size>1024)
            {
                size-=1024;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
        setInfo(info);
    }
}
