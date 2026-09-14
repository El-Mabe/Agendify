CREATE TABLE appointments (
    id                    VARCHAR(64)  PRIMARY KEY,
    appointment_date      DATE         NOT NULL,
    appointment_time      TIME         NOT NULL,
    consultorio           VARCHAR(8)   NOT NULL,
    patient_name          VARCHAR(200) NOT NULL,
    cedula                VARCHAR(20),
    phone                 VARCHAR(30)  NOT NULL,
    asistencia            VARCHAR(32),
    status                VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    origin                VARCHAR(20)  NOT NULL DEFAULT 'BOT',
    legacy_key            VARCHAR(64),
    sheet_name            VARCHAR(100),
    sheet_row_number      INTEGER,
    version               BIGINT       NOT NULL DEFAULT 0,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_appointments_status CHECK (status IN ('SCHEDULED', 'CANCELLED')),
    CONSTRAINT chk_appointments_origin CHECK (origin IN ('BOT', 'SHEET', 'IMPORT')),
    CONSTRAINT chk_appointments_consultorio CHECK (consultorio IN ('A', 'B')),
    CONSTRAINT chk_appointments_sheet_row CHECK (sheet_row_number IS NULL OR sheet_row_number > 0)
);

CREATE UNIQUE INDEX uq_appointments_active_cedula_date
    ON appointments (cedula, appointment_date)
    WHERE status = 'SCHEDULED' AND cedula IS NOT NULL AND cedula <> '';

CREATE UNIQUE INDEX uq_appointments_active_slot
    ON appointments (appointment_date, appointment_time, consultorio)
    WHERE status = 'SCHEDULED';

CREATE UNIQUE INDEX uq_appointments_sheet_location
    ON appointments (sheet_name, sheet_row_number)
    WHERE status = 'SCHEDULED'
      AND sheet_name IS NOT NULL
      AND sheet_row_number IS NOT NULL;

CREATE UNIQUE INDEX uq_appointments_active_legacy_key
    ON appointments (legacy_key)
    WHERE status = 'SCHEDULED' AND legacy_key IS NOT NULL;

CREATE INDEX idx_appointments_phone_status
    ON appointments (phone, status);

CREATE INDEX idx_appointments_date_status
    ON appointments (appointment_date, status);

CREATE TABLE appointment_sheet_sync (
    id                    BIGSERIAL    PRIMARY KEY,
    appointment_id        VARCHAR(64)  NOT NULL,
    appointment_version   BIGINT       NOT NULL,
    operation             VARCHAR(20)  NOT NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts              INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at       TIMESTAMP,
    locked_at             TIMESTAMP,
    last_error            VARCHAR(1000),
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appointment_sheet_sync_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT uq_appointment_sheet_sync_version
        UNIQUE (appointment_id, appointment_version, operation),
    CONSTRAINT chk_appointment_sheet_sync_operation
        CHECK (operation IN ('UPSERT', 'DELETE', 'ATTENDANCE')),
    CONSTRAINT chk_appointment_sheet_sync_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'SYNCED', 'FAILED'))
);

CREATE INDEX idx_appointment_sheet_sync_pending
    ON appointment_sheet_sync (status, next_attempt_at);

CREATE TABLE clinic_holidays (
    holiday_date DATE PRIMARY KEY,
    synced_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE clinic_information (
    info_key    VARCHAR(100) PRIMARY KEY,
    info_value TEXT         NOT NULL,
    synced_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE appointment_sheet_inbox (
    event_id       VARCHAR(64) PRIMARY KEY,
    appointment_id VARCHAR(64),
    received_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
