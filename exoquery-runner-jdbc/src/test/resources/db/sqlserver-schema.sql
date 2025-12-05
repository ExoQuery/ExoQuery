CREATE TABLE Person
(
    id        INT IDENTITY(1,1) PRIMARY KEY,
    firstName VARCHAR(255),
    lastName  VARCHAR(255),
    age       INT
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
    id          INTEGER IDENTITY(1,1) PRIMARY KEY,
    description VARCHAR(255),
    sku         BIGINT
);

CREATE TABLE KmpTestEntity
(
    timeLocalDate        DATE,           -- java.time.LocalDate
    timeLocalTime        TIME,           -- java.time.LocalTime
    timeLocalDateTime    DATETIME,       -- java.time.LocalDateTime
    timeInstant          DATETIMEOFFSET, -- java.time.Instant
    timeLocalDateOpt     DATE,
    timeLocalTimeOpt     TIME,           -- java.time.LocalTime
    timeLocalDateTimeOpt DATETIME,       -- java.time.LocalDateTime
    timeInstantOpt       DATETIMEOFFSET  -- java.time.Instant
);

CREATE TABLE TimeEntity
(
    sqlDate            DATE,           -- java.sql.Date
    sqlTime            TIME,           -- java.sql.Time
    sqlTimestamp       DATETIME,       -- java.sql.Timestamp
    timeLocalDate      DATE,           -- java.time.LocalDate
    timeLocalTime      TIME,           -- java.time.LocalTime
    timeLocalDateTime  DATETIME,       -- java.time.LocalDateTime
    -- DATETIMEOFFSET is SQL Server's equvalent of Postgres TIMESTAMP WITH TIME ZONE
    timeZonedDateTime  DATETIMEOFFSET, -- java.time.ZonedDateTime
    timeInstant        DATETIMEOFFSET, -- java.time.Instant
    -- There is no such thing as a Time+Timezone column in SQL Server
    timeOffsetTime     DATETIMEOFFSET, -- java.time.OffsetTime
    timeOffsetDateTime DATETIMEOFFSET  -- java.time.OffsetDateTime
);

CREATE TABLE EncodingTestEntity
(
    stringMan    VARCHAR(255),
    booleanMan   BIT,
    byteMan      SMALLINT,
    shortMan     SMALLINT,
    intMan       INTEGER,
    longMan      BIGINT,
    floatMan     FLOAT,
    doubleMan    DOUBLE PRECISION,
    byteArrayMan VARBINARY(MAX),
    customMan    VARCHAR(255),
    stringOpt    VARCHAR(255),
    booleanOpt   BIT,
    byteOpt      SMALLINT,
    shortOpt     SMALLINT,
    intOpt       INTEGER,
    longOpt      BIGINT,
    floatOpt     FLOAT,
    doubleOpt    DOUBLE PRECISION,
    byteArrayOpt VARBINARY(MAX),
    customOpt    VARCHAR(255)
);

CREATE TABLE JavaTestEntity(
    bigDecimalMan DECIMAL(5,2),
    javaUtilDateMan DATETIMEOFFSET, -- java.util.Date i.e. legacy date with time zone
    uuidMan VARCHAR(255),
    bigDecimalOpt DECIMAL(5,2),
    javaUtilDateOpt DATETIMEOFFSET, -- java.util.Date i.e. legacy date with time zone
    uuidOpt VARCHAR(255)
);

CREATE TABLE JsonExample(
    id INT PRIMARY KEY,
    value VARCHAR(255)
);
