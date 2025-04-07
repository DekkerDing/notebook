package io.github.dekkerding.examples.adapter;

public class TargetAbstractionlmpl extends TargetAbstraction {
    /**
     * @param str
     * @return
     */
    @Override
    public String filter(String str) {
        return str + "TargetAbstractionlmpl";
    }
}