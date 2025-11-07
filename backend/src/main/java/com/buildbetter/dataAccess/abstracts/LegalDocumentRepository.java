package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.LegalDocument;
import com.buildbetter.enums.LegalDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LegalDocumentRepository extends JpaRepository<LegalDocument, String> {
    List<LegalDocument> findByType(LegalDocumentType type);
    Optional<LegalDocument> findByTypeAndIsCurrent(LegalDocumentType type, boolean isCurrent);
    Optional<LegalDocument> findByTypeAndVersion(LegalDocumentType type, String version);
}
