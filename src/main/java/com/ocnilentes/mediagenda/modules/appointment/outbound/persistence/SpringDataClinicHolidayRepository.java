package com.ocnilentes.mediagenda.modules.appointment.outbound.persistence;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataClinicHolidayRepository
        extends JpaRepository<ClinicHolidayJpaEntity, LocalDate> {
}
