package io.github.dekkerding.examples.composition;

public interface AbstractNode {
    public boolean isRoot();
    public int getld();
    public int getParentld();
    public void setld(int id);
    public void setParentld(int parentld);
    public void add(AbstractNode abstractNode);
    public void remove(AbstractNode g);
    public AbstractNode getChild(int i);

}