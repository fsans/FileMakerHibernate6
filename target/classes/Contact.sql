CREATE TABLE
    contact (
        id Number NOT NULL,
        first_name Text,
        last_name Text,
        photo_url Text,
        title Text,
        company Text,
        job_title Text,
        website Text,
        photo_content_type Text,
        notes Text,
        uuid Text DEFAULT Get (UUID),
        mod_id Number DEFAULT Get (RecordModificationCount),
        update_time TIMESTAMP(0),
        create_time TIMESTAMP(0),
        update_unix_time Number DEFAULT UnixTime (update_time),
        email Text,
        password Text,
        name Text,
        login Text
    );

CREATE INDEX first_name ON contact (first_name);

CREATE INDEX last_name ON contact (last_name);

CREATE INDEX photo_url ON contact (photo_url);

CREATE INDEX title ON contact (title);

CREATE INDEX company ON contact (company);

CREATE INDEX job_title ON contact (job_title);

CREATE INDEX website ON contact (website);

CREATE INDEX id ON contact (id);

CREATE INDEX photo_content_type ON contact (photo_content_type);

CREATE INDEX mod_id ON contact (mod_id);

CREATE INDEX update_time ON contact (update_time);

CREATE INDEX update_unix_time ON contact (update_unix_time);

CREATE INDEX email ON contact (email);

CREATE INDEX name ON contact (name);