package com.tsy.oa.intelligence.search.event.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SearchIndexEventSequenceMapper {

    int insert(
            @Param("eventId") String eventId,
            @Param("createdAt") LocalDateTime createdAt
    );

    long findMaximumSequence();

    List<SearchIndexReplayDelta> findProcessedAfter(
            @Param("aggregateType") String aggregateType,
            @Param("afterSequence") long afterSequence,
            @Param("limit") int limit
    );
}
