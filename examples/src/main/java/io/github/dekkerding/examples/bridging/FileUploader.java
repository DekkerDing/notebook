package io.github.dekkerding.examples.bridging;

public interface FileUploader {
    Object upload(String path);
    boolean check(Object object);
}