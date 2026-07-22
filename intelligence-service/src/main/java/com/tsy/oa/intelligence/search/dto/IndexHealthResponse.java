package com.tsy.oa.intelligence.search.dto;

public record IndexHealthResponse(
        boolean elasticsearchReachable,
        IndexState noticeIndex,
        IndexState applicationIndex,
        RebuildProgressResponse noticeRebuild,
        RebuildProgressResponse applicationRebuild
) {
    public record IndexState(String name, boolean exists) {
    }
}
