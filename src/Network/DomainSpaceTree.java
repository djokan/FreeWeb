package Network;

import DataStructures.*;
import Network.NetworkSynchronizer.*;
import DataStructures.IPPort;
import Utilities.*;

import javax.crypto.Cipher;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;


public class DomainSpaceTree implements RepeatingTask {
    private byte[] domainHash; // TODO what if branch levels arent same?

    public byte[] getDomainHash() {
        return domainHash;
    }

    private ConcurrentLinkedQueue<IPPort> domainRoots;

    private static Map<CustomByteArray,DomainSpaceTree> domains;

    public int getMyDomainSpaceTreeLevel() {
        return this.myDomainSpaceTreeLevel;
    }

    public Map<CustomByteArray, DomainSpaceTreeData> getDomainsData() {
        return this.domainsInfo;
    }

    static {
        domains = new ConcurrentHashMap<CustomByteArray, DomainSpaceTree>();
    }

    public Map<CustomByteArray, DomainSpaceTreeData> domainsInfo;
    private int myDomainSpaceTreeLevel;
    private byte[] myDomainSpaceTreePath;
    private int actionSwitch=0; // 0-4= refresh  5= change
    private int timer=0;
    private ConcurrentLinkedQueue<IPPort> myParents;
    private ConcurrentLinkedQueue<IPPort> myNeighbours;
    private ConcurrentLinkedQueue<IPPort>[] myChildren;
    private Semaphore sem;
    private DomainSpaceTreeSettings settings;
    private FileInfo fileInfo;
    private int numberOfChildren;

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public static DomainSpaceTree getDomainSpaceTree(CustomByteArray domain)
    {
        return domains.get(domain);
    }


    public DomainSpaceTree(byte[] domainHash,DomainSpaceTreeSettings settings) throws Exception
    {
        if (domainHash.length!=Constants.hashSize)
        {
            throw new Exception("Hashsize is not valid");
        }
        sem = new Semaphore(1);
        this.settings = settings;
        this.numberOfChildren = 1<<(settings.branchingLevel);
        this.domainHash = domainHash;
        domainRoots = new ConcurrentLinkedQueue<>();
        myParents = new ConcurrentLinkedQueue<>();
        myNeighbours = new ConcurrentLinkedQueue<>();
        myChildren = new ConcurrentLinkedQueue[1<<settings.branchingLevel];
        domainsInfo = new ConcurrentHashMap<CustomByteArray, DomainSpaceTreeData>();
        for (int i=0;i<(1<<settings.branchingLevel);i++)
        {
            myChildren[i] = new ConcurrentLinkedQueue<>();
        }
        myDomainSpaceTreeLevel = 0;
        myDomainSpaceTreePath = new byte[Constants.hashSize];
        ThreadLocalRandom.current().nextBytes(myDomainSpaceTreePath);
        domains.putIfAbsent(Utilities.byteArraytoCustomByteArray(domainHash),this);
        NetworkSynchronizer.addToTasks(this);
        fileInfo = null;
    }



