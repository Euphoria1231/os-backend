package com.tsy.oa.common.log;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessOperationLoggerTests {

    @Test
    void successfulCommandPublishesSuccessLogWithResolvedTargetId() {
        List<OperationLogCommand> published = new ArrayList<>();
        BusinessOperationLogger logger = new BusinessOperationLogger(
                "flow-service", published::add, (command, exception) -> {
                }
        );

        String result = logger.execute(context(null), () -> "application-42", value -> value);

        assertEquals("application-42", result);
        assertEquals(1, published.size());
        OperationLogCommand command = published.getFirst();
        assertEquals("flow-service", command.serviceName());
        assertEquals("SUCCESS", command.operationStatus());
        assertEquals("application-42", command.targetId());
        assertEquals(null, command.errorMessage());
    }

    @Test
    void successfulCommandCanResolveOperatorFromBusinessResult() {
        List<OperationLogCommand> published = new ArrayList<>();
        BusinessOperationLogger logger = new BusinessOperationLogger(
                "user-service", published::add, (command, exception) -> {
                }
        );
        OperationLogContext anonymousContext = new OperationLogContext(
                null, "zhangsan", "AUTH", "LOGIN", "EMPLOYEE", null,
                "用户登录", "/api/user/auth/login", "POST", "127.0.0.1"
        );

        Long result = logger.executeWithContext(
                anonymousContext,
                () -> 7L,
                employeeId -> new OperationLogContext(
                        employeeId, null, "AUTH", "LOGIN", "EMPLOYEE",
                        employeeId.toString(), "用户登录", "/api/user/auth/login",
                        "POST", "127.0.0.1"
                )
        );

        assertEquals(7L, result);
        assertEquals(7L, published.getFirst().operatorId());
        assertEquals("7", published.getFirst().targetId());
    }

    @Test
    void failedCommandPublishesFailureLogAndRethrowsOriginalException() {
        List<OperationLogCommand> published = new ArrayList<>();
        BusinessOperationLogger logger = new BusinessOperationLogger(
                "attendance-service", published::add, (command, exception) -> {
                }
        );
        IllegalStateException businessFailure = new IllegalStateException("clock-in rejected");

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> logger.execute(context("employee-7"), () -> {
                    throw businessFailure;
                })
        );

        assertSame(businessFailure, thrown);
        assertEquals(1, published.size());
        OperationLogCommand command = published.getFirst();
        assertEquals("FAILURE", command.operationStatus());
        assertEquals("employee-7", command.targetId());
        assertEquals("IllegalStateException: clock-in rejected", command.errorMessage());
    }

    @Test
    void logSinkFailureDoesNotChangeSuccessfulBusinessResult() {
        AtomicReference<RuntimeException> reportedFailure = new AtomicReference<>();
        BusinessOperationLogger logger = new BusinessOperationLogger(
                "notice-service",
                command -> {
                    throw new IllegalStateException("user-service unavailable");
                },
                (command, exception) -> reportedFailure.set(exception)
        );

        Long result = logger.execute(context(null), () -> 9L, String::valueOf);

        assertEquals(9L, result);
        assertEquals("user-service unavailable", reportedFailure.get().getMessage());
    }

    @Test
    void logSinkFailureDoesNotReplaceOriginalBusinessFailure() {
        AtomicReference<RuntimeException> reportedFailure = new AtomicReference<>();
        BusinessOperationLogger logger = new BusinessOperationLogger(
                "intelligence-service",
                command -> {
                    throw new IllegalStateException("user-service unavailable");
                },
                (command, exception) -> reportedFailure.set(exception)
        );
        IllegalArgumentException businessFailure = new IllegalArgumentException("analysis denied");

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> logger.execute(context("analysis-3"), () -> {
                    throw businessFailure;
                })
        );

        assertSame(businessFailure, thrown);
        assertEquals("user-service unavailable", reportedFailure.get().getMessage());
    }

    private OperationLogContext context(String targetId) {
        return new OperationLogContext(
                7L,
                null,
                "FLOW",
                "SUBMIT_LEAVE",
                "APPLICATION",
                targetId,
                "提交请假申请",
                "/api/flow/applications/leave",
                "POST",
                "127.0.0.1"
        );
    }
}
