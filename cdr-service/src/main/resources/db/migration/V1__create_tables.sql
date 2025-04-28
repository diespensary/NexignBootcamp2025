CREATE TABLE subscriber (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    msisdn VARCHAR(11) NOT NULL UNIQUE,
    operator VARCHAR(50) NOT NULL
);

CREATE TABLE cdr_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    call_type VARCHAR(2),
    served_subscriber VARCHAR(11),
    another_subscriber VARCHAR(11),
    start_time TIMESTAMP WITHOUT TIME ZONE,
    end_time TIMESTAMP WITHOUT TIME ZONE
);
