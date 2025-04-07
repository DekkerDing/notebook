package io.github.dekkerding.examples.bridging;

public class FileUploaderlmpl implements FileUploader{

    private FileUploadExcutor excutor = null;

    public FileUploaderlmpl(FileUploadExcutor excutor) {
        this.excutor = excutor;
    }

    /**
     * @param path
     * @return
     */
    @Override
    public Object upload(String path) {
        return excutor.upload(path);
    }

    /**
     * @param object
     * @return
     */
    @Override
    public boolean check(Object object) {
        return excutor.check(object);
    }
}