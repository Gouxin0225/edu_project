-- Create or repair a deterministic assistant account for smoke tests.
-- Login: assistant001 / password123
INSERT INTO `sys_user` (
  `username`,
  `password`,
  `real_name`,
  `role`,
  `must_change_password`,
  `is_deleted`
) VALUES (
  'assistant001',
  '$2a$12$ayQyZ1wMkQW1pc.VhndwMeHrD.sSANOBCCwhenTexwi1JRT2QGslC',
  '测试班主任',
  'ASSISTANT',
  0,
  0
) ON DUPLICATE KEY UPDATE
  `password` = VALUES(`password`),
  `real_name` = VALUES(`real_name`),
  `role` = VALUES(`role`),
  `must_change_password` = VALUES(`must_change_password`),
  `is_deleted` = VALUES(`is_deleted`);
