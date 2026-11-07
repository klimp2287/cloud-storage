CREATE TABLE users_user_files
(
    user_username       VARCHAR(255) NOT NULL,
    user_files_filename VARCHAR(255) NOT NULL,

    CONSTRAINT fk_user_files_user
        FOREIGN KEY (user_username)
            REFERENCES users (username),

    CONSTRAINT fk_user_files_file
        FOREIGN KEY (user_files_filename)
            REFERENCES files (filename),

    CONSTRAINT pk_users_user_files
        PRIMARY KEY (user_username, user_files_filename)
);