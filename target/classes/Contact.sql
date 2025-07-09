CREATE TABLE
    contact (
        id Number NOT NULL,
        email Text NOT NULL,
        login Text NOT NULL,
        password Text NOT NULL,
        first_name Text,
        last_name Text,
        title Text,
        job_title Text,
        company Text,
        website Text,
        notes Text,
        photo_url Text,
        photo_content_type Text,
        last_contact_date TIMESTAMP(0),
        ROWID Number,
        ROWMODID Number,
        uuid Text DEFAULT Get(UUID),
        sku Text,
        create_timestamp TIMESTAMP(0),
        update_timestamp TIMESTAMP(0)
    );

CREATE INDEX email ON contact (email);

CREATE INDEX login ON contact (login);

CREATE INDEX first_name ON contact (first_name);

CREATE INDEX last_name ON contact (last_name);

CREATE INDEX title ON contact (title);

CREATE INDEX job_title ON contact (job_title);

CREATE INDEX company ON contact (company);

CREATE INDEX website ON contact (website);

CREATE INDEX photo_url ON contact (photo_url);

CREATE INDEX photo_content_type ON contact (photo_content_type);

CREATE INDEX id ON contact (id);