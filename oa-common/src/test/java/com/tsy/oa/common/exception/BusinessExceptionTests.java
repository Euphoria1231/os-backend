package com.tsy.oa.common.exception;

import com.tsy.oa.common.error.CommonErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BusinessExceptionTests {

    @Test
    void businessExceptionRetainsErrorCodeAndMessage() {
        BusinessException exception = new BusinessException(CommonErrorCode.NOT_FOUND);

        assertSame(CommonErrorCode.NOT_FOUND, exception.errorCode());
        assertEquals("资源不存在", exception.getMessage());
    }
}
