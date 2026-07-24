package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import feign.FeignException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class FeignAttendanceAnalysisSource implements AttendanceAnalysisSource {
    private final AttendanceSourceClient client;
    public FeignAttendanceAnalysisSource(AttendanceSourceClient client) { this.client = client; }

    @Override
    public List<AttendanceSourceRecord> findRecords(
            long requesterId,
            long targetEmployeeId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        try {
            ApiResponse<List<AttendanceSourceRecord>> response = client.records(
                    requesterId,
                    targetEmployeeId,
                    startDate,
                    endDate
            );
            if (response == null || response.code() != 0 || response.data() == null) {
                throw new IllegalStateException("Attendance source is unavailable");
            }
            return response.data();
        } catch (FeignException.Forbidden exception) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}
