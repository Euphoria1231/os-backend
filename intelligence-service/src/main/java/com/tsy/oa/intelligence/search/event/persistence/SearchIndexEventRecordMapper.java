package com.tsy.oa.intelligence.search.event.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface SearchIndexEventRecordMapper {

    int claim(
            @Param("eventId") String eventId,
            @Param("aggregateType") String aggregateType,
            @Param("aggregateId") long aggregateId,
            @Param("eventVersion") long eventVersion,
            @Param("operation") String operation
    );

    int markCompleted(
            @Param("eventId") String eventId,
            @Param("processingStatus") String processingStatus,
            @Param("processedAt") LocalDateTime processedAt
    );
}
