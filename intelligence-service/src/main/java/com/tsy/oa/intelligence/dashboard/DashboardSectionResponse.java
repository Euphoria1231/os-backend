package com.tsy.oa.intelligence.dashboard;

public record DashboardSectionResponse<T>(DashboardSectionStatus status, T data, String message) {

    public static <T> DashboardSectionResponse<T> success(T data) {
        return new DashboardSectionResponse<>(DashboardSectionStatus.SUCCESS, data, null);
    }

    public static <T> DashboardSectionResponse<T> failed(String message) {
        return new DashboardSectionResponse<>(DashboardSectionStatus.FAILED, null, message);
    }
}
