CREATE TABLE Person
(
    id        SERIAL PRIMARY KEY,
    firstName VARCHAR(255),
    lastName  VARCHAR(255),
    age       INT
);

CREATE TABLE Address
(
    ownerId INT,
    street  VARCHAR,
    zip     Int
);

CREATE TABLE Robot
(
    ownerId INT,
    model   VARCHAR(255),
    age     INT
);

CREATE TABLE Product
(
    description VARCHAR(255),
    id          SERIAL PRIMARY KEY,
    sku         BIGINT
);

CREATE TABLE KmpTestEntity
(
    timeLocalDate        DATE,                     -- java.time.LocalDate
    timeLocalTime        TIME,                     -- java.time.LocalTime
    timeLocalDateTime    TIMESTAMP,                -- java.time.LocalDateTime
    timeInstant          TIMESTAMP WITH TIME ZONE, -- java.time.Instant
    timeLocalDateOpt     DATE,
    timeLocalTimeOpt     TIME,
    timeLocalDateTimeOpt TIMESTAMP,
    timeInstantOpt       TIMESTAMP WITH TIME ZONE
);

CREATE TABLE TimeEntity
(
    sqlDate            DATE,                     -- java.sql.Date
    sqlTime            TIME,                     -- java.sql.Time
    sqlTimestamp       TIMESTAMP,                -- java.sql.Timestamp
    timeLocalDate      DATE,                     -- java.time.LocalDate
    timeLocalTime      TIME,                     -- java.time.LocalTime
    timeLocalDateTime  TIMESTAMP,                -- java.time.LocalDateTime
    timeZonedDateTime  TIMESTAMP WITH TIME ZONE, -- java.time.ZonedDateTime
    timeInstant        TIMESTAMP WITH TIME ZONE, -- java.time.Instant
    -- Postgres actually has a notion of a Time+Timezone type unlike most DBs
    timeOffsetTime     TIME WITH TIME ZONE,      -- java.time.OffsetTime
    timeOffsetDateTime TIMESTAMP WITH TIME ZONE  -- java.time.OffsetDateTime
);

CREATE TABLE EncodingTestEntity
(
    stringMan    VARCHAR(255),
    booleanMan   BOOLEAN,
    byteMan      SMALLINT,
    shortMan     SMALLINT,
    intMan       INTEGER,
    longMan      BIGINT,
    floatMan     FLOAT,
    doubleMan    DOUBLE PRECISION,
    byteArrayMan BYTEA,
    customMan    VARCHAR(255),
    stringOpt    VARCHAR(255),
    booleanOpt   BOOLEAN,
    byteOpt      SMALLINT,
    shortOpt     SMALLINT,
    intOpt       INTEGER,
    longOpt      BIGINT,
    floatOpt     FLOAT,
    doubleOpt    DOUBLE PRECISION,
    byteArrayOpt BYTEA,
    customOpt    VARCHAR(255)
);

CREATE TABLE JsonbExample
(
    id    SERIAL PRIMARY KEY,
    value JSONB
);

CREATE TABLE JsonbExample2
(
    id     SERIAL PRIMARY KEY,
    value1 JSONB,
    value2 JSONB
);

CREATE TABLE JsonbExample3
(
    id     SERIAL PRIMARY KEY,
    value  JSONB,
    sample SERIAL
);

CREATE TABLE JsonExample
(
    id    SERIAL PRIMARY KEY,
    value JSON
);


CREATE TABLE MiscTest
(
    id    INTEGER NOT NULL PRIMARY KEY,
    value TEXT    NOT NULL
);

CREATE TABLE JavaTestEntity
(
    bigDecimalMan   DECIMAL(5, 2),
    javaUtilDateMan TIMESTAMP,
    uuidMan         UUID,
    bigDecimalOpt   DECIMAL(5, 2),
    javaUtilDateOpt TIMESTAMP,
    uuidOpt         UUID
);
