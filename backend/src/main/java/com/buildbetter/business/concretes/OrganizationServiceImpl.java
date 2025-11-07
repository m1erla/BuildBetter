package com.buildbetter.business.concretes;

import com.buildbetter.business.abstracts.OrganizationService;
import com.buildbetter.core.utilities.exceptions.BusinessException;
import com.buildbetter.dataAccess.abstracts.OrganizationMemberRepository;
import com.buildbetter.dataAccess.abstracts.OrganizationRepository;
import com.buildbetter.dataAccess.abstracts.UserRepository;
import com.buildbetter.entities.concretes.Organization;
import com.buildbetter.entities.concretes.OrganizationMember;
import com.buildbetter.entities.concretes.User;
import com.buildbetter.enums.OrganizationRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Organization createOrganization(Organization organization, String ownerId) {
        // Validate slug uniqueness
        if (organization.getSlug() != null && organizationRepository.findBySlug(organization.getSlug()).isPresent()) {
            throw new BusinessException("Organization slug already exists");
        }

        Organization savedOrg = organizationRepository.save(organization);

        // Add owner as first member
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException("User not found"));

        OrganizationMember ownerMember = OrganizationMember.builder()
                .organization(savedOrg)
                .user(owner)
                .role(OrganizationRole.OWNER)
                .isActive(true)
                .build();

        memberRepository.save(ownerMember);

        return savedOrg;
    }

    @Override
    public Organization getOrganizationById(String id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Organization not found"));
    }

    @Override
    public Organization getOrganizationBySlug(String slug) {
        return organizationRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException("Organization not found"));
    }

    @Override
    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    @Override
    @Transactional
    public Organization updateOrganization(String id, Organization organization) {
        Organization existing = getOrganizationById(id);

        if (organization.getName() != null) {
            existing.setName(organization.getName());
        }
        if (organization.getDescription() != null) {
            existing.setDescription(organization.getDescription());
        }
        if (organization.getLogoUrl() != null) {
            existing.setLogoUrl(organization.getLogoUrl());
        }
        if (organization.getSettings() != null) {
            existing.setSettings(organization.getSettings());
        }

        return organizationRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteOrganization(String id) {
        Organization organization = getOrganizationById(id);
        organization.setActive(false);
        organizationRepository.save(organization);
    }

    @Override
    @Transactional
    public OrganizationMember addMember(String organizationId, String userId, OrganizationRole role) {
        Organization organization = getOrganizationById(organizationId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Check if already a member
        if (memberRepository.existsByOrganizationIdAndUserId(organizationId, userId)) {
            throw new BusinessException("User is already a member of this organization");
        }

        // Check quota
        if (!canAddUser(organizationId)) {
            throw new BusinessException("Organization has reached maximum user limit");
        }

        OrganizationMember member = OrganizationMember.builder()
                .organization(organization)
                .user(user)
                .role(role)
                .isActive(true)
                .build();

        return memberRepository.save(member);
    }

    @Override
    @Transactional
    public void removeMember(String organizationId, String userId) {
        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new BusinessException("Member not found"));

        // Prevent removing the last owner
        if (member.getRole() == OrganizationRole.OWNER) {
            long ownerCount = memberRepository.findByOrganizationIdAndRole(organizationId, OrganizationRole.OWNER).size();
            if (ownerCount <= 1) {
                throw new BusinessException("Cannot remove the last owner");
            }
        }

        memberRepository.delete(member);
    }

    @Override
    @Transactional
    public void updateMemberRole(String organizationId, String userId, OrganizationRole role) {
        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new BusinessException("Member not found"));

        member.setRole(role);
        memberRepository.save(member);
    }

    @Override
    public List<OrganizationMember> getOrganizationMembers(String organizationId) {
        return memberRepository.findByOrganizationId(organizationId);
    }

    @Override
    public boolean isMember(String organizationId, String userId) {
        return memberRepository.existsByOrganizationIdAndUserId(organizationId, userId);
    }

    @Override
    public boolean hasRole(String organizationId, String userId, OrganizationRole role) {
        return memberRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .map(member -> member.getRole() == role)
                .orElse(false);
    }

    @Override
    public boolean canAddUser(String organizationId) {
        Organization organization = getOrganizationById(organizationId);
        if (organization.getMaxUsers() == null) {
            return true; // Unlimited
        }
        long currentCount = memberRepository.countActiveMembers(organizationId);
        return currentCount < organization.getMaxUsers();
    }

    @Override
    public boolean canCreateAd(String organizationId) {
        Organization organization = getOrganizationById(organizationId);
        if (organization.getMaxAds() == null) {
            return true; // Unlimited
        }
        // This would need to count ads - simplified here
        return true;
    }

    @Override
    public long getCurrentMemberCount(String organizationId) {
        return memberRepository.countActiveMembers(organizationId);
    }

    @Override
    public long getCurrentAdCount(String organizationId) {
        // This would need actual ad counting logic
        return 0;
    }
}
