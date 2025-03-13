package io.github.dekkerding.examples;

/**
 *  JDK 1.8
 *  饿汉式
  */
public class Singleton {

    private Singleton(){}

    private final static Singleton INSTANCE = new Singleton();

    public static Singleton getInstance(){
        return INSTANCE;
    }
}