CREATE TYPE match_status AS ENUM ('OPEN', 'FULL', 'CANCELLED', 'FINISHED');

CREATE TABLE matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(100) NOT NULL,
    location VARCHAR(255) NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    max_players INT NOT NULL CHECK (max_players > 0),
    status match_status NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
