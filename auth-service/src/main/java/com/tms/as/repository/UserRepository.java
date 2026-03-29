package com.tms.as.repository;

import com.tms.as.entity.Role;
import com.tms.as.entity.Status;
import com.tms.as.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    boolean existsByEmployeeCode(String employeeCode);

    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (:status is null or u.status = :status)
              and (
                    :search is null
                    or lower(u.fullName) like lower(concat('%', :search, '%'))
                    or lower(u.email) like lower(concat('%', :search, '%'))
                    or lower(u.employeeCode) like lower(concat('%', :search, '%'))
              )
            order by u.createdAt desc, u.fullName asc
            """)
    List<User> findUsersForAdmin(@Param("role") Role role,
                                 @Param("status") Status status,
                                 @Param("search") String search);

    @Query("""
            select u from User u
            where u.role in :roles
              and u.status = :status
              and (
                    :search is null
                    or lower(u.fullName) like lower(concat('%', :search, '%'))
                    or lower(u.email) like lower(concat('%', :search, '%'))
                    or lower(u.employeeCode) like lower(concat('%', :search, '%'))
              )
            order by u.fullName asc
            """)
    List<User> findAssignableManagers(@Param("roles") Collection<Role> roles,
                                      @Param("status") Status status,
                                      @Param("search") String search);
}
