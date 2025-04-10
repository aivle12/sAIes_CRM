package com.aivle.project.entity;

import com.aivle.project.enums.Dept;
import com.aivle.project.enums.Position;
import com.aivle.project.enums.Role;
import com.aivle.project.enums.Team;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "employee")
public class EmployeeEntity {
    @Id
    private String employeeId;

    @Column(length = 50, nullable = false)
    private String employeeName;

    @Column(nullable = false)
    private LocalDate hireDate;

    @Column
    private LocalDate terminationDate;

    @Column
    private Float baseSalary;

    @Enumerated(EnumType.STRING)
    private Position position;

    @Enumerated(EnumType.STRING)
    private Role accessPermission;

    @Column(nullable = false)
    private String password;

    @Column(length = 30, nullable = false)
    private String passwordAnswer;

    @Enumerated(EnumType.STRING)
    private Dept departmentId;

    @Enumerated(EnumType.STRING)
    private Team teamId;

    // 외부 외래키

    @OneToMany(mappedBy = "employeeId", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH})
    private List<OpportunitiesEntity> opportunities;

    @OneToMany(mappedBy = "employeeId", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH})
    private List<ContractsEntity> contracts;

    @OneToMany(mappedBy = "employeeId", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH})
    private List<LeadsEntity> leads;

    @OneToMany(mappedBy = "employeeId", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH})
    private List<AccountEntity> accounts;

    @OneToMany(mappedBy = "employeeId", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH})
    private List<OrdersEntity> orders;

    @OneToMany(mappedBy = "employeeId", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH})
    private List<ProductsEntity> products;
}
