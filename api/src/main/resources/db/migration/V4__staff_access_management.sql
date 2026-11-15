-- Beneficiaries do not access the platform, so the role is no longer accepted.
ALTER TABLE staff_members DROP CONSTRAINT ck_staff_members_role;
ALTER TABLE staff_members
    ADD CONSTRAINT ck_staff_members_role CHECK (role IN ('ROLE_ADMIN', 'ROLE_COORDINATOR', 'ROLE_VOLUNTEER'));

ALTER TABLE staff_members DROP CONSTRAINT ck_staff_members_status;
UPDATE staff_members SET status = 'BLOCKED' WHERE status = 'INACTIVE';
ALTER TABLE staff_members
    ADD CONSTRAINT ck_staff_members_status CHECK (status IN ('ACTIVE', 'BLOCKED'));

ALTER TABLE staff_members ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT false;
