package com.home.ma.photolocationnote.utility;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

public class NVP implements NameValuePair, Serializable {
    private BasicNameValuePair nvp;

    public NVP(String name, Integer value) {
        nvp = new BasicNameValuePair(name, value.toString());
    }

    @Override
    public String getName() {
        return nvp.getName();
    }

    @Override
    public String getValue() {
        return nvp.getValue();
    }

    // serialization support

    private static final long serialVersionUID = 1L;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(nvp.getName());
        out.writeObject(nvp.getValue());
      //  out.writeString(nvp.getName());
      //  out.writeString(nvp.getValue());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        nvp = new BasicNameValuePair(in.readObject().toString(), in.readObject().toString());
    }

    private void readObjectNoData() throws ObjectStreamException {
        // nothing to do
    }
}
