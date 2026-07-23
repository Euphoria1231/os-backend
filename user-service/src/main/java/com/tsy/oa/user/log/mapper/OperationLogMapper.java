package com.tsy.oa.user.log.mapper;

import com.tsy.oa.user.log.model.BusinessOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OperationLogMapper {

    int insert(BusinessOperationLog operationLog);

    BusinessOperationLog findById(Long id);

    long count(
            @Param("operatorId") Long operatorId,
            @Param("operatorKeyword") String operatorKeyword,
            @Param("businessModule") String businessModule,
            @Param("operationStatus") String operationStatus,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    List<BusinessOperationLog> findPage(
            @Param("operatorId") Long operatorId,
            @Param("operatorKeyword") String operatorKeyword,
            @Param("businessModule") String businessModule,
            @Param("operationStatus") String operationStatus,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );
}
