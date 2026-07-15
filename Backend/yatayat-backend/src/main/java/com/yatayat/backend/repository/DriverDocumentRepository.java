package com.yatayat.backend.repository;

import com.yatayat.backend.entity.DriverDocument;
import com.yatayat.backend.entity.DriverProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverDocumentRepository
        extends JpaRepository<DriverDocument, Long> {

    List<DriverDocument> findByDriverProfile(DriverProfile profile);
}