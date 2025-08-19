CREATE SCHEMA purposely_inconsistent;

CREATE TABLE purposely_inconsistent."UserProfile" (
  "userId" SERIAL PRIMARY KEY,
  "firstName" VARCHAR(80) NOT NULL,
  last_name VARCHAR(80) NOT NULL,
  updated_at TIMESTAMP
);

CREATE TABLE purposely_inconsistent.user_profile_details (
  user_profile_detail_id INTEGER PRIMARY KEY,
  user_id INTEGER NOT NULL,
  address_line1 VARCHAR(200),
  addressLine2 VARCHAR(200),
  city VARCHAR(100),
  state_code CHAR(2),
  postal_code VARCHAR(20),
  birth_date DATE,
  preferred_contact_time TIME,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

create schema gen_type_test;

CREATE TABLE gen_type_test.KmpTestEntity
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

CREATE TABLE gen_type_test.TimeEntity
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

-- CREATE TABLE gen_type_test.EncodingTestEntity
-- (
--     stringMan    VARCHAR(255),
--     booleanMan   BOOLEAN,
--     byteMan      SMALLINT,
--     shortMan     SMALLINT,
--     intMan       INTEGER,
--     longMan      BIGINT,
--     floatMan     FLOAT,
--     doubleMan    DOUBLE PRECISION,
--     byteArrayMan BYTEA,
--     customMan    VARCHAR(255),
--     stringOpt    VARCHAR(255),
--     booleanOpt   BOOLEAN,
--     byteOpt      SMALLINT,
--     shortOpt     SMALLINT,
--     intOpt       INTEGER,
--     longOpt      BIGINT,
--     floatOpt     FLOAT,
--     doubleOpt    DOUBLE PRECISION,
--     byteArrayOpt BYTEA,
--     customOpt    VARCHAR(255)
-- );

CREATE TABLE gen_type_test.JsonbExample2
(
    id     SERIAL PRIMARY KEY,
    value1 JSONB,
    value2 JSONB
);
