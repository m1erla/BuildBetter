package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {
    Optional<Organization> findBySlug(String slug);
    Optional<Organization> findByDomain(String domain);
    Optional<Organization> findByStripeCustomerId(String stripeCustomerId);
    List<Organization> findByIsActive(boolean isActive);

    @Query("SELECT o FROM Organization o WHERE o.subscription.id = :subscriptionId")
    Optional<Organization> findBySubscriptionId(String subscriptionId);

    @Query("SELECT COUNT(o) FROM Organization o WHERE o.isActive = true")
    long countActiveOrganizations();
}
