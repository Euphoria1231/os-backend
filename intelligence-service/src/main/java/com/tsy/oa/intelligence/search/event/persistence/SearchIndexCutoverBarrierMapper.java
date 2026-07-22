package com.tsy.oa.intelligence.search.event.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface SearchIndexCutoverBarrierMapper {

    int initializeIfAbsent(
            @Param("aggregateType") String aggregateType,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    String findForUpdate(@Param("aggregateType") String aggregateType);
}
