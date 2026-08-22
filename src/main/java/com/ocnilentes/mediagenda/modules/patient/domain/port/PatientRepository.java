package com.ocnilentes.mediagenda.modules.patient.domain.port;

import com.ocnilentes.mediagenda.modules.patient.domain.model.Patient;
import java.util.Optional;

public interface PatientRepository {

    Optional<Patient> findByCedula(String cedula);

    Patient save(Patient patient);
}
