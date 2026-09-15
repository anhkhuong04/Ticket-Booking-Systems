CREATE TABLE IF NOT EXISTS integration_test_fixture (
    id UUID PRIMARY KEY,
    label VARCHAR(100) NOT NULL
);

TRUNCATE TABLE integration_test_fixture;

INSERT INTO integration_test_fixture (id, label)
VALUES ('00000000-0000-0000-0000-000000000001', 'minimal-fixture');
