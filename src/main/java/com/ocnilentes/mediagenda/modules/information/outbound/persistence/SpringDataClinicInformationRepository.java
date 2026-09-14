package com.ocnilentes.mediagenda.modules.information.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataClinicInformationRepository
        extends JpaRepository<ClinicInformationJpaEntity, String> {
}
