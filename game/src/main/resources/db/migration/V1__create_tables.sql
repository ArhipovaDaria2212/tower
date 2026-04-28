-- V1__create_tables.sql
-- Initial database schema for Tower game

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    device_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

-- Playthroughs table
CREATE TABLE playthroughs (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    current_floor_id UUID,
    player_x FLOAT,
    player_y FLOAT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_playthrough_user ON playthroughs(user_id);

-- Floors table
CREATE TABLE floors (
    id UUID PRIMARY KEY,
    playthrough_id UUID NOT NULL REFERENCES playthroughs(id) ON DELETE CASCADE,
    floor_number INTEGER NOT NULL,
    seed BIGINT NOT NULL,
    generator_version INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE(playthrough_id, floor_number)
);

CREATE INDEX idx_floor_playthrough ON floors(playthrough_id, floor_number);

-- Fog chunks table
CREATE TABLE fog_chunks (
    id UUID PRIMARY KEY,
    floor_id UUID NOT NULL REFERENCES floors(id) ON DELETE CASCADE,
    chunk_x INTEGER NOT NULL,
    chunk_y INTEGER NOT NULL,
    mask BYTEA,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE(floor_id, chunk_x, chunk_y)
);

CREATE INDEX idx_fog_chunk_floor ON fog_chunks(floor_id);

-- Entitlements table (for paywall)
CREATE TABLE entitlements (
    id UUID PRIMARY KEY,
    user_id UUID,
    max_unlocked_floor INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_entitlements_user ON entitlements(user_id);
