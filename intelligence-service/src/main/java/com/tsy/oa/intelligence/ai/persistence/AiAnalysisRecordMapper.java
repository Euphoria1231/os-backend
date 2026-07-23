package com.tsy.oa.intelligence.ai.persistence;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiAnalysisRecordMapper {

    int insert(AiAnalysisRecord record);

    AiAnalysisRecord findById(long id);
}
