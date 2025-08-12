CREATE TABLE "UserProfile" (
  "userId" INTEGER PRIMARY KEY,
  "firstName" VARCHAR(80) NOT NULL,
  last_name VARCHAR(80) NOT NULL,
  "emailAddress" VARCHAR(200) UNIQUE NOT NULL,
  "isActive" INTEGER NOT NULL DEFAULT 1,
  "createdAt" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE user_profile_details (
  user_profile_detail_id INTEGER PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES "UserProfile"("userId"),
  address_line1 VARCHAR(200),
  addressLine2 VARCHAR(200),
  city VARCHAR(100),
  state_code CHAR(2),
  postal_code VARCHAR(20),
  birth_date DATE,
  preferred_contact_time TIME,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "userLogin_history" (
  "loginId" INTEGER PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES "UserProfile"("userId"),
  login_ts TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ipAddress VARCHAR(45),
  user_agent VARCHAR(300),
  success_flag INTEGER NOT NULL
);

CREATE TABLE "UserEmailPrefs" (
  "prefId" INTEGER PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES "UserProfile"("userId"),
  newsletter_optIn INTEGER NOT NULL DEFAULT 0,
  promo_opt_out INTEGER NOT NULL DEFAULT 0,
  last_changed TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_password_reset_tokens (
  token_id INTEGER PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES "UserProfile"("userId") ON DELETE CASCADE,
  reset_token VARCHAR(120) NOT NULL,
  requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  used_at TIMESTAMP
);

CREATE TABLE "OrganizationAccounts" (
  "orgId" INTEGER PRIMARY KEY,
  org_name VARCHAR(160) NOT NULL,
  legal_name VARCHAR(200),
  created_on DATE NOT NULL,
  active_flag INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE org_account_members (
  org_member_id INTEGER PRIMARY KEY,
  org_id INTEGER NOT NULL REFERENCES "OrganizationAccounts"("orgId") ON DELETE CASCADE,
  user_id INTEGER NOT NULL REFERENCES "UserProfile"("userId") ON DELETE CASCADE,
  role_name VARCHAR(60) NOT NULL,
  joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "TeamMembers" (
  "teamId" INTEGER NOT NULL,
  "memberId" INTEGER NOT NULL,
  team_role VARCHAR(60),
  added_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("teamId", "memberId")
);
