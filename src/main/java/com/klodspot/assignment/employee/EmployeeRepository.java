package com.klodspot.assignment.employee;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByIdAndOwnerId(Long id, Long ownerId);
    boolean existsByEmailIgnoreCaseAndOwnerId(String email, Long ownerId);
    boolean existsByEmailIgnoreCaseAndIdNotAndOwnerId(String email, Long id, Long ownerId);
    long countByOwnerId(Long ownerId);
    long countByStatusAndOwnerId(EmploymentStatus status, Long ownerId);

    @Query(
        """
        select count(distinct e.department) from Employee e
        where e.ownerId = :ownerId or (e.ownerId is null and :ownerId is null)
        """
    )
    long countDepartments(@Param("ownerId") Long ownerId);

    @Modifying(clearAutomatically = true)
    @Query("update Employee e set e.ownerId = :ownerId where e.ownerId is null")
    int assignUnownedEmployees(@Param("ownerId") Long ownerId);
}
