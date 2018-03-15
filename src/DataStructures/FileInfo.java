package DataStructures;

import Utilities.Utilities;

import javax.sql.rowset.serial.SerialBlob;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Arrays;
import Utilities.*;
public class FileInfo {

    byte[] files;


    public FileInfo() {
        files = new byte[0];
    }


    public void add(byte[] fileHash, byte[] checksum) {
        if (files.length%64!=0)
        {
            files = Utilities.add(files, Constants.nullHash);
        }
        files = Utilities.add(files, Utilities.add(fileHash, checksum));
    }

    public void setFileInfo(byte[] f){
        files = f;
    }

    public byte[] getHashOf(String filename)
    {
        return  getHashOf(Utilities.toSHA256(filename));
    }


    public byte[] getHashOf(byte[] fileHash)
    {
        int index = 0;
        while (index<files.length)
        {
            if (Arrays.equals(Arrays.copyOfRange(files,index,index+ Constants.hashSize),fileHash))
                return Arrays.copyOfRange(files,index+ Constants.hashSize,index+2* Constants.hashSize);
            index+=2* Constants.hashSize;
        }
        return null;
    }

    public boolean doesFileExist(String file)
    {
        return doesFileExist(Utilities.toSHA256(file));
    }

    public boolean doesFileExist(byte[] file)
    {
        int index = 0;
        while (index<files.length)
        {
            if (Arrays.equals(Arrays.copyOfRange(files,index,index+ Constants.hashSize),file))
                return true;
            index+= Constants.hashSize;
        }
        return false;
    }

    public byte[] getFileInfo() {
        return files;
    }




    public void addFile(byte[] lasthash) {
        files = Utilities.add(files,lasthash);
    }
}
