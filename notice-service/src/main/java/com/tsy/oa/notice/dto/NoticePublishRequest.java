package com.tsy.oa.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoticePublishRequest(
        @NotBlank(message = "公告标题不能为空")
        @Size(max = 200, message = "公告标题不能超过200个字符")
        String title,

        @NotBlank(message = "公告内容不能为空")
        @Size(max = 10000, message = "公告内容不能超过10000个字符")
        String content
) {
}