    // utility function for getNodesInfo
    private IPPort startSettingsforGetNodesInfo(DomainTreeNodePosition d, DomainSpaceTreeInfo iii)
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
            if ((getMyDomainSpaceTreeLevel()<=d.getLevel()) && Utilities.arePathsSame (d.getPath(), getMyDomainSpaceTreePath(), getMyDomainSpaceTreeLevel()) && (getParents().length !=0 || getNeighbours().length != 0 || b)) {
                if (getChildren(d.getPath()).length == 0)
                    return null;
                domainsRoot = getChildren(d.getPath())[ThreadLocalRandom.current().nextInt(0, getChildren(d.getPath()).length)];
            } else {

                IPPort[] tempipp = DomainsTree.getDomainRoots(domainHash);
                tempipp = Utilities.eraseMyIp(tempipp);
                if (tempipp==null || tempipp.length==0)
                {
                    domainsRoot = null;
                }
                else {

                    domainsRoot = tempipp[ThreadLocalRandom.current().nextInt(0, tempipp.length)];
                }
            }
        }
        else
        {
            if (iii.getData().getType()!=4)
            {
                domainsRoot=null;
            }
            else {
                IPPort[] tempipp = Utilities.byteArrayToIPPortArray(iii.getData().getData());
                if (tempipp.length==0)
                {
                    domainsRoot = null;
                }
                else {
                    domainsRoot = tempipp[ThreadLocalRandom.current().nextInt(0, tempipp.length)];
                }
            }
        }

        return domainsRoot;
    }

    private DomainSpaceTreeInfo getNodesInfo(DomainTreeNodePosition d  ,DomainSpaceTreeInfo iii) // finds nodes that host given position on tree starting from second argument
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
        DomainSpaceTreeInfo lastlastdti = null;
        DomainSpaceTreeInfo lastdti = null;
        DomainSpaceTreeInfo dti=null;

        if (d.getLevel()==0)
        {
            IPPort[] ipp = DomainsTree.getDomainRoots(domainHash);
            if (ipp==null)
            {
                ipp = new IPPort[0];
            }
            DomainSpaceTreeData dstd = new DomainSpaceTreeData(Utilities.IPPortArrayToByteArray(ipp),4);
            DomainSpaceTreeInfo dsti = new DomainSpaceTreeInfo(d,dstd);
            return dsti;
        }

        for (int i=0;i<Constants.numberOfDomainTrys;i++) {
            byte[] receive;
            try {
                byte[] send = Utilities.add(Utilities.intToByteArray(MessageParser.GETDOMAINSPACETREENODEINFO), Utilities.add(domainHash,hash));

                receive = Utilities.receiveMailBox(Utilities.createDatagramPacket(send , user), MessageParser.getDomainSpaceTreeInfoBox());
                dti = Utilities.byteArrayToDomainSpaceTreeInfo(receive);

                if (dti!=null && dti.getData()!=null && dti.getData().getType()==4)
                    dti.getData().setData(Utilities.IPPortArrayToByteArray(Utilities.eraseMyIp(Utilities.byteArrayToIPPortArray(dti.getData().getData()))));

                if (dti!=null && dti.getData()!=null && dti.getData().getType()==5) {
                    int myNum = 0;
                    IPPort a = Utilities.byteArrayToIPPort(dti.getData().getData());

                    if (a.equals(MessageParser.getMyInternalIp()) || a.equals(MessageParser.getMyExternalIp())) {
                        dti.getData().setData(Utilities.IPPortArrayToByteArray(new IPPort[0]));
                        dti.getData().setType(4);
                    }
                }


            } catch (Exception ea) {
                dti = null;
                ea.printStackTrace();
            }
            if (dti !=null && dti.getPosition().getLevel() == d.getLevel() && dti.getData()!=null && dti.getData().getData().length>0 && Utilities.arePathsSame(hash,dti.getPosition().getPath(),d.getLevel())) {
                System.out.println("SFOUND!");
                return dti;
            }

            if ((dti == null || dti.getData()==null || dti.getData().getData()==null || dti.getData().getData().length == 0  || dti.getPosition().getLevel()>d.getLevel() || dti.getPosition().getLevel()%settings.branchingLevel!=0 || (!(Utilities.arePathsSame(dti.getPosition().getPath(),d.getPath(),d.getLevel()))) || dti.getPosition().getLevel()<=maxlevel || dti.getData().getType()<4) && user.equals( domainsRoot)) {
                System.out.println("SFIRST FAIL!" + user.toString());
                removeFromDomainRoots(user);

                try{
                    MessageParser.requestdomainspacetreeposition(Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(MessageParser.REQUESTDOMAINTREEPOSITION),domainHash),user));

                    NetworkListener.getServerSocket().send(Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(MessageParser.REQUESTDOMAINSPACETREEPOSITION),domainHash),user));
                }
                catch (Exception e)
                {}
                domainsRoot = startSettingsforGetNodesInfo(d,iii);
                while (domainsRoot==null && i<Constants.numberOfDomainTrys)
                {
                    i++;
                    domainsRoot = startSettingsforGetNodesInfo(d,iii);
                }
                user = domainsRoot;
            } else {
                if (dti == null || dti.getData()==null || dti.getData().getData()==null || dti.getData().getData().length == 0 || dti.getPosition().getLevel()>d.getLevel() || dti.getPosition().getLevel()%settings.branchingLevel!=0 || (!(Utilities.arePathsSame(dti.getPosition().getPath(),d.getPath(),d.getLevel()))) || dti.getPosition().getLevel()<=maxlevel || dti.getData().getType()<4) {
                    System.out.println("SFAIL!" + user.toString());
                    try{
                        MessageParser.requestdomainspacetreeposition(Utilities.createDatagramPacket("a".getBytes(),user));
                        NetworkListener.getServerSocket().send(Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(MessageParser.REQUESTDOMAINSPACETREEPOSITION),domainHash),user));
                    }
                    catch (Exception e)
                    {}
                    dti = lastdti;
                    if (dti.getData().getType()==4)
                    {
                        IPPort[] tempipp = Utilities.byteArrayToIPPortArray(dti.getData().getData());
                        user = tempipp[ThreadLocalRandom.current().nextInt(0, tempipp.length)];
                    }

                    if (dti.getData().getType()==5)
                    {
                        user = Utilities.byteArrayToIPPort(dti.getData().getData());
                    }

                } else {
                    System.out.println("SLEVEL DEEPER!" + user.toString());
                    lastdti = dti;
                    maxlevel= dti.getPosition().getLevel();
                    if (dti.getData().getType()==4)
                    {
                        IPPort[] tempipp = Utilities.byteArrayToIPPortArray(dti.getData().getData());
                        user = tempipp[ThreadLocalRandom.current().nextInt(0, tempipp.length)];
                    }

                    if (dti.getData().getType()==5)
                    {
                        user = Utilities.byteArrayToIPPort(dti.getData().getData());
                    }

                }
            }
        }
        if (dti==null)
        {
            if (lastdti==null)
            {
                IPPort[] tempipp = DomainsTree.getDomainRoots(domainHash);

                return new DomainSpaceTreeInfo(new DomainTreeNodePosition(0,d.getPath()),new DomainSpaceTreeData(Utilities.IPPortArrayToByteArray(tempipp),4));
            }
            return lastdti;
        }
        return dti;
    }

    private void removeFromDomainRoots(IPPort user) {
        if ((!user.equals(MessageParser.getMyExternalIp())) && (!user.equals(MessageParser.getMyInternalIp()))) {
            this.domainRoots.remove(user);
        }
    }

    private DomainSpaceTreeInfo getNodesInfo(DomainTreeNodePosition d) // finds nodes that host given position on tree starting from root
    {
        return getNodesInfo(d,null);
    }

    private void changeDomainSpaceTreePosition() {//TODO test


        System.out.println("SCHG");

        byte[] hash = new byte[Constants.hashSize];

        ThreadLocalRandom.current().nextBytes(hash);

        DomainSpaceTreeInfo dti = getNodesInfo(new DomainTreeNodePosition(256,hash));

        int maxlevel ;



        if (dti!=null )
            maxlevel= dti.getPosition().getLevel() - dti.getPosition().getLevel()%settings.branchingLevel;
        else
            maxlevel=0;

        int myLevel = ThreadLocalRandom.current().nextInt(0,maxlevel/settings.branchingLevel+1)*settings.branchingLevel;

        myDomainSpaceTreeLevel = myLevel;
        myDomainSpaceTreePath = hash;

        getMyNeighbours().clear();
        getMyParents().clear();

        for (int i=0;i<numberOfChildren;i++)
        {
            try {
                myChildren[i].clear();
            }catch (Exception ignored)
            {
                break;
            }

        }

        refreshDomainSpaceTreePosition();
    }


    void refreshDomainSpaceTreePosition() // finds neighbours, parents and children
    {//TODO test
        System.out.println("REFss");

        if (MessageParser.getMyExternalIp()==null)
            try {
                Network.getRandomUser();
            } catch (Exception e) {
                e.printStackTrace();
            }

        if (getMyDomainSpaceTreeLevel()==0 && MessageParser.getMyExternalIp()!=null) {
            DomainsTree.advertise(domainHash, MessageParser.getMyExternalIp());
        }

        /*try {

            byte[] settbyt = getDomainFile(Utilities.toSHA256("config.cfg"), domainHash); //TODO DEBUG

            DomainSpaceTree.DomainSpaceTreeSettings newsettings = Utilities.ByteArrayToDomainSpaceTreeSettings(Arrays.copyOfRange(settbyt, Constants.hashSize, settbyt.length));

            if (!this.settings.equals(newsettings))
            {
                this.settings = newsettings;
                myChildren = new ConcurrentLinkedQueue[1<<settings.branchingLevel];
                for (int i=0;i<(1<<settings.branchingLevel);i++)
                {
                    myChildren[i] = new ConcurrentLinkedQueue<>();
                }
            }

        }catch (Exception e)
        {

        }*/

        DomainSpaceTreeInfo dti;
        if (myDomainSpaceTreeLevel >=settings.branchingLevel*2)
        {
//            System.out.println("OVO NE SME DA SE DESI SA MALOM MREZOM!");
//            System.exit(1);
            dti = getNodesInfo(new DomainTreeNodePosition(myDomainSpaceTreeLevel -settings.branchingLevel, myDomainSpaceTreePath));
            if (dti.getPosition().getLevel()!= myDomainSpaceTreeLevel -settings.branchingLevel|| dti.getData()==null || dti.getData().getData()==null  || dti.getData().getData().length==0 || dti.getData().getType()!=4)
                return;
            while (true) {
                try {
                    myParents.remove();
                }catch (Exception ignored)
                {
                    break;
                }
            }

            IPPort[] ipparray = Utilities.byteArrayToIPPortArray(dti.getData().getData());

            for (IPPort node : ipparray) {
                boolean skip=false;
                IPPort[] ipp = new IPPort[1];
                DomainSpaceTreeInfo dtni;
                if (Utilities.isExternal(node)) {
                    if (MessageParser.getMyExternalIp()==null)
                        skip=true;
                    ipp[0] =  MessageParser.getMyExternalIp();
                    dtni = new DomainSpaceTreeInfo(new DomainTreeNodePosition(this.getMyDomainSpaceTreeLevel(), this.getMyDomainSpaceTreePath()),new DomainSpaceTreeData(Utilities.IPPortArrayToByteArray(ipp),4));
                }
                else {
                    if (MessageParser.getMyInternalIp()==null)
                        skip=true;
                    ipp[0]=MessageParser.getMyInternalIp();
                    dtni = new DomainSpaceTreeInfo(new DomainTreeNodePosition(this.getMyDomainSpaceTreeLevel(), this.getMyDomainSpaceTreePath()),new DomainSpaceTreeData(Utilities.IPPortArrayToByteArray(ipp),4));
                }
                if (!skip)
                    try {
                        DatagramPacket dtp = Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(MessageParser.SUBMITDOMAINSPACETREENODEINFO),Utilities.add(domainHash,Utilities.domainSpaceTreeInfoToByteArray(dtni))),node);
                        NetworkListener.getServerSocket().send(dtp);
                    } catch (Exception e) {
                    }

                addParent(dti.getPosition(),node);
            }
            dti = getNodesInfo(new DomainTreeNodePosition(myDomainSpaceTreeLevel, myDomainSpaceTreePath),dti);

            if (dti.getPosition().getLevel()!= myDomainSpaceTreeLevel || dti.getData()==null || dti.getData().getData()==null  || dti.getData().getData().length==0 || dti.getData().getType()!=4)
                return;

            while (true) {
                try {
                    myNeighbours.remove();
                }catch (Exception ignored)
                {
                    break;
                }
            }

            ipparray = Utilities.byteArrayToIPPortArray(dti.getData().getData());

            for (IPPort node : ipparray) {
                addNeighbour(dti.getPosition(),node);
            }
            findChildrenAndDomains();
        }
        else if (myDomainSpaceTreeLevel ==settings.branchingLevel)
        {
            dti = findRoots();
            if ((dti.getPosition().getLevel()+settings.branchingLevel!= myDomainSpaceTreeLevel) || (dti.getData()==null) || (dti.getData().getData()==null) || dti.getData().getData().length==0 || dti.getData().getType()!=4)
                return;
            while (true) {
                try {
                    myParents.remove();
                }catch (Exception ignored)
                {
                    break;
                }
            }
            for (IPPort node :  Utilities.byteArrayToIPPortArray(dti.getData().getData())) {

                boolean skip=false;
                IPPort[] ipp = new IPPort[1];
                DomainSpaceTreeInfo dtni;
                if (Utilities.isExternal(node)) {
                    if (MessageParser.getMyExternalIp()==null)
                        skip=true;
                    ipp[0] =  MessageParser.getMyExternalIp();
                    dtni = new DomainSpaceTreeInfo(new DomainTreeNodePosition(getMyDomainSpaceTreeLevel(), getMyDomainSpaceTreePath()),new DomainSpaceTreeData(Utilities.IPPortArrayToByteArray(ipp),4));
                }
                else {
                    if (MessageParser.getMyInternalIp()==null)
                        skip=true;
                    ipp[0]=MessageParser.getMyInternalIp();
                    dtni = new DomainSpaceTreeInfo(new DomainTreeNodePosition(getMyDomainSpaceTreeLevel(), getMyDomainSpaceTreePath()), new DomainSpaceTreeData(Utilities.IPPortArrayToByteArray(ipp),4));
                }
                if (!skip)
                    try {
                        DatagramPacket dtp = Utilities.createDatagramPacket(Utilities.add(Utilities.add(Utilities.intToByteArray(MessageParser.SUBMITDOMAINSPACETREENODEINFO),domainHash),Utilities.domainSpaceTreeInfoToByteArray(dtni)),node);
                        NetworkListener.getServerSocket().send(dtp);
                    } catch (Exception e) {
                    }

                addParent(dti.getPosition(),node);
            }
            dti = getNodesInfo(new DomainTreeNodePosition(myDomainSpaceTreeLevel, myDomainSpaceTreePath),dti);
            if ((dti.getPosition().getLevel()!= myDomainSpaceTreeLevel) || (dti.getData()==null) || (dti.getData().getData()==null) || dti.getData().getData().length==0 || dti.getData().getType()!=4)
                return;
            while (true) {
                try {
                    myNeighbours.remove();
                }catch (Exception ignored)
                {
                    break;
                }
            }
            for (IPPort node : Utilities.byteArrayToIPPortArray(dti.getData().getData())) {
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
            if ((dti.getPosition().getLevel()!= myDomainSpaceTreeLevel) || (dti.getData()==null) || (dti.getData().getData()==null) || dti.getData().getData().length==0 || dti.getData().getType()!=4)
                return;
            while (true) {
                try {
                    myNeighbours.remove();
                }catch (Exception ignored)
                {
                    break;
                }
            }

            for (IPPort node : Utilities.byteArrayToIPPortArray(dti.getData().getData())) {
                addNeighbour(dti.getPosition(),node);
            }
            findChildrenAndDomains();
        }

        System.out.println("SLVL: " + myDomainSpaceTreeLevel);
        System.out.println("SPATH: " + new String(myDomainSpaceTreePath));
    }

    public static byte[] getDomainFile(byte[] url, byte[] host) {
        byte[] sumfile = new byte[0];
        DomainSpaceTree dst = getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(host));
        if (dst==null)
        {
            getFileInfo(host);

            dst = getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(host));
        }
        if (dst==null)
            return null;
        FileInfo fi = dst.fileInfo;
        if (fi==null)
            getFileInfo(host);
        fi = dst.fileInfo;
        if (fi==null)
            return null;
        byte[] nextHash = fi.getHashOf(url);
        byte[] file;
        int trys;
        do
        {
            trys=0;
            byte[] tryy;
            do {
                if (nextHash==null)
                    return null;
                file = getDomainData(nextHash,host);
                trys++;
                tryy = Utilities.toSHA256(file);
            } while ((file==null || !Arrays.equals(tryy,nextHash)) && trys<Constants.numberOfDomainTrys);

            if (trys==Constants.numberOfDomainTrys)
            {
                return null;
            }
            nextHash = Arrays.copyOfRange(file,file.length-Constants.hashSize,file.length);
            sumfile = Utilities.add(sumfile,Arrays.copyOfRange(file,0,file.length-Constants.hashSize));
        }while (!Arrays.equals(nextHash,Constants.nullHash));
        sumfile = Arrays.copyOfRange(sumfile,Constants.intSize,sumfile.length);
        return sumfile;
    }

    public static byte[] getPrivateFile(byte[] url, byte[] host) {
        byte[] sumfile = new byte[0];
        DomainSpaceTree dst = getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(host));
        if (dst==null)
        {
            getFileInfo(host);

            dst = getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(host));
        }
        if (dst==null)
            return null;

        byte[] nextHash = url;
        byte[] file;
        int trys;
        do
        {
            trys=0;
            byte[] tryy;
            do {
                if (nextHash==null)
                    return null;
                file = getDomainData(nextHash,host);
                trys++;
                tryy = Utilities.toSHA256(file);
            } while ((file==null || !Arrays.equals(tryy,nextHash)) && trys<Constants.numberOfDomainTrys);

            if (trys==Constants.numberOfDomainTrys)
            {
                return null;
            }
            nextHash = Arrays.copyOfRange(file,file.length-Constants.hashSize,file.length);
            sumfile = Utilities.add(sumfile,Arrays.copyOfRange(file,0,file.length-Constants.hashSize));
        }while (!Arrays.equals(nextHash,Constants.nullHash));
        sumfile = Arrays.copyOfRange(sumfile,Constants.intSize,sumfile.length);
        return sumfile;
    }



    DomainSpaceTreeInfo findRoots()//TODO test
    {
        IPPort[] tempipp = DomainsTree.getDomainRoots(domainHash);

        return new DomainSpaceTreeInfo(new DomainTreeNodePosition(0,Utilities.toSHA256("")),new DomainSpaceTreeData(Utilities.IPPortArrayToByteArray(tempipp),4));
    }

    void findChildrenAndDomains()//TODO test
    {
        if (getNeighbours().length==0)
            return;
        IPPort randomnei=null;
        DomainSpaceTreeInfo dti=null;
        IPPort[] neigh = getNeighbours();

        try {
            System.out.println();
            NetworkListener.getServerSocket().send(Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(MessageParser.GETDOMAINSPACETREEDATA),domainHash), neigh[ThreadLocalRandom.current().nextInt(0, neigh.length)]));
        }
        catch (Exception e)
        {}


        for (int p=0;p<1<<settings.branchingLevel;p++)
        {
            byte[] receive;
            for (int i=0;i<Constants.numberOfDomainTrys;i++) {
                try {
                    byte[] cp = Utilities.makeChildPath(myDomainSpaceTreePath, myDomainSpaceTreeLevel, p);
                    byte[] send = Utilities.add(Utilities.intToByteArray(MessageParser.GETDOMAINSPACETREENODEINFO), Utilities.add(domainHash,cp));
                    randomnei = neigh[ThreadLocalRandom.current().nextInt(0, neigh.length)];
                    receive = Utilities.receiveMailBox(Utilities.createDatagramPacket(send, randomnei), MessageParser.getDomainSpaceTreeInfoBox());
                    dti = Utilities.byteArrayToDomainSpaceTreeInfo(receive);
                    if (dti != null && dti.getData()!= null && dti.getData().getData()!=null && dti.getData().getData().length>0 &&  dti.getData().getType()==4)
                        break;
                } catch (Exception e) {
                }
            }

            if (dti != null && dti.getData()!= null && dti.getData().getData()!=null && dti.getData().getData().length>0 &&  dti.getData().getType()==4)
            {
                if (dti.getPosition().getLevel()== getMyDomainSpaceTreeLevel()+settings.branchingLevel)
                {
                    for (IPPort node : Utilities.byteArrayToIPPortArray(dti.getData().getData())) {
                        addChild(dti.getPosition(),node);
                    }
                }
                else if (dti.getPosition().getLevel()==Constants.hashSize*8)
                {
                    for (IPPort node : Utilities.byteArrayToIPPortArray(dti.getData().getData())) {
                        setInfo(dti);
                    }
                }
            }
        }
    }



    public ConcurrentLinkedQueue<IPPort>[] getMyChildren() {
        return this.myChildren;
    }

    public byte[] getMyDomainSpaceTreePath() {
        return this.myDomainSpaceTreePath;
    }

    public void addChild(DomainTreeNodePosition pos, IPPort user) {

        if ((!user.equals(MessageParser.getMyExternalIp())) && (!user.equals(MessageParser.getMyInternalIp()))) {
            int branchValue = Utilities.getBranchValue(pos.getPath(), getMyDomainSpaceTreeLevel());


            for (CustomByteArray b : getDomainsData().keySet()) {
                if (Utilities.getBranchValue(b.getArray(), getMyDomainSpaceTreeLevel()) == branchValue) {
                    System.out.println("IZBACIO SAM DIJETETOVO!");
                    getDomainsData().remove(b);
                }
            }

            if (!getMyChildren()[branchValue].contains(user)) {
                getMyChildren()[branchValue].add(user);
                if (getMyChildren()[branchValue].size() > settings.numberOfCachedPeers) {
                    getMyChildren()[branchValue].poll();
                }
            }
        }
    }

    public void removeChild(IPPort user)
    {
        for (int i=0;i<numberOfChildren;i++)
            getMyChildren()[i].remove(user);

    }

    public IPPort[] getChildren(byte[] path)
    {
        return getMyChildren()[Utilities.getBranchValue(path, getMyDomainSpaceTreeLevel())].toArray(new IPPort[0]);
    }

    public IPPort[] getNeighbours()
    {
        return getMyNeighbours().toArray(new IPPort[0]);
    }

    public IPPort[] getParents()
    {
        return getMyParents().toArray(new IPPort[0]);
    }


    public ConcurrentLinkedQueue<IPPort> getMyNeighbours() {
        return this.myNeighbours;
    }

    public void addNeighbour(DomainTreeNodePosition pos, IPPort user) {
        if ((!user.equals(MessageParser.getMyExternalIp())) && (!user.equals(MessageParser.getMyInternalIp())))
            if (!getMyNeighbours().contains(user)) {
                getMyNeighbours().add(user);
                if (getMyNeighbours().size() > settings.numberOfCachedPeers) {
                    getMyNeighbours().poll();
                }
            }
    }

    public void addParent(DomainTreeNodePosition pos, IPPort user) {
        if ((!user.equals(MessageParser.getMyExternalIp())) && (!user.equals(MessageParser.getMyInternalIp()))) {
            if (!getMyParents().contains(user)) {
                getMyParents().add(user);
                if (getMyParents().size() > settings.numberOfCachedPeers) {
                    getMyParents().poll();
                }
            }
        }
    }

    public void removeNeighbour(IPPort user)
    {
        getMyNeighbours().remove(user);

    }

    public IPPort[] getDomainRoots()
    {
        if (this.domainRoots.size() == 0)
            return null;
        return this.domainRoots.toArray(new IPPort[0]);
    }

    public void addToDomainRoots(IPPort user)
    {
        if ((!user.equals(MessageParser.getMyExternalIp())) && (!user.equals(MessageParser.getMyInternalIp()))) {
            if (!this.domainRoots.contains(user)) {
                this.domainRoots.add(user);
                if (this.domainRoots.size() >= settings.numberOfCachedPeers) {
                    this.domainRoots.poll();
                }
            }
        }
    }


    public static DomainSpaceTree createNewTree(byte[] domainHash,DomainSpaceTreeSettings settings) throws Exception {
        return new DomainSpaceTree(domainHash,settings);
    }

    public static byte[] getDomainFile(String url, String host)
    {
        return getDomainFile(Utilities.toSHA256(url),Utilities.toSHA256(host));

    }

    public static byte[] getDomainData(String url, String host)
    {
        byte[] url1 = Utilities.toSHA256(url);
        byte[] domain = Utilities.toSHA256(host);
        return getDomainData(url1,domain);
    }

    public static void getFileInfo(byte[] domain)
    {
        byte[] hash = getDomainData(Utilities.toSHA256("fileinfo"),domain);
        if (hash==null)
            return;
        hash = Utilities.cryptWithKey(hash,DomainsSecret.getPublicKey(domain), Cipher.DECRYPT_MODE);
        hash = Arrays.copyOfRange(hash,0,Constants.hashSize);
        byte[] sumfi = new byte[0];
        byte[] fi;
        do {
            fi = getDomainData(hash, domain);
            if (fi==null || !Arrays.equals(Utilities.toSHA256(fi),hash))
            {
                return;
            }
            hash = Arrays.copyOfRange(fi,fi.length-Constants.hashSize,fi.length);
            sumfi = Utilities.add(sumfi,Arrays.copyOfRange(fi,0,fi.length-Constants.hashSize));

        } while (!Arrays.equals(hash,Constants.nullHash));
        fi = Arrays.copyOfRange(sumfi,Constants.intSize,sumfi.length);
        if (fi==null)
            return;
        DomainSpaceTree dst = DomainSpaceTree.getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(domain));
        if (dst!=null)
        {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileInfo(fi);

            dst.fileInfo = fileInfo;
            dst.eraseInvalidData();

        }
    }

    private void eraseInvalidData() {
//        for (Map.Entry<CustomByteArray, DomainSpaceTreeData> data : domainsInfo.entrySet())
//        {
//            if (!fileInfo.doesFileExist(data.getKey().getArray()))
//            {
//                domainsInfo.remove(data.getKey());
//            }
//        }
    }

    public static byte[] getDomainData(byte[] data, byte[] domain)
    {
        DomainSpaceTree dst = getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(domain));

        if (!Arrays.equals(data,Utilities.toSHA256("fileinfo")) && dst == null)
        {
            getFileInfo(domain);
            getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(domain));
        }

        byte[] settb;
        if (dst==null)
        {
            int i=0;
            while (i<Constants.numberOfDomainTrys) {
                i++;
                IPPort[] ipp = DomainsTree.getDomainRoots(domain);

                if (ipp==null) continue;

                ipp = Utilities.eraseMyIp(ipp);

                if (ipp != null && ipp.length > 0) {
                    try {
                        settb = Utilities.receiveMailBox(Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(MessageParser.GETDOMAINSPACETREESETTINGS), domain), ipp[ThreadLocalRandom.current().nextInt(0, ipp.length)]), MessageParser.getDomainSpaceTreeSettingsBox());
                    } catch (Exception o) {
                        continue;
                    }
                    DomainSpaceTreeSettings sett = Utilities.ByteArrayToDomainSpaceTreeSettings(settb);// must be protected with public key
                    try {
                        dst = new DomainSpaceTree(domain, sett);
                        Thread.sleep(10000);
                        break;
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
            if (dst==null) {
                try {
                    dst = new DomainSpaceTree(domain,new DomainSpaceTreeSettings(2,60000,32,10024000,512));
                } catch (Exception e) {
                    return null;
                }
            }
        }

        if (data==null)
        {
            data=data;
        }

        DomainSpaceTreeInfo dsti = dst.getNodesInfo(new DomainTreeNodePosition(8*Constants.hashSize,data));

        if (dsti!=null && dsti.getData()!=null && dsti.getData().getData()!=null)
        {
            if (dsti.getPosition().getLevel()==Constants.hashSize*8)
            {
                return dsti.getData().getData();
            }
        }
        return null;
    }


    public DomainSpaceTreeInfo getInfo(byte[] path) {

        DomainSpaceTree dst = getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(domainHash));
        if (dst==null)
            return null;

        if (!Utilities.arePathsSame(path,dst.getMyDomainSpaceTreePath(),dst.getMyDomainSpaceTreeLevel()))
        {
            return null;
        }
        IPPort[] children = dst.getChildren(path);
        if (children.length==0)
        {
            return dst.getData(path);
        }
        else
        {
            DomainSpaceTreeData dstd = new DomainSpaceTreeData(Utilities.IPPortArrayToByteArray(children),4);
            return new DomainSpaceTreeInfo(new DomainTreeNodePosition(dst.getMyDomainSpaceTreeLevel()+dst.settings.branchingLevel,path),dstd);
        }
    }

    private void becomeChild()
    {
        myDomainSpaceTreeLevel += settings.branchingLevel;
    }


    public void printNodeInfo()
    {
        System.out.println("My branch level is:" + getMyDomainSpaceTreeLevel());
        if (getMyDomainSpaceTreeLevel()>0)
            System.out.println("My branch value is:" + Utilities.getBranchValue(getMyDomainSpaceTreePath(), getMyDomainSpaceTreeLevel()-settings.branchingLevel));


        System.out.println("Deca:");

        for (int i=0;i<numberOfChildren;i++) {
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

    private boolean shouldChange(DomainSpaceTreeData what, DomainSpaceTreeData with) {
        return true; //TODO not implemented
    }


    public void setInfo(DomainSpaceTreeInfo data) {
        if (Utilities.arePathsSame(data.getPosition().getPath(), getMyDomainSpaceTreePath(), getMyDomainSpaceTreeLevel()) && data.getPosition().getPath().length== Constants.hashSize)
            if (data.getPosition().getLevel()==Constants.hashSize*8 )
            {//Domain Info
                    int branchValue = Utilities.getBranchValue(data.getPosition().getPath(), getMyDomainSpaceTreeLevel());
                    if (getMyChildren()[branchValue].size()==0)
                    {
                        CustomByteArray key = Utilities.byteArraytoCustomByteArray(data.getPosition().getPath());

                        if (DomainsSecret.verifyData(data,this)) {
                            if (getDomainsData().putIfAbsent(key, data.getData())==null)
                            {
                                System.out.println("ADVERTISING TO NEIGHBOURS");
                                for (IPPort neighbour :
                                        getMyNeighbours()) {
                                    try {
                                        DatagramPacket dp = Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(MessageParser.SUBMITDOMAINSPACETREENODEINFO),Utilities.add(domainHash,Utilities.domainSpaceTreeInfoToByteArray(data))),neighbour);
                                        NetworkListener.getServerSocket().send(dp);
                                    } catch (Exception e) {

                                    }
                                }
                            }
                            else
                            {
                                DomainSpaceTreeData queue = getDomainsData().get(key);

                                if (Arrays.equals(key.getArray(),Utilities.toSHA256("fileinfo")))
                                {
                                    this.fileInfo=null;
                                }

                                if (data.getData().getType()==queue.getType()) {
                                    DomainSpaceTreeData merged = merge(queue, data.getData(),data.getPosition());
                                    if (!merged.equals(queue)) {
                                        getDomainsData().put(key, merged);

                                        if (Arrays.equals(key.getArray(),Utilities.toSHA256("fileinfo")))
                                            this.fileInfo = null;

                                        System.out.println("ADVERTISING TO NEIGHBOURS");
                                        for (IPPort neighbour :
                                                getMyNeighbours()) {
                                            try {
                                                DatagramPacket dp = Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(MessageParser.SUBMITDOMAINSPACETREENODEINFO),Utilities.add(domainHash,Utilities.domainSpaceTreeInfoToByteArray(data))),neighbour);
                                                NetworkListener.getServerSocket().send(dp);
                                            } catch (Exception e) {}
                                        }
                                    }
                                }
                            }

                            if (getDomainsDataSize()> settings.maxDataSizePerNode) {
                                System.out.println("OVERFLOW!");
                                if (Utilities.getBranchValue(getMyDomainSpaceTreePath(), getMyDomainSpaceTreeLevel()+settings.branchingLevel)%2==0) {
                                    System.out.println("BECAME CHILD!");

                                    this.myParents = getMyNeighbours();

                                    this.myNeighbours = getMyChildren()[Utilities.getBranchValue(getMyDomainSpaceTreePath(), getMyDomainSpaceTreeLevel())];

                                    this.becomeChild();

                                    for (CustomByteArray b : getDomainsData().keySet()) {
                                        if (!Utilities.arePathsSame(this.myDomainSpaceTreePath,b.getArray(),this.myDomainSpaceTreeLevel))
                                        {
                                            getDomainsData().remove(b);
                                        }
                                    }
                                    this.timer=30000;
                                }
                            }


                            if (data.getData().getType()==0) {

                                DomainSpaceTreeData dasta = domainsInfo.get(key);
                                if (!Arrays.equals(key.getArray(),Utilities.toSHA256("fileinfo")))
                                {
                                    if (!Arrays.equals(Utilities.toSHA256(dasta.getData()),key.getArray()))
                                    {
                                        System.out.println("LOSEEEEEEEEEEEEEEEEEEEEE");
                                    }
                                }
                            }



                        }
                    }
                    else
                    {
                        try {
                            for (IPPort child: getMyChildren()[branchValue]) {
                                DatagramPacket dp = Utilities.createDatagramPacket(Utilities.add(Utilities.intToByteArray(MessageParser.SUBMITDOMAINSPACETREENODEINFO), Utilities.add(domainHash,Utilities.domainSpaceTreeInfoToByteArray(data))), child);
                                NetworkListener.getServerSocket().send(dp);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
            }
            else
            {// Tree Node Info

                if (data.getData().getType()==4) {
                    if ((data.getPosition().getLevel() == getMyDomainSpaceTreeLevel() - settings.branchingLevel)) {
                        for (IPPort node : Utilities.byteArrayToIPPortArray(data.getData().getData()))
                            if ((!node.equals(MessageParser.getMyExternalIp())) && (!node.equals(MessageParser.getMyInternalIp())))
                                addParent(data.getPosition(), node);
                    }

                    if ((data.getPosition().getLevel() == getMyDomainSpaceTreeLevel())) {
                        for (IPPort node : Utilities.byteArrayToIPPortArray(data.getData().getData()))
                            addNeighbour(data.getPosition(), node);
                    }
                    if ((data.getPosition().getLevel() == getMyDomainSpaceTreeLevel() + settings.branchingLevel)) {
                        for (IPPort node : Utilities.byteArrayToIPPortArray(data.getData().getData())) {
                            System.out.println("CHILD ADDED!");
                            if ((!node.equals(MessageParser.getMyExternalIp())) && (!node.equals(MessageParser.getMyInternalIp())))
                                addChild(data.getPosition(), node);
                        }
                    }
                }

                if (data.getData().getType()==5) {
                    if ((data.getPosition().getLevel() == getMyDomainSpaceTreeLevel() - settings.branchingLevel)) {
                        IPPort node = Utilities.byteArrayToIPPort(data.getData().getData());
                            if ((!node.equals(MessageParser.getMyExternalIp())) && (!node.equals(MessageParser.getMyInternalIp())))
                                addParent(data.getPosition(), node);
                    }

                    if ((data.getPosition().getLevel() == getMyDomainSpaceTreeLevel())) {
                        IPPort node = Utilities.byteArrayToIPPort(data.getData().getData());
                            addNeighbour(data.getPosition(), node);
                    }
                    if ((data.getPosition().getLevel() == getMyDomainSpaceTreeLevel() + settings.branchingLevel)) {
                        IPPort node = Utilities.byteArrayToIPPort(data.getData().getData());
                            System.out.println("CHILD ADDED!");
                            if ((!node.equals(MessageParser.getMyExternalIp())) && (!node.equals(MessageParser.getMyInternalIp())))
                                addChild(data.getPosition(), node);
                    }
                }

            }




    }

    private DomainSpaceTreeData merge(DomainSpaceTreeData what, DomainSpaceTreeData with,DomainTreeNodePosition pos) {

        if (what.getType()!=with.getType())
            return what;
        if (what.equals(with))
            return what;
        byte[] fileinfoHash = Utilities.toSHA256("fileinfo");
        if (what.getType()==0 && with.getType()==0 && Arrays.equals(pos.getPath(),fileinfoHash))
        {
            byte[] whatdec = Utilities.cryptWithKey(what.getData(),DomainsSecret.getPublicKey(domainHash),Cipher.DECRYPT_MODE);
            byte[] withdec = Utilities.cryptWithKey(with.getData(),DomainsSecret.getPublicKey(domainHash),Cipher.DECRYPT_MODE);
            if (withdec.length!=Constants.hashSize+2*Constants.longSize)
            {
                return what;
            }
            long whatstart = Utilities.bytearrayToLong(Arrays.copyOfRange(whatdec,Constants.hashSize,Constants.hashSize+Constants.longSize));
            long withstart = Utilities.bytearrayToLong(Arrays.copyOfRange(withdec,Constants.hashSize,Constants.hashSize+Constants.longSize));

            if (withstart<whatstart)
                return what;


        }

        if (what.getType()==2 && with.getData().length>0)
        {
            DomainSpaceTreeData neww = new DomainSpaceTreeData(Utilities.add(what.getData(),with.getData()),2);
            return neww;
        }

        return with;
    }

    public DomainSpaceTreeInfo getData(byte[] path)
    {
        if (!Utilities.arePathsSame(path, getMyDomainSpaceTreePath(), getMyDomainSpaceTreeLevel()))
        {
            return null;
        }

        CustomByteArray key = Utilities.byteArraytoCustomByteArray(path);

        DomainSpaceTreeData queue = getDomainsData().get(key);
        if (queue==null || queue.getData().length==0)
        {
            return null;
        }

        return new DomainSpaceTreeInfo(new DomainTreeNodePosition(Constants.hashSize*8, path),queue);
    }

    public ConcurrentLinkedQueue<IPPort> getMyParents() {
        return this.myParents;
    }

    public void removeParent(IPPort user) {
        getMyParents().remove(user);
    }



    @Override
    public void action() {


        if (sem.tryAcquire()) {
            if (actionSwitch < 5) {
                System.out.println("REFRESH");
                refreshDomainSpaceTreePosition();
            } else {
                System.out.println("CHANGE");
                changeDomainSpaceTreePosition();
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
            timer = settings.refreshTimeout;
            return true;
        }
        return false;
    }


    public int getDomainsDataSize() {
        int size=0;
        for ( DomainSpaceTreeData v :getDomainsData().values())
        {
            size+=v.getData().length+Constants.intSize;
        }
        return size;
    }

    public DomainSpaceTreeSettings getSettings() {
        return settings;
    }

    public int advertise(DomainSpaceTreeInfo info) {

        byte[] send = Utilities.add(Utilities.add(Utilities.intToByteArray(MessageParser.SUBMITDOMAINSPACETREENODEINFO),domainHash), Utilities.domainSpaceTreeInfoToByteArray(info));

        for (IPPort node : Utilities.eraseMyIp(Utilities.byteArrayToIPPortArray(findRoots().getData().getData())))
        {
            System.out.println("TO: " + node.toString());
            try {
                DatagramPacket dp = Utilities.createDatagramPacket(send, node);
                NetworkListener.getServerSocket().send(dp);
            }catch (Exception e){}
        }
        setInfo(info);
        return send.length;
    }

    public void advertise(List<DomainSpaceTreeInfo> infos)  {
        int send = 0;
        for (DomainSpaceTreeInfo dti : infos)
        {
            send += advertise(dti);
            if (send>1024)
            {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                send-=1024;
            }
        }
    }

    public static DomainSpaceTree getDomainSpaceTree(String host) {
        return getDomainSpaceTree(Utilities.byteArraytoCustomByteArray(Utilities.toSHA256(host)));
    }


    public static class DomainSpaceTreeSettings{
        private int branchingLevel;
        private int refreshTimeout;
        private int numberOfCachedPeers;
        private int maxDataSizePerNode;
        private int maxSingleDataSize;

        public DomainSpaceTreeSettings(int branchingLevel, int refreshTimeout, int numberOfCachedPeers, int maxDataSizePerNode, int maxSingleDataSize) {
            this.branchingLevel = branchingLevel;
            this.refreshTimeout = refreshTimeout;
            this.numberOfCachedPeers = numberOfCachedPeers;
            this.maxDataSizePerNode = maxDataSizePerNode;
            this.maxSingleDataSize = maxSingleDataSize;
        }

        public int getBranchingLevel() {
            return branchingLevel;
        }

        public int getRefreshTimeout() {
            return refreshTimeout;
        }

        public int getNumberOfCachedPeers() {
            return numberOfCachedPeers;
        }

        public int getMaxDataSizePerNode() {
            return maxDataSizePerNode;
        }

        public int getMaxSingleDataSize() {
            return maxSingleDataSize;
        }

        public void setBranchingLevel(int branchingLevel) {

            this.branchingLevel = branchingLevel;
        }

        public void setRefreshTimeout(int refreshTimeout) {
            this.refreshTimeout = refreshTimeout;
        }

        public void setNumberOfCachedPeers(int numberOfCachedPeers) {
            this.numberOfCachedPeers = numberOfCachedPeers;
        }

        public void setMaxDataSizePerNode(int maxDataSizePerNode) {
            this.maxDataSizePerNode = maxDataSizePerNode;
        }

        public void setMaxSingleDataSize(int maxSingleDataSize) {
            this.maxSingleDataSize = maxSingleDataSize;
        }

        public boolean equals(Object other)
        {
            if (other == null) return false;
            if (other == this) return true;
            if (!(other instanceof DomainSpaceTreeSettings))return false;
            DomainSpaceTreeSettings otherMyClass = (DomainSpaceTreeSettings)other;
            if (!(otherMyClass.branchingLevel!=this.branchingLevel)) return false;
            if (!(otherMyClass.maxDataSizePerNode!=this.maxDataSizePerNode)) return false;
            if (!(otherMyClass.maxSingleDataSize!=this.maxSingleDataSize)) return false;
            if (!(otherMyClass.numberOfCachedPeers!=this.numberOfCachedPeers)) return false;
            if (!(otherMyClass.refreshTimeout!=this.refreshTimeout)) return false;
            return true;
        }
    }

}
