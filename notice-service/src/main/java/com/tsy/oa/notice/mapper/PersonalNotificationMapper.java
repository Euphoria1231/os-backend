package com.tsy.oa.notice.mapper;

import com.tsy.oa.notice.model.PersonalNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PersonalNotificationMapper {

    int insert(PersonalNotification notification);

    long countByRecipient(Long recipientEmployeeId);

    List<PersonalNotification> findPageByRecipient(
            @Param("recipientEmployeeId") Long recipientEmployeeId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    int countUnread(Long recipientEmployeeId);

    boolean existsByIdAndRecipient(
            @Param("id") Long id,
            @Param("recipientEmployeeId") Long recipientEmployeeId
    );

    int markRead(
            @Param("id") Long id,
            @Param("recipientEmployeeId") Long recipientEmployeeId
    );

    int markAllRead(Long recipientEmployeeId);
}
