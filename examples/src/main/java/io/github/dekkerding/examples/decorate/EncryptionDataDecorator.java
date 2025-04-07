package io.github.dekkerding.examples.decorate;

import java.util.Base64;

public class EncryptionDataDecorator extends DataLoaderDecorator{

    public EncryptionDataDecorator(DataLoader wrapper) {
        super(wrapper);
    }

    /**
     * @return
     */
    @Override
    public String read() {
        return decode(super.read());
    }

    /**
     * @param data
     */
    @Override
    public void write(String data) {
        super.write(encode(data));
    }

    private String encode(String data) {
        byte[] bytes = data.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] += (byte)1;
        }
        return data;
    }

    private String decode(String data) {
        byte[] decode = Base64.getDecoder().decode(data);
        for (int i = 0; i < decode.length; i++) {
            decode[i] -= (byte)1;
        }
        return new String(decode);
    }
}