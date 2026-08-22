package com.ocnilentes.mediagenda.modules.patient.outbound.persistence;

import com.ocnilentes.mediagenda.modules.patient.domain.model.FollowUpStatus;
import com.ocnilentes.mediagenda.modules.patient.domain.model.Patient;
import com.ocnilentes.mediagenda.modules.patient.domain.port.PatientRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaPatientRepository implements PatientRepository {

    private final SpringDataPatientRepository springDataPatientRepository;

    JpaPatientRepository(SpringDataPatientRepository springDataPatientRepository) {
        this.springDataPatientRepository = springDataPatientRepository;
    }

    @Override
    public Optional<Patient> findByCedula(String cedula) {
        return springDataPatientRepository.findByCedula(cedula).map(this::toDomain);
    }

    @Override
    public Patient save(Patient patient) {
        PatientJpaEntity entity = springDataPatientRepository.findByCedula(patient.cedula())
                .orElseGet(() -> new PatientJpaEntity(patient.cedula(), patient.fullName(), patient.lastPhone()));
        entity.setFullName(patient.fullName());
        entity.setLastPhone(patient.lastPhone());
        entity.setLastAttendedAt(patient.lastAttendedAt());
        entity.setNextFollowUpOn(patient.nextFollowUpOn());
        entity.setFollowUpStatus(
                patient.followUpStatus() == null ? FollowUpStatus.NONE : patient.followUpStatus()
        );
        return toDomain(springDataPatientRepository.save(entity));
    }

    private Patient toDomain(PatientJpaEntity entity) {
        return new Patient(
                entity.getCedula(),
                entity.getFullName(),
                entity.getLastPhone(),
                entity.getLastAttendedAt(),
                entity.getNextFollowUpOn(),
                entity.getFollowUpStatus() == null ? FollowUpStatus.NONE : entity.getFollowUpStatus()
        );
    }
}
