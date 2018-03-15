package DataStructures;

import Utilities.Utilities;

import java.util.Arrays;

public class DomainTreeNodePosition
{
    public int getLevel() {
        return level;
    }

    public byte[] getPath() {
        return Utilities.byteArraytobyteArray(path);
    }

    public DomainTreeNodePosition(int level, byte[] path) {
        this.level = level;
        this.path = Utilities.byteArraytoByteArray(path);
    }

    public int hashCode()
    {
        return level.hashCode() ^ Arrays.hashCode(path);
    }
    public boolean equals(Object other)
    {
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof DomainTreeNodePosition))return false;
        DomainTreeNodePosition otherMyClass = (DomainTreeNodePosition)other;
        if (!otherMyClass.level.equals(this.level)) return false;
        if (!Arrays.equals(otherMyClass.path,this.path)) return false;
        return true;
    }

    private Integer level;
    private Byte[] path;

}