package io.github.dekkerding.examples.bridging;

/**
 *  实现一个新的上传到云存储的文件上传执行器
 *  先新建一个叫OSSFileUploaderlmpl的具体实现类然后建立对应的云存储文件执行器
 *  接着再分别实现华为云、阿里云、腾讯云等各种不同云存储的文件上传执行器
 */
public interface FileUploadExcutor {
    Object upload(String path);
    boolean check(Object object);
}