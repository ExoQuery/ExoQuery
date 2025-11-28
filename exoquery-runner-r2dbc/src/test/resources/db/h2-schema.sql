CREATE TABLE IF NOT EXISTS Person (
  id IDENTITY PRIMARY KEY,
  firstName VARCHAR(255),
  lastName VARCHAR(255),
  age INT
);

CREATE TABLE IF NOT EXISTS Address (
  ownerId INT,
  street  VARCHAR(255),
  zip     VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS Robot (
  ownerId INT,
  model   VARCHAR(255),
  age     INT
);
