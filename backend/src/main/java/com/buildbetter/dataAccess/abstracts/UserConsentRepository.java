package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.UserConsent;
import com.buildbetter.enums.LegalDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, String> {
    List<UserConsent> findByUserId(String userId);
    Optional<UserConsent> findByUserIdAndDocumentType(String userId, LegalDocumentType documentType);
    Optional<UserConsent> findByUserIdAndDocumentTypeAndDocumentVersion(String userId, LegalDocumentType documentType, String documentVersion);
}
