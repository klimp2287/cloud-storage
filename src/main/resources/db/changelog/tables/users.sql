CREATE TABLE users
(
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,

    CONSTRAINT pk_users PRIMARY KEY (username)
);