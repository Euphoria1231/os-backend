package com.tsy.oa.apitest.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 统一响应模型 - 对应后端 ApiResponse<T>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResponse {

    private int code;
    private String message;
    private Object data;

    public ApiResponse() {}

    @JsonProperty("code")
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    @JsonProperty("message")
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    @JsonProperty("data")
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public boolean isSuccess() {
        return code == 200;
    }

    @Override
    public String toString() {
        return "ApiResponse{code=" + code + ", message='" + message + "'}";
    }
}
