ALTER TABLE patients
    ADD COLUMN last_attended_at DATE,
    ADD COLUMN next_follow_up_on DATE,
    ADD COLUMN follow_up_status VARCHAR(32) NOT NULL DEFAULT 'NONE';

CREATE TABLE appointment_events (
    id               BIGSERIAL PRIMARY KEY,
    appointment_key  VARCHAR(64),
    event_type       VARCHAR(32)  NOT NULL,
    cedula           VARCHAR(20),
    patient_name     VARCHAR(200),
    phone            VARCHAR(30)  NOT NULL,
    appointment_date DATE,
    appointment_time TIME,
    consultorio      VARCHAR(8),
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_appointment_events_cedula_created
    ON appointment_events (cedula, created_at DESC);

CREATE INDEX idx_appointment_events_key_type
    ON appointment_events (appointment_key, event_type);

CREATE TABLE attendance_sync (
    appointment_key  VARCHAR(64) PRIMARY KEY,
    attended_date    DATE        NOT NULL,
    cedula           VARCHAR(20),
    synced_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
