package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecord;
import com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecordMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AiAnalysisRecordService {
    private final AiAnalysisRecordMapper mapper;
    public AiAnalysisRecordService(AiAnalysisRecordMapper mapper) { this.mapper = mapper; }
    public AiAnalysisRecordResponse get(long id, long requesterId, List<String> roles) {
        AiAnalysisRecord record = mapper.findById(id);
        if (record == null) throw new BusinessException(CommonErrorCode.NOT_FOUND);
        if (record.getInitiatorEmployeeId() != requesterId && (roles == null || roles.stream().noneMatch("SUPER_ADMIN"::equals))) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        return new AiAnalysisRecordResponse(record.getId(), record.getRequestType(), record.getBusinessReferenceId(),
                record.getStatus(), record.getDurationMs(), record.getResultSummary(), record.getAuditedAt());
    }
}
