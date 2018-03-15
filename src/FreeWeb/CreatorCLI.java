package FreeWeb;

import DataStructures.DomainSpaceTreeData;
import DataStructures.DomainSpaceTreeInfo;
import DataStructures.DomainTreeNodePosition;
import DataStructures.FileInfo;
import Database.Database;
import Network.DomainSpaceTree;
import Network.NetworkSynchronizer;
import Utilities.*;
import Utilities.Constants;
import com.sun.net.httpserver.HttpServer;

import javax.crypto.Cipher;
import java.io.Console;
import java.io.FileOutputStream;
import java.security.*;
import java.util.*;

public class CreatorCLI {
    public static void main(String[] args) throws Exception {


        if (args.length>0) {
            Console console = System.console();
            if (console == null) {
                System.out.println("Couldn't get Console instance");
                System.exit(0);
            }
            if (args[0].equals("--generatekeys"))
            {
                char passwordArray[] = console.readPassword("Enter your secret password: ");
                char passwordArrayr[] = console.readPassword("Enter your secret password again: ");

                if (Arrays.equals(passwordArray,passwordArrayr))
                {
                    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                    keyGen.initialize(Constants.PUBLICKEYSIZE, new SecureRandom());
                    KeyPair pair= keyGen.genKeyPair();

                    PrivateKey pk = Utilities.EncryptPrivateKey(pair.getPrivate(),passwordArray);

                    byte[] pub = Utilities.PublicKeytoByteArray(pair.getPublic());
                    byte[] pri = Utilities.PrivateKeytoByteArray(pk);
                    byte[] sum = Utilities.add(Utilities.intToByteArray(pub.length),Utilities.add(pub,pri));
                    FileOutputStream fos = new FileOutputStream("keys.cfg");
                    fos.write(sum);
                    fos.close();
                }
                else {
                    console.printf("Entered passwords doesn't match");
                }

            }
            if (args[0].equals("--generateconfig"))
            {

                String domainName = console.readLine("Enter domain name: ");

                String branchingLevelStr = console.readLine("Enter branching level: ");

                int branchingLevel=0;

                try {
                    branchingLevel = Integer.parseInt(branchingLevelStr);
                }catch (Exception e)
                {
                    System.out.println("Invalid input!");
                    return;
                }

                String refreshTimeoutStr = console.readLine("Enter refreshTimeout: ");

                int refreshTimeout=0;

                try {
                    refreshTimeout = Integer.parseInt(refreshTimeoutStr);
                }catch (Exception e)
                {
                    System.out.println("Invalid input!");
                    return;
                }

                String numberOfCachedPeersStr = console.readLine("Enter numberOfCachedPeers: ");

                int numberOfCachedPeers=0;
                try {
                    numberOfCachedPeers = Integer.parseInt(numberOfCachedPeersStr);
                }catch (Exception e)
                {
                    System.out.println("Invalid input!");
                    return;
                }

                String maxDataSizePerNodeStr = console.readLine("Enter maxDataSizePerNode: ");

                int maxDataSizePerNode=0;
                try {
                    maxDataSizePerNode = Integer.parseInt(maxDataSizePerNodeStr);
                }catch (Exception e)
                {
                    System.out.println("Invalid input!");
                    return;
                }

                String maxSingleDataSizeStr = console.readLine("Enter maxSingleDataSize: ");

                int maxSingleDataSize=0;

                try {
                    maxSingleDataSize = Integer.parseInt(maxSingleDataSizeStr);
                }catch (Exception e)
                {
                    System.out.println("Invalid input!");
                    return;
                }


                DomainSpaceTree.DomainSpaceTreeSettings dsett = new DomainSpaceTree.DomainSpaceTreeSettings(branchingLevel,refreshTimeout,numberOfCachedPeers,maxDataSizePerNode,maxSingleDataSize);

                byte[] domainHash = Utilities.toSHA256(domainName);

                FileOutputStream fos = new FileOutputStream("config.cfg");

                fos.write(domainHash);
                fos.write(Utilities.DomainSpaceTreeSettingsToByteArray(dsett));
                fos.close();

            }
            return;
        }

        KeyPair kp= null;

        Console console = System.console();
        if (console == null) {
            System.out.println("Couldn't get Console instance");
            System.exit(0);
        }

        char passwordArray[] = console.readPassword("Enter your secret password: ");

//        char passwordArray[] = "micamica".toCharArray();

        PublicKey publicKey=null;
        PrivateKey privateKey=null;

        byte[] b= Utilities.readAllBytes("keys.cfg");

        int pubsize = Utilities.bytearrayToInt(Arrays.copyOfRange(b,0,Constants.intSize));

        publicKey = Utilities.ByteArraytoPublicKey(Arrays.copyOfRange(b,Constants.intSize,Constants.intSize+pubsize));
        privateKey = Utilities.DecryptPrivateKey(Utilities.ByteArraytoPrivateKey(Arrays.copyOfRange(b,Constants.intSize+pubsize,b.length)),passwordArray);

        kp = new KeyPair(publicKey,privateKey);
        if (!Utilities.isValid(kp))
        {
            System.out.println("Entered password is incorrect");
            return;
        }



        NetworkSynchronizer s;
        HttpServer server;
        //Start Database
        Database.startServerOnRandomPort();
        //Start Network Synchronizer
        Constants.networklistenPort = 3348;
        s = new NetworkSynchronizer();
        s.start();
        //Share Website Files

        byte[] siteconfig = Utilities.readAllBytes("config.cfg");

        byte[] domainHash = Arrays.copyOfRange(siteconfig,0,Constants.hashSize);

        DomainSpaceTree.DomainSpaceTreeSettings settings = Utilities.ByteArrayToDomainSpaceTreeSettings(Arrays.copyOfRange(siteconfig,Constants.hashSize,siteconfig.length));

        DomainSpaceTree dst = DomainSpaceTree.createNewTree(domainHash,settings);

        String durationStr = console.readLine("Enter duration of validity of files [sec]: ");


        long duration=0;

        try {
            duration = Long.parseLong(durationStr);
        }catch (Exception e)
        {
            System.out.println("Invalid input!");
            return;
        }

        long startTime = Utilities.getTime(),endTime;
        endTime = startTime+duration;




        FileInfo fileinfo = new FileInfo();
        List<DomainSpaceTreeInfo> queue =  new LinkedList<DomainSpaceTreeInfo>();

        for (String fileName : Utilities.listAllFiles())
        {
//            if (fileName.contains("\\") || fileName.charAt(0)=='.')
//                continue;
            byte[] file = Utilities.readAllBytes(fileName);
            addFileToQueue(Utilities.toSHA256(fileName), file, queue,fileinfo,privateKey,settings.getMaxSingleDataSize());
            System.out.println("FAJL");
        }
        byte[] lasthash = addFileToQueue(null,fileinfo.getFileInfo(),queue,null, privateKey,settings.getMaxSingleDataSize());

        if (!fileinfo.doesFileExist("index.html"))
        {
            System.out.println("NE POSTOJI");
        }

        DomainSpaceTreeData dstd = new DomainSpaceTreeData(Utilities.cryptWithKey(Utilities.add(lasthash,Utilities.add(Utilities.longToByteArray(startTime),Utilities.longToByteArray(endTime))),privateKey,Cipher.ENCRYPT_MODE),0);

        DomainTreeNodePosition dtnp = new DomainTreeNodePosition(Constants.hashSize*8,Utilities.toSHA256("fileinfo"));
        DomainSpaceTreeInfo dsti = new DomainSpaceTreeInfo(dtnp,dstd);
        queue.add(0,dsti);
        System.out.println("ADV, fileinfo size : " + fileinfo.getFileInfo().length);
        Thread.sleep(20000);
        while (true)
        {
            System.out.println(queue.size());
            int iii=0;
            for (DomainSpaceTreeInfo info: queue.toArray(new DomainSpaceTreeInfo[0]))
            {
                System.out.println("ADVERTISING ");
                Utilities.printByteArray(info.getPosition().getPath());
//                System.out.println("data: ");
//                Utilities.printByteArray(info.getData().getData());
                dst.advertise(info);
                if (iii%10==0)
                Thread.sleep(300);
            }

            Thread.sleep(5000);

        }


    }

