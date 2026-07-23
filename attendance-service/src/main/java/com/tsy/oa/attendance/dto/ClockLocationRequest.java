package com.tsy.oa.attendance.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ClockLocationRequest(
        @NotNull(message = "经度不能为空")
        @DecimalMin(value = "-180.0", message = "经度必须大于或等于 -180")
        @DecimalMax(value = "180.0", message = "经度必须小于或等于 180")
        Double longitude,
        @NotNull(message = "纬度不能为空")
        @DecimalMin(value = "-90.0", message = "纬度必须大于或等于 -90")
        @DecimalMax(value = "90.0", message = "纬度必须小于或等于 90")
        Double latitude
) {
}
