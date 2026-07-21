package com.tsy.oa.notice.mapper;

import com.tsy.oa.notice.model.Notice;
import com.tsy.oa.notice.model.NoticeView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoticeMapper {

    int insert(Notice notice);

    Notice findPublishedById(Long id);

    List<NoticeView> findPublishedForEmployee(Long employeeId);

    NoticeView findPublishedByIdForEmployee(
            @Param("noticeId") Long noticeId,
            @Param("employeeId") Long employeeId
    );

    int insertReadIfAbsent(
            @Param("noticeId") Long noticeId,
            @Param("employeeId") Long employeeId
    );

    int countUnread(Long employeeId);
}
