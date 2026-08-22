package com.ocnilentes.mediagenda.modules.patient.application;

import com.ocnilentes.mediagenda.modules.patient.domain.exception.PatientException;
import com.ocnilentes.mediagenda.modules.patient.domain.model.FollowUpStatus;
import com.ocnilentes.mediagenda.modules.patient.domain.model.Patient;
import com.ocnilentes.mediagenda.modules.patient.domain.port.PatientRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final int followUpIntervalMonths;

    public PatientService(
            PatientRepository patientRepository,
            @Value("${follow-up.interval-months:11}") int followUpIntervalMonths
    ) {
        this.patientRepository = patientRepository;
        this.followUpIntervalMonths = followUpIntervalMonths > 0 ? followUpIntervalMonths : 11;
    }

    public Optional<Patient> findByCedula(String rawCedula) {
        String cedula = CedulaValidator.normalizeAndValidate(rawCedula);
        return patientRepository.findByCedula(cedula);
    }

    public Patient registerOrUpdate(String rawCedula, String fullName, String phone) {
        String cedula = CedulaValidator.normalizeAndValidate(rawCedula);
        requireText(fullName, "El nombre del paciente es obligatorio");
        requireText(phone, "El teléfono es obligatorio");

        Patient existing = patientRepository.findByCedula(cedula).orElse(null);
        Patient patient = new Patient(
                cedula,
                fullName.trim(),
                phone.trim(),
                existing == null ? null : existing.lastAttendedAt(),
                existing == null ? null : existing.nextFollowUpOn(),
                existing == null || existing.followUpStatus() == null
                        ? FollowUpStatus.NONE
                        : existing.followUpStatus()
        );
        return patientRepository.save(patient);
    }

    public Patient touchPhone(String rawCedula, String phone) {
        String cedula = CedulaValidator.normalizeAndValidate(rawCedula);
        requireText(phone, "El teléfono es obligatorio");

        Patient existing = patientRepository.findByCedula(cedula)
                .orElseThrow(() -> new PatientException("Paciente no encontrado"));
        return patientRepository.save(new Patient(
                cedula,
                existing.fullName(),
                phone.trim(),
                existing.lastAttendedAt(),
                existing.nextFollowUpOn(),
                existing.followUpStatus()
        ));
    }

    public Patient markAttended(String rawCedula, String fullName, String phone, LocalDate attendedDate) {
        String cedula = CedulaValidator.normalizeAndValidate(rawCedula);
        requireText(fullName, "El nombre del paciente es obligatorio");
        requireText(phone, "El teléfono es obligatorio");
        if (attendedDate == null) {
            throw new PatientException("La fecha de asistencia es obligatoria");
        }

        Patient existing = patientRepository.findByCedula(cedula).orElse(null);
        LocalDate nextFollowUp = attendedDate.plusMonths(followUpIntervalMonths);
        Patient patient = new Patient(
                cedula,
                fullName.trim(),
                phone.trim(),
                attendedDate,
                nextFollowUp,
                FollowUpStatus.ACTIVE
        );
        if (existing != null
                && existing.lastAttendedAt() != null
                && existing.lastAttendedAt().isAfter(attendedDate)) {
            // No retroceder una asistencia más reciente.
            return existing;
        }
        return patientRepository.save(patient);
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new PatientException(message);
        }
    }
}
