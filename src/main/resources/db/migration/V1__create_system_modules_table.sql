CREATE TABLE system_modules
(
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(100)             NOT NULL UNIQUE,
    name        VARCHAR(150)             NOT NULL,
    description VARCHAR(255)             NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO system_modules (code, name, description)
VALUES ('CORE_HEALTH', 'Core Health Module', 'Starter module used to validate application structure.');
