CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    name VARCHAR(250) NOT NULL
    );

CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
    );

CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    annotation VARCHAR(2000) NOT NULL,
    description VARCHAR(7000) NOT NULL,
    title VARCHAR(120) NOT NULL,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    initiator_id BIGINT NOT NULL REFERENCES users(id),
    lat FLOAT NOT NULL,
    lon FLOAT NOT NULL,
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    participant_limit INTEGER NOT NULL DEFAULT 0,
    request_moderation BOOLEAN NOT NULL DEFAULT TRUE,
    state VARCHAR(20) NOT NULL,
    created_on TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    published_on TIMESTAMP WITHOUT TIME ZONE,
    event_date TIMESTAMP WITHOUT TIME ZONE NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_events_state ON events (state);
CREATE INDEX IF NOT EXISTS idx_events_category ON events (category_id);
CREATE INDEX IF NOT EXISTS idx_events_initiator ON events (initiator_id);
CREATE INDEX IF NOT EXISTS idx_events_event_date ON events (event_date);

CREATE TABLE IF NOT EXISTS participation_requests (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id),
    requester_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uq_request UNIQUE (event_id, requester_id)
    );

CREATE INDEX IF NOT EXISTS idx_requests_event ON participation_requests (event_id);
CREATE INDEX IF NOT EXISTS idx_requests_requester ON participation_requests (requester_id);

CREATE TABLE IF NOT EXISTS compilations (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(50) NOT NULL UNIQUE,
    pinned BOOLEAN NOT NULL DEFAULT FALSE
    );

CREATE TABLE IF NOT EXISTS compilation_events (
    compilation_id BIGINT NOT NULL REFERENCES compilations(id) ON DELETE CASCADE,
    event_id BIGINT NOT NULL REFERENCES events(id),
    PRIMARY KEY (compilation_id, event_id)
    );