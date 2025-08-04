CREATE SCHEMA IF NOT EXISTS local;

CREATE TABLE local.products (
    id UUID PRIMARY KEY,
    article_number VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(255) NOT NULL,
    price DECIMAL NOT NULL,
    unit VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);