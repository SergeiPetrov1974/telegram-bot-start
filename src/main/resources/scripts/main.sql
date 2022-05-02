
 -- liquibase formatted sql

-- changeset sergei:1
CREATE TABLE notification_task
(
    id           INT,
    idChat       INT,
    notification text,
    dataTime     TIMESTAMP,
    PRIMARY KEY (id)
);

