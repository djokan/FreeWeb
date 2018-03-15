package DataStructures;

public class DomainTreeNodeInfo
{
    public DomainTreeNodePosition getPosition() {
        return position;
    }

    public IPPort getNode() {
        return node;
    }

    public DomainTreeNodeInfo(DomainTreeNodePosition position, IPPort node) {
        this.position = position;
        this.node = node;
    }

    private DomainTreeNodePosition position;
    private IPPort node;
}