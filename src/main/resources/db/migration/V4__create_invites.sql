CREATE TABLE invites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL UNIQUE,
    expires_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