    private static byte[] addFileToQueue(byte[] fileHash, byte[] file, List<DomainSpaceTreeInfo> queue, FileInfo fileinfo, PrivateKey privateKey, int maxSingleDataSize) {

        byte[] formattedfile = Utilities.add(Utilities.add(Utilities.intToByteArray(file.length),file),Constants.nullHash);
        List<byte[]> parts = new LinkedList<>();
        int index = 0;

        while (formattedfile.length-index>maxSingleDataSize)
        {
            parts.add(0,Arrays.copyOfRange(formattedfile,index,index+maxSingleDataSize));
            index+= maxSingleDataSize-Constants.hashSize;
        }
        parts.add(0,Arrays.copyOfRange(formattedfile,index,formattedfile.length));

        byte[] lasthash;
        byte[] part= parts.remove(0);

        addToQueue(part,queue);
        lasthash = Utilities.toSHA256(part);
        while (true)
            try {
                part = parts.remove(0);
                Utilities.addChecksumAtEnd(part,lasthash);
                addToQueue(part,queue);
                if (fileinfo!=null)
                    fileinfo.addFile(lasthash);
                lasthash = Utilities.toSHA256(part);
            }catch (Exception e){
                break;
            }
        if (fileinfo!=null)
            fileinfo.add(fileHash,lasthash);
        return lasthash;
    }

    public static void addToQueue(byte[] part, List<DomainSpaceTreeInfo> list)
    {
        DomainSpaceTreeData dstd = new DomainSpaceTreeData(part,0);
        DomainTreeNodePosition dtnp = new DomainTreeNodePosition(Constants.hashSize*8,Utilities.toSHA256(part));
        DomainSpaceTreeInfo dsti = new DomainSpaceTreeInfo(dtnp,dstd);
        list.add(dsti);
    }

}
