package org.example.network.protocol;

import java.io.Serializable;

public class Response implements Serializable {
    private ResponseType type;
    private Object data;
    private String error;

    public Response() {}

    public Response(ResponseType type, Object data) {
        this.type = type;
        this.data = data;
    }

    public Response(ResponseType type, String error) {
        this.type = type;
        this.error = error;
    }

    public ResponseType getType() {
        return type;
    }

    public void setType(ResponseType type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "Response{" +
                "type=" + type +
                ", data=" + data +
                ", error='" + error + '\'' +
                '}';
    }
}