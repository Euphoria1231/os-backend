package com.tsy.oa.user.rbac.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.user.rbac.dto.MenuRequest;
import com.tsy.oa.user.rbac.dto.MenuResponse;
import com.tsy.oa.user.rbac.service.RbacService;
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
@RequestMapping("/api/user/menus")
public class MenuController {

    private final RbacService rbacService;

    public MenuController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @PostMapping
    public ApiResponse<MenuResponse> create(@Valid @RequestBody MenuRequest request) {
        return ApiResponse.success(rbacService.createMenu(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<MenuResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(rbacService.getMenu(id));
    }

    @GetMapping
    public ApiResponse<List<MenuResponse>> list() {
        return ApiResponse.success(rbacService.listMenus());
    }

    @PutMapping("/{id}")
    public ApiResponse<MenuResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MenuRequest request
    ) {
        return ApiResponse.success(rbacService.updateMenu(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        rbacService.deleteMenu(id);
        return ApiResponse.success(null);
    }
}
