package io.github.dekkerding.examples.composition;

public class Leaf implements AbstractNode{
    /**
     * @return
     */
    @Override
    public boolean isRoot() {
        return false;
    }

    /**
     * @return
     */
    @Override
    public int getld() {
        return 0;
    }

    /**
     * @return
     */
    @Override
    public int getParentld() {
        return 0;
    }

    /**
     * @param id
     */
    @Override
    public void setld(int id) {

    }

    /**
     * @param parentld
     */
    @Override
    public void setParentld(int parentld) {

    }

    /**
     * @param abstractNode
     */
    @Override
    public void add(AbstractNode abstractNode) {
        throw new UnsupportedOperationException("子叶子节点，无法增加");
    }

    /**
     * @param g
     */
    @Override
    public void remove(AbstractNode g) {
        throw new UnsupportedOperationException("子叶子节点，无法删除");
    }

    /**
     * @param i
     * @return
     */
    @Override
    public AbstractNode getChild(int i) {
        return null;
    }
}