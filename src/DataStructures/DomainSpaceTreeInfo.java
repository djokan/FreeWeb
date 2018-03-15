package DataStructures;

public class DomainSpaceTreeInfo
{
    private DomainTreeNodePosition position;
    private DomainSpaceTreeData data;

    public DomainSpaceTreeInfo(DomainTreeNodePosition pos, DomainSpaceTreeData data) {
        this.position = pos;
        this.data = data;
    }

    public void setPosition(DomainTreeNodePosition position) {
        this.position = position;
    }

    public void setData(DomainSpaceTreeData data) {
        this.data = data;
    }

    public DomainTreeNodePosition getPosition() {
        return position;
    }

    public DomainSpaceTreeData getData() {
        return data;
    }

    public int hashCode()
    {
        return position.hashCode() ^ data.hashCode();
    }
    public boolean equals(Object other)
    {
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof DomainTreeNodePosition))return false;
        DomainSpaceTreeInfo otherMyClass = (DomainSpaceTreeInfo)other;
        if (!otherMyClass.position.equals(this.position)) return false;
        if (!otherMyClass.data.equals(this.data)) return false;
        return true;
    }

}