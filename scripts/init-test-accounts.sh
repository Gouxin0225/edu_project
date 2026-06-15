#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_USER="${DB_USER:-edu_app}"
DB_PASSWORD="${DB_PASSWORD:-openlab123}"
DB_NAME="${DB_NAME:-edu_db}"
TEST_CLASS_ID="${TEST_CLASS_ID:-}"

TEST_ADMIN_USERNAME="${TEST_ADMIN_USERNAME:-test_admin}"
TEST_TEACHER_USERNAME="${TEST_TEACHER_USERNAME:-test_teacher}"
TEST_ASSISTANT_USERNAME="${TEST_ASSISTANT_USERNAME:-test_assistant}"
TEST_STUDENT_USERNAME="${TEST_STUDENT_USERNAME:-test_student}"

# BCrypt hash for openlab@123. Override TEST_PASSWORD_HASH to use a different stable test password.
TEST_PASSWORD_HASH="${TEST_PASSWORD_HASH:-\$2a\$10\$3bcPcH93ktwtniEfQh1G7Oe9YZ0aNYIFq3S2RyJT8SzUpcA4z1oGm}"

MYSQL=(mysql -h"$DB_HOST" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME")

if [ -z "$TEST_CLASS_ID" ]; then
  TEST_CLASS_ID="$("${MYSQL[@]}" -Nse "select id from sys_class where is_deleted=0 order by id limit 1")"
fi

if [ -z "$TEST_CLASS_ID" ]; then
  echo "No active class found. Set TEST_CLASS_ID or create a class first." >&2
  exit 2
fi

"${MYSQL[@]}" <<SQL
START TRANSACTION;

INSERT INTO sys_user (username, password, real_name, role, class_id, created_by, must_change_password, is_deleted, create_source)
VALUES
('${TEST_ADMIN_USERNAME}', '${TEST_PASSWORD_HASH}', 'Regression Admin', 'ADMIN', NULL, 1, 0, 0, 'TEST_INIT'),
('${TEST_TEACHER_USERNAME}', '${TEST_PASSWORD_HASH}', 'Regression Teacher', 'TEACHER', NULL, 1, 0, 0, 'TEST_INIT'),
('${TEST_ASSISTANT_USERNAME}', '${TEST_PASSWORD_HASH}', 'Regression Assistant', 'ASSISTANT', NULL, 1, 0, 0, 'TEST_INIT'),
('${TEST_STUDENT_USERNAME}', '${TEST_PASSWORD_HASH}', 'Regression Student', 'STUDENT', ${TEST_CLASS_ID}, 1, 0, 0, 'TEST_INIT')
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  real_name = VALUES(real_name),
  role = VALUES(role),
  class_id = VALUES(class_id),
  must_change_password = 0,
  is_deleted = 0,
  create_source = 'TEST_INIT';

SET @teacher_id = (SELECT id FROM sys_user WHERE username = '${TEST_TEACHER_USERNAME}' LIMIT 1);
SET @assistant_id = (SELECT id FROM sys_user WHERE username = '${TEST_ASSISTANT_USERNAME}' LIMIT 1);
SET @student_id = (SELECT id FROM sys_user WHERE username = '${TEST_STUDENT_USERNAME}' LIMIT 1);

INSERT INTO teacher_class_rel (teacher_id, class_id)
SELECT @teacher_id, ${TEST_CLASS_ID}
WHERE NOT EXISTS (
  SELECT 1 FROM teacher_class_rel WHERE teacher_id = @teacher_id AND class_id = ${TEST_CLASS_ID}
);

INSERT INTO teacher_class_rel (teacher_id, class_id)
SELECT @assistant_id, ${TEST_CLASS_ID}
WHERE NOT EXISTS (
  SELECT 1 FROM teacher_class_rel WHERE teacher_id = @assistant_id AND class_id = ${TEST_CLASS_ID}
);

INSERT INTO class_student_rel (class_id, student_id, status, join_source, created_by)
SELECT ${TEST_CLASS_ID}, @student_id, 'ACTIVE', 'TEST_INIT', 1
WHERE NOT EXISTS (
  SELECT 1 FROM class_student_rel WHERE class_id = ${TEST_CLASS_ID} AND student_id = @student_id AND status = 'ACTIVE'
);

COMMIT;
SQL

echo "Initialized test accounts on class ${TEST_CLASS_ID}."
echo "Users: ${TEST_ADMIN_USERNAME}, ${TEST_TEACHER_USERNAME}, ${TEST_ASSISTANT_USERNAME}, ${TEST_STUDENT_USERNAME}"
echo "Default password: openlab@123"
