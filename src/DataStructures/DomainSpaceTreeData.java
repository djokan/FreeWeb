package DataStructures;

import java.util.Arrays;

public class DomainSpaceTreeData
{
    private byte[] data;
    private int type; //1-domainData 2-userPublicData 3- userPrivateData, 4-IPPortArray, 5- IPPort

    public byte[] getData() {
        return data;
    }

    public DomainSpaceTreeData(byte[] data, int type) {
        this.data = data;
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int hashCode()
    {
        return new Integer(type).hashCode() ^ Arrays.hashCode(data);
    }
    public boolean equals(Object other)
    {
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof DomainSpaceTreeData))return false;
        DomainSpaceTreeData otherMyClass = (DomainSpaceTreeData)other;
        if (!(otherMyClass.type==this.type)) return false;
        if (!Arrays.equals(otherMyClass.data,this.data)) return false;
        return true;
    }


}