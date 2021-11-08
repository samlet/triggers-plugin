package com.bluecc.generic;

public class EventResponse<T> {
    String status;
    T data;

    EventResponse(){}

    public EventResponse(String status, T data) {
        this.status = status;
        this.data = data;
    }

    @Override
    public String toString() {
        return "EventResponse{" +
                "status='" + status + '\'' +
                ", data=" + data +
                '}';
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

