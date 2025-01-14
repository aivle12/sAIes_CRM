package com.aivle.project.repository;

import com.aivle.project.entity.EmployeeEntity;
import com.aivle.project.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeEntity, String> {
    boolean existsByEmployeeId(String username);
    EmployeeEntity findByEmployeeId(String username);
    boolean existsByAccessPermission(Role role);

    @Query("SELECT e.employeeId FROM EmployeeEntity e WHERE e.employeeId LIKE CONCAT(:year, '%') ORDER BY e.employeeId DESC LIMIT 1")
    String findLastEmployeeIdByYear(@Param("year") String year);
}
