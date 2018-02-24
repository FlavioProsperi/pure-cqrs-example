DROP TABLE IF EXISTS events;

CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    payload JSON NOT NULL
);

DROP TABLE IF EXISTS damaged_cars;

CREATE TABLE damaged_cars (
    id UUID PRIMARY KEY,
    registration_plate VARCHAR NOT NULL,
    status VARCHAR NOT NULL
);