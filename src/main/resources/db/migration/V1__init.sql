-- initialize users table
CREATE TABLE users (
             id BIGSERIAL PRIMARY KEY,
             email VARCHAR(255) NOT NULL UNIQUE,
             first_name VARCHAR(255) NOT NULL,
             last_name VARCHAR(255) NOT NULL,
             phone_number VARCHAR(255) NOT NULL,
             created_on TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
             updated_on TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
             version BIGINT
);
