package io.github.dekkerding.examples.adapter;

public class Adapter extends TargetAbstraction {

    private OtherClass otherClass;

    public Adapter() {
        this.otherClass = new OtherClass();
    }

    /**
     * @param str
     * @return
     */
    @Override
    public String filter(String str) {
        return otherClass.extend(str);
    }
}