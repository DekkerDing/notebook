package io.github.dekkerding.examples.decorate;

public class DataLoaderDecorator implements DataLoader{

    private DataLoader wrapper;

    public DataLoaderDecorator(DataLoader wrapper) {
        this.wrapper = wrapper;
    }

    /**
     * @return
     */
    @Override
    public String read() {
        return wrapper.read();
    }

    /**
     * @param data
     */
    @Override
    public void write(String data) {
        wrapper.write(data);
    }
}