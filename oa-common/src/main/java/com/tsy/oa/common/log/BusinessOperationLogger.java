package com.tsy.oa.common.log;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class BusinessOperationLogger {

    private static final String SUCCESS = "SUCCESS";
    private static final String FAILURE = "FAILURE";

    private final String serviceName;
    private final Consumer<OperationLogCommand> logSink;
    private final BiConsumer<OperationLogCommand, RuntimeException> publishFailureHandler;

    public BusinessOperationLogger(
            String serviceName,
            Consumer<OperationLogCommand> logSink,
            BiConsumer<OperationLogCommand, RuntimeException> publishFailureHandler
    ) {
        this.serviceName = serviceName;
        this.logSink = logSink;
        this.publishFailureHandler = publishFailureHandler;
    }

    public <T> T execute(OperationLogContext context, Supplier<T> action) {
        return execute(context, action, ignored -> context.targetId());
    }

    public <T> T execute(
            OperationLogContext context,
            Supplier<T> action,
            Function<T, String> targetIdResolver
    ) {
        return executeWithContext(
                context,
                action,
                result -> context.withTargetId(targetIdResolver.apply(result))
        );
    }

    public <T> T executeWithContext(
            OperationLogContext context,
            Supplier<T> action,
            Function<T, OperationLogContext> successContextResolver
    ) {
        try {
            T result = action.get();
            OperationLogContext successContext = successContextResolver.apply(result);
            publish(command(successContext, successContext.targetId(), SUCCESS, null));
            return result;
        } catch (RuntimeException exception) {
            publish(command(context, context.targetId(), FAILURE, summarize(exception)));
            throw exception;
        }
    }

    public void execute(OperationLogContext context, Runnable action) {
        execute(context, () -> {
            action.run();
            return null;
        });
    }

    private OperationLogCommand command(
            OperationLogContext context,
            String targetId,
            String status,
            String errorMessage
    ) {
        return new OperationLogCommand(
                context.operatorId(),
                context.operatorName(),
                serviceName,
                context.businessModule(),
                context.operationType(),
                context.targetType(),
                targetId,
                context.summary(),
                status,
                context.requestPath(),
                context.httpMethod(),
                context.clientIp(),
                errorMessage
        );
    }

    private void publish(OperationLogCommand command) {
        try {
            logSink.accept(command);
        } catch (RuntimeException exception) {
            publishFailureHandler.accept(command, exception);
        }
    }

    private String summarize(RuntimeException exception) {
        String type = exception.getClass().getSimpleName();
        String message = exception.getMessage();
        return message == null || message.isBlank() ? type : type + ": " + message;
    }
}
