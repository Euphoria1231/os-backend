package com.tsy.oa.user.position.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.user.position.dto.PositionRequest;
import com.tsy.oa.user.position.dto.PositionResponse;
import com.tsy.oa.user.position.service.PositionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/positions")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @PostMapping
    public ApiResponse<PositionResponse> create(@Valid @RequestBody PositionRequest request) {
        return ApiResponse.success(positionService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<PositionResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(positionService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<PositionResponse>> list() {
        return ApiResponse.success(positionService.list());
    }

    @PutMapping("/{id}")
    public ApiResponse<PositionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PositionRequest request
    ) {
        return ApiResponse.success(positionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        positionService.delete(id);
        return ApiResponse.success(null);
    }
}
