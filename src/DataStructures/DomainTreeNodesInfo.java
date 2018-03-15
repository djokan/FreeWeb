package DataStructures;

public class DomainTreeNodesInfo
{
    public DomainTreeNodePosition getPosition() {
        return position;
    }

    public void setNode(IPPort[] node) {
        this.node = node;
    }

    public void setPosition(DomainTreeNodePosition position) {

        this.position = position;
    }

    public IPPort[] getNodes() {
        return node;
    }

    public DomainTreeNodesInfo(DomainTreeNodePosition position, IPPort[] node) {
        this.position = position;
        this.node = node;
    }

    private DomainTreeNodePosition position;
    private IPPort[] node;
}