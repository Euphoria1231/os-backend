package com.tsy.oa.intelligence.search.event.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface SearchIndexAggregateStateMapper {

    SearchIndexAggregateState findForUpdate(
            @Param("aggregateType") String aggregateType,
            @Param("aggregateId") long aggregateId
    );

    int initializeIfAbsent(
            @Param("aggregateType") String aggregateType,
            @Param("aggregateId") long aggregateId,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    int update(
            @Param("aggregateType") String aggregateType,
            @Param("aggregateId") long aggregateId,
            @Param("lastEventVersion") long lastEventVersion,
            @Param("lastEventId") String lastEventId,
            @Param("lastOperation") String lastOperation,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
