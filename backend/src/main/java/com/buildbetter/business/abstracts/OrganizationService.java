package com.buildbetter.business.abstracts;

import com.buildbetter.entities.concretes.Organization;
import com.buildbetter.entities.concretes.OrganizationMember;
import com.buildbetter.enums.OrganizationRole;

import java.util.List;

public interface OrganizationService {
    Organization createOrganization(Organization organization, String ownerId);
    Organization getOrganizationById(String id);
    Organization getOrganizationBySlug(String slug);
    List<Organization> getAllOrganizations();
    Organization updateOrganization(String id, Organization organization);
    void deleteOrganization(String id);

    // Member management
    OrganizationMember addMember(String organizationId, String userId, OrganizationRole role);
    void removeMember(String organizationId, String userId);
    void updateMemberRole(String organizationId, String userId, OrganizationRole role);
    List<OrganizationMember> getOrganizationMembers(String organizationId);
    boolean isMember(String organizationId, String userId);
    boolean hasRole(String organizationId, String userId, OrganizationRole role);

    // Usage limits
    boolean canAddUser(String organizationId);
    boolean canCreateAd(String organizationId);
    long getCurrentMemberCount(String organizationId);
    long getCurrentAdCount(String organizationId);
}
