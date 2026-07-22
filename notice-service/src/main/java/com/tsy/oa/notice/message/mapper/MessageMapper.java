package com.tsy.oa.notice.message.mapper;

import com.tsy.oa.notice.message.model.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper {

    int insertConsumeRecordIfAbsent(
            @Param("eventId") String eventId,
            @Param("topic") String topic
    );

    int insertMessage(Message message);

    List<Message> findMessagesForRecipient(
            @Param("recipientId") Long recipientId,
            @Param("read") Boolean read,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    Message findMessageForRecipient(
            @Param("id") Long id,
            @Param("recipientId") Long recipientId
    );

    int markRead(
            @Param("id") Long id,
            @Param("recipientId") Long recipientId
    );

    int markAllRead(Long recipientId);

    int countUnread(Long recipientId);
}
