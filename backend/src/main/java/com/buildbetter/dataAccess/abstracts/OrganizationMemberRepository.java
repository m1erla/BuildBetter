package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.OrganizationMember;
import com.buildbetter.enums.OrganizationRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, String> {
    List<OrganizationMember> findByOrganizationId(String organizationId);
    List<OrganizationMember> findByUserId(String userId);
    Optional<OrganizationMember> findByOrganizationIdAndUserId(String organizationId, String userId);
    List<OrganizationMember> findByOrganizationIdAndRole(String organizationId, OrganizationRole role);

    @Query("SELECT COUNT(om) FROM OrganizationMember om WHERE om.organization.id = :organizationId AND om.isActive = true")
    long countActiveMembers(String organizationId);

    boolean existsByOrganizationIdAndUserId(String organizationId, String userId);
}
