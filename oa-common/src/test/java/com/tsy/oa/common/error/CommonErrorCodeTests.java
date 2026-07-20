package com.tsy.oa.common.error;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommonErrorCodeTests {

    @Test
    void badRequestErrorCodeExposesStableCodeAndMessage() {
        ErrorCode errorCode = CommonErrorCode.BAD_REQUEST;

        assertEquals(40000, errorCode.code());
        assertEquals("请求参数错误", errorCode.message());
    }

    @Test
    void standardCommonErrorCodesRemainStable() {
        assertEquals(40100, CommonErrorCode.UNAUTHORIZED.code());
        assertEquals("未登录或登录已过期", CommonErrorCode.UNAUTHORIZED.message());
        assertEquals(40300, CommonErrorCode.FORBIDDEN.code());
        assertEquals("无权访问", CommonErrorCode.FORBIDDEN.message());
        assertEquals(40400, CommonErrorCode.NOT_FOUND.code());
        assertEquals("资源不存在", CommonErrorCode.NOT_FOUND.message());
        assertEquals(50000, CommonErrorCode.INTERNAL_SERVER_ERROR.code());
        assertEquals("系统内部错误", CommonErrorCode.INTERNAL_SERVER_ERROR.message());
    }

    @Test
    void commonErrorCodesUseUniqueCodes() {
        CommonErrorCode[] errorCodes = CommonErrorCode.values();
        Set<Integer> codes = Arrays.stream(errorCodes)
                .map(CommonErrorCode::code)
                .collect(Collectors.toSet());

        assertEquals(errorCodes.length, codes.size());
    }
}
