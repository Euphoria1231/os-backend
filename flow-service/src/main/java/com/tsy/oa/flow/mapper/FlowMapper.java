package com.tsy.oa.flow.mapper;

import com.tsy.oa.flow.dashboard.dto.ApprovalDailyTrendResponse;
import com.tsy.oa.flow.dashboard.dto.ApprovalStatusCountsResponse;
import com.tsy.oa.flow.dashboard.dto.ApprovalTypeDistributionResponse;
import com.tsy.oa.flow.model.ApprovalTaskRecord;
import com.tsy.oa.flow.model.FlowApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FlowMapper {

    int insertApplication(FlowApplication application);

    FlowApplication findApplicationById(Long id);

    List<FlowApplication> findApplicationsByApplicant(Long applicantId);

    List<FlowApplication> findPendingApplicationsByApprover(Long approverId);

    int updateApplicationStatusIfPending(@Param("id") Long id, @Param("status") String status);

    int insertApprovalRecord(
            @Param("applicationId") Long applicationId,
            @Param("approverId") Long approverId,
            @Param("action") String action,
            @Param("comment") String comment
    );

    List<ApprovalTaskRecord> findCompletedTasksByApprover(Long approverId);

    long countApplications();

    List<FlowApplication> findApplicationPage(
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    ApprovalStatusCountsResponse countDashboardStatuses(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    List<ApprovalTypeDistributionResponse> findDashboardTypeDistribution(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    List<ApprovalDailyTrendResponse> findDashboardDailyTrend(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
