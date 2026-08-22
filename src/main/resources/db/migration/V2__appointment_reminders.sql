CREATE TABLE appointment_reminders (
    id               BIGSERIAL PRIMARY KEY,
    appointment_key  VARCHAR(64)  NOT NULL,
    channel          VARCHAR(32)  NOT NULL,
    phone            VARCHAR(30)  NOT NULL,
    patient_name     VARCHAR(200) NOT NULL,
    cedula           VARCHAR(20),
    appointment_date DATE         NOT NULL,
    appointment_time TIME         NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    meta_message_id  VARCHAR(128),
    error_message    VARCHAR(500),
    sent_at          TIMESTAMP,
    responded_at     TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_appointment_reminders_key_channel UNIQUE (appointment_key, channel)
);

CREATE INDEX idx_appointment_reminders_phone_status
    ON appointment_reminders (phone, status);
