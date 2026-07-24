package com.tsy.oa.user.position.mapper;

import com.tsy.oa.user.position.model.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PositionMapper {

    int insert(Position position);

    Position findById(Long id);

    List<Position> findAll();

    int update(Position position);

    int deleteById(Long id);

    int countByCode(@Param("code") String code, @Param("excludeId") Long excludeId);

    int countByDepartmentId(Long departmentId);

    int countEmployeesOutsideDepartment(
            @Param("positionId") Long positionId,
            @Param("departmentId") Long departmentId
    );
}
