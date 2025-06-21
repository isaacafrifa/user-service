-- Add indexes for free form search fields
-- These indexes will improve performance of the free form text search functionality

-- Add index on first_name field in users table
CREATE INDEX idx_users_first_name ON users(first_name);

-- Add index on last_name field in users table
CREATE INDEX idx_users_last_name ON users(last_name);

-- Add index on phone_number field in users table
CREATE INDEX idx_users_phone_number ON users(phone_number);