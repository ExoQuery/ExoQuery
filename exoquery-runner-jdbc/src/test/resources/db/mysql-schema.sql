CREATE TABLE Person
(
    id        INT NOT NULL AUTO_INCREMENT,
    firstName VARCHAR(255),
    lastName  VARCHAR(255),
    age       INT,
    PRIMARY KEY (id)
);

CREATE TABLE Address
(
    ownerId INT,
    street  VARCHAR(255),
    zip     INT
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
    id          BIGINT NOT NULL AUTO_INCREMENT,
    sku         BIGINT,
    PRIMARY KEY (id)
);

CREATE TABLE KmpTestEntity
(
    timeLocalDate        DATE,      -- java.time.LocalDate
    timeLocalTime        TIME,      -- java.time.LocalTime
    timeLocalDateTime    TIMESTAMP, -- java.time.LocalDateTime
    timeInstant          DATETIME,  -- java.time.Instant
    timeLocalDateOpt     DATE,
    timeLocalTimeOpt     TIME,
    timeLocalDateTimeOpt TIMESTAMP,
    timeInstantOpt       DATETIME
);

CREATE TABLE TimeEntity
(
    sqlDate            DATE,      -- java.sql.Date
    sqlTime            TIME,      -- java.sql.Time
    sqlTimestamp       TIMESTAMP, -- java.sql.Timestamp
    timeLocalDate      DATE,      -- java.time.LocalDate
    timeLocalTime      TIME,      -- java.time.LocalTime
    timeLocalDateTime  TIMESTAMP, -- java.time.LocalDateTime
    -- MySQL has no understanding of Date+Timezone or Time+Timezone
    -- The only thing you can do is use DATETIME which at least tries
    -- not to convert to/from UTC whenever it is written.
    -- More info here: https://dev.mysql.com/doc/refman/8.0/en/datetime.html
    timeZonedDateTime  DATETIME,  -- java.time.ZonedDateTime
    timeInstant        DATETIME,  -- java.time.Instant
    timeOffsetTime     TIME,      -- java.time.OffsetTime
    timeOffsetDateTime DATETIME   -- java.time.OffsetDateTime
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
    doubleMan DOUBLE,
    byteArrayMan VARBINARY(255),
    customMan    VARCHAR(255),
    stringOpt    VARCHAR(255),
    booleanOpt   BOOLEAN,
    byteOpt      SMALLINT,
    shortOpt     SMALLINT,
    intOpt       INTEGER,
    longOpt      BIGINT,
    floatOpt     FLOAT,
    doubleOpt DOUBLE,
    byteArrayOpt VARBINARY(255),
    customOpt    VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS JsonExample
(
    id    INTEGER PRIMARY KEY,
    value JSON
);

CREATE TABLE JavaTestEntity
(
    bigDecimalMan   DECIMAL(5, 2),
    javaUtilDateMan DATETIME,
    uuidMan         VARCHAR(255),
    bigDecimalOpt   DECIMAL(5, 2),
    javaUtilDateOpt DATETIME,
    uuidOpt         VARCHAR(255)
);
