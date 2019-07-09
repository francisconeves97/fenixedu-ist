package pt.ist.fenixedu.parking.dto;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import com.google.common.io.ByteStreams;

public class OpenFileBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private transient InputStream inputStream;
    private String fileName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public byte[] readStream() {
        try {
            return ByteStreams.toByteArray(inputStream);
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

}