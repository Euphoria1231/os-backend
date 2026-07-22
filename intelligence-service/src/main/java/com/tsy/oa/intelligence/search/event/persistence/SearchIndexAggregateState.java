package com.tsy.oa.intelligence.search.event.persistence;

public class SearchIndexAggregateState {

    private String aggregateType;
    private long aggregateId;
    private long lastEventVersion;
    private String lastEventId;
    private String lastOperation;

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public long getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(long aggregateId) {
        this.aggregateId = aggregateId;
    }

    public long getLastEventVersion() {
        return lastEventVersion;
    }

    public void setLastEventVersion(long lastEventVersion) {
        this.lastEventVersion = lastEventVersion;
    }

    public String getLastEventId() {
        return lastEventId;
    }

    public void setLastEventId(String lastEventId) {
        this.lastEventId = lastEventId;
    }

    public String getLastOperation() {
        return lastOperation;
    }

    public void setLastOperation(String lastOperation) {
        this.lastOperation = lastOperation;
    }
}
