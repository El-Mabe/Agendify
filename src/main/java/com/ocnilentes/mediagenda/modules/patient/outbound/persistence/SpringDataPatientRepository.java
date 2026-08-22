package com.ocnilentes.mediagenda.modules.patient.outbound.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPatientRepository extends JpaRepository<PatientJpaEntity, String> {

    Optional<PatientJpaEntity> findByCedula(String cedula);
}
