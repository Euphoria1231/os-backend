package com.tsy.oa.flow.attendance;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.flow.attendance.AttendanceServiceClient.MakeupCompletionRequest;
import com.tsy.oa.flow.attendance.AttendanceServiceClient.MakeupCompletionResponse;
import com.tsy.oa.flow.attendance.AttendanceServiceClient.MakeupEligibilityResponse;
import com.tsy.oa.flow.error.FlowErrorCode;
import feign.FeignException;
import org.springframework.stereotype.Component;

@Component
public class FeignAttendanceMakeupGateway implements AttendanceMakeupGateway {

    private final AttendanceServiceClient attendanceServiceClient;

    public FeignAttendanceMakeupGateway(AttendanceServiceClient attendanceServiceClient) {
        this.attendanceServiceClient = attendanceServiceClient;
    }

    @Override
    public MakeupEligibility getEligibility(Long attendanceRecordId, Long employeeId) {
        try {
            ApiResponse<MakeupEligibilityResponse> response = attendanceServiceClient
                    .getMakeupEligibility(attendanceRecordId, employeeId);
            MakeupEligibilityResponse data = response == null ? null : response.data();
            if (data == null || !data.eligible()) {
                throw new BusinessException(FlowErrorCode.MAKEUP_NOT_ELIGIBLE);
            }
            return new MakeupEligibility(
                    data.attendanceRecordId(), data.employeeId(), data.attendanceDate(),
                    data.remainingCount()
            );
        } catch (FeignException exception) {
            throw attendanceException(exception, FlowErrorCode.MAKEUP_NOT_ELIGIBLE);
        }
    }

    @Override
    public void completeMakeup(Long attendanceRecordId, Long employeeId, Long applicationId) {
        try {
            ApiResponse<MakeupCompletionResponse> response = attendanceServiceClient.completeMakeup(
                    attendanceRecordId,
                    new MakeupCompletionRequest(employeeId, applicationId)
            );
            if (response == null || response.data() == null) {
                throw new BusinessException(FlowErrorCode.MAKEUP_COMPLETION_FAILED);
            }
        } catch (FeignException exception) {
            throw attendanceException(exception, FlowErrorCode.MAKEUP_COMPLETION_FAILED);
        }
    }

    private BusinessException attendanceException(
            FeignException exception,
            FlowErrorCode clientError
    ) {
        if (exception.status() >= 400 && exception.status() < 500) {
            return new BusinessException(clientError);
        }
        return new BusinessException(FlowErrorCode.ATTENDANCE_SERVICE_UNAVAILABLE);
    }
}
