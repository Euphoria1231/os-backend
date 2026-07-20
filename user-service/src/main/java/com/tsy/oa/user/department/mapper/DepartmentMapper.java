package com.tsy.oa.user.department.mapper;

import com.tsy.oa.user.department.model.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DepartmentMapper {

    int insert(Department department);

    Department findById(Long id);

    List<Department> findAll();

    int update(Department department);

    int deleteById(Long id);

    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);
}
