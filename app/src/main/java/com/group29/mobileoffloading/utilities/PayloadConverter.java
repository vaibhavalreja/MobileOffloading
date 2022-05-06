package com.group29.mobileoffloading.utilities;

import com.google.android.gms.nearby.connection.Payload;
import com.group29.mobileoffloading.DataModels.NodeDataPayload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class PayloadConverter {
    public static Payload toPayload(NodeDataPayload tPayload) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(tPayload);
        objectOutputStream.flush();

        byte[] bytes = byteArrayOutputStream.toByteArray();

        Payload payload = Payload.fromBytes(bytes);
        return payload;
    }

    public static NodeDataPayload fromPayload(Payload payload) throws IOException, ClassNotFoundException {
        byte[] receivedBytes = payload.asBytes();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(receivedBytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

        return (NodeDataPayload) objectInputStream.readObject();
    }
}
