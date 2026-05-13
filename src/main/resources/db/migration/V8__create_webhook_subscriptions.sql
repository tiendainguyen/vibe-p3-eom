CREATE TABLE webhook_subscriptions (
    id         BIGSERIAL PRIMARY KEY,
    url        VARCHAR(2048) NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE webhook_subscription_event_types (
    subscription_id BIGINT      NOT NULL REFERENCES webhook_subscriptions (id) ON DELETE CASCADE,
    event_type      VARCHAR(50) NOT NULL,
    PRIMARY KEY (subscription_id, event_type)
);
