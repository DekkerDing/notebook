package io.github.dekkerding.examples.decorate;

import java.io.*;

public class FileDataLoader implements DataLoader{

    private String filePath;

    public FileDataLoader(String filePath) {
        this.filePath = filePath;
    }

    /**
     * @return
     */
    @Override
    public String read() {
        char[] buffer =null;
        File file =new File(filePath);
        try (FileReader fileReader = new FileReader(file)){
            buffer=new char[(int) file.length()];
            fileReader.read(buffer);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new String(buffer);
    }

    /**
     * @param data
     */
    @Override
    public void write(String data) {
        File file =new File(filePath);
        try (OutputStream outputStream =  new FileOutputStream(file)){
            outputStream.write(data.getBytes(), 0, data.length());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}