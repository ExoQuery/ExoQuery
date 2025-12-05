CREATE TABLE Person
(
    id        INTEGER PRIMARY KEY,
    firstName VARCHAR(255),
    lastName  VARCHAR(255),
    age       INT
);

CREATE TABLE Address
(
    ownerId INT,
    street  VARCHAR,
    zip     INT
);

CREATE TABLE Robot
(
    ownerId INT,
    model   VARCHAR(255),
    age     INT
);

CREATE TABLE IF NOT EXISTS Product
(
    id
    INTEGER
    PRIMARY
    KEY,
    description
    VARCHAR
(
    255
),
    sku BIGINT
    );

CREATE TABLE KmpTestEntity
(
    timeLocalDate        INTEGER, -- java.time.LocalDate
    timeLocalTime        INTEGER, -- java.time.LocalTime
    timeLocalDateTime    INTEGER, -- java.time.LocalDateTime
    timeInstant          INTEGER, -- java.time.Instant
    timeLocalDateOpt     INTEGER,
    timeLocalTimeOpt     INTEGER, -- java.time.LocalTime
    timeLocalDateTimeOpt INTEGER, -- java.time.LocalDateTime
    timeInstantOpt       INTEGER  -- java.time.Instant
);

-- Sqlite is a bit crazy, they don't have any actual date types:
-- https://www.sqlite.org/datatype3.html
CREATE TABLE TimeEntity
(
    sqlDate            INTEGER, -- java.sql.Date
    sqlTime            INTEGER, -- java.sql.Time
    sqlTimestamp       INTEGER, -- java.sql.Timestamp
    timeLocalDate      INTEGER, -- java.time.LocalDate
    timeLocalTime      INTEGER, -- java.time.LocalTime
    timeLocalDateTime  INTEGER, -- java.time.LocalDateTime
    timeZonedDateTime  INTEGER, -- java.time.ZonedDateTime
    timeInstant        INTEGER, -- java.time.Instant
    timeOffsetTime     INTEGER, -- java.time.OffsetTime
    timeOffsetDateTime INTEGER  -- java.time.OffsetDateTime
);

CREATE TABLE IF NOT EXISTS EncodingTestEntity
(
    stringMan
    VARCHAR
(
    255
),
    booleanMan BOOLEAN,
    byteMan SMALLINT,
    shortMan SMALLINT,
    intMan INTEGER,
    longMan BIGINT,
    floatMan FLOAT,
    doubleMan DOUBLE PRECISION,
    byteArrayMan BLOB,
    customMan VARCHAR
(
    255
),
    stringOpt VARCHAR
(
    255
),
    booleanOpt BOOLEAN,
    byteOpt SMALLINT,
    shortOpt SMALLINT,
    intOpt INTEGER,
    longOpt BIGINT,
    floatOpt FLOAT,
    doubleOpt DOUBLE PRECISION,
    byteArrayOpt BLOB,
    customOpt VARCHAR
(
    255
)
    );

CREATE TABLE IF NOT EXISTS JsonbExample
(
    id    INTEGER PRIMARY KEY,
    value BLOB
);

CREATE TABLE IF NOT EXISTS JsonbExample2
(
    id     INTEGER PRIMARY KEY,
    value1 BLOB,
    value2 BLOB
);

CREATE TABLE IF NOT EXISTS JsonbExample3
(
    id     INTEGER PRIMARY KEY,
    value  BLOB,
    sample INTEGER
);

CREATE TABLE IF NOT EXISTS JsonExample
(
    id    INTEGER PRIMARY KEY,
    value TEXT
);

CREATE TABLE IF NOT EXISTS JavaTestEntity
(
    bigDecimalMan
    DECIMAL
(
    5,
    2
),
    javaUtilDateMan BIGINT,
    uuidMan VARCHAR
(
    36
),
    bigDecimalOpt DECIMAL
(
    5,
    2
),
    javaUtilDateOpt BIGINT,
    uuidOpt VARCHAR
(
    36
)
    );
