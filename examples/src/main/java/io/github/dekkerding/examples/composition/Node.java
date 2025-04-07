package io.github.dekkerding.examples.composition;

import java.util.ArrayList;
import java.util.List;

public class Node implements AbstractNode{

    private List<AbstractNode> children;
    private int id;
    private int pid;

    public Node() {
        this.children = new ArrayList<AbstractNode>();
    }

    /**
     * @return
     */
    @Override
    public boolean isRoot() {
        return -1 == pid;
    }

    /**
     * @return
     */
    @Override
    public int getld() {
        return id;
    }

    /**
     * @return
     */
    @Override
    public int getParentld() {
        return pid;
    }

    /**
     * @param id
     */
    @Override
    public void setld(int id) {
        this.id = id;
    }

    /**
     * @param parentld
     */
    @Override
    public void setParentld(int parentld) {
        this.pid = parentld;
    }

    /**
     * @param abstractNode
     */
    @Override
    public void add(AbstractNode abstractNode) {
        abstractNode.setParentld(this.pid+children.size());
        abstractNode.setld(abstractNode.getParentld()+1);
        children.add(abstractNode);
    }

    /**
     * @param abstractNode
     */
    @Override
    public void remove(AbstractNode abstractNode) {
        children.remove(abstractNode);
    }

    /**
     * @param i
     * @return
     */
    @Override
    public AbstractNode getChild(int i) {
        return children.get(i);
    }
}