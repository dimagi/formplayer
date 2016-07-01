CREATE TABLE formplayer_sessions (
    id text NOT NULL,
    instancexml text,
    formxml text,
    restorexml text,
    username text,
    initlang text,
    sequenceid text,
    domain text,
    posturl text,
    sessiondata bytea,
    CONSTRAINT sessions_pkey PRIMARY KEY (id)
)