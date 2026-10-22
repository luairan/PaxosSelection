CREATE TABLE IF NOT EXISTS business_fence (
    resource_namespace VARCHAR(64) PRIMARY KEY,
    fence_token BIGINT NOT NULL CHECK (fence_token >= 0),
    owner_id VARCHAR(64) NOT NULL
);
CREATE TABLE IF NOT EXISTS business_entry (
    resource_namespace VARCHAR(64) NOT NULL REFERENCES business_fence(resource_namespace),
    entry_key VARCHAR(128) NOT NULL,
    entry_value VARCHAR(4096) NOT NULL,
    fence_token BIGINT NOT NULL,
    PRIMARY KEY (resource_namespace, entry_key)
);
