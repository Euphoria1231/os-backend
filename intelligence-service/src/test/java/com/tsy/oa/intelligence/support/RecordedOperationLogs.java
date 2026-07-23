package com.tsy.oa.intelligence.support;

import com.tsy.oa.common.log.OperationLogCommand;

import java.util.List;

public class RecordedOperationLogs {

    private final List<OperationLogCommand> commands = new java.util.concurrent.CopyOnWriteArrayList<>();

    public void add(OperationLogCommand command) {
        commands.add(command);
    }

    public long count(String operationType, String status) {
        return commands.stream()
                .filter(command -> operationType.equals(command.operationType()))
                .filter(command -> status.equals(command.operationStatus()))
                .count();
    }

    public void clear() {
        commands.clear();
    }
}
