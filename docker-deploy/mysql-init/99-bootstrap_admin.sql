-- Initial admin account for a clean database.
-- Login: admin / admin123
-- Change this password immediately after first login.
INSERT INTO `sys_user` (
  `username`,
  `password`,
  `real_name`,
  `role`,
  `must_change_password`,
  `is_deleted`
) VALUES (
  'admin',
  '$2b$12$FIUbexgFpMzUPagXehCqRutl0qx4wEatNHTU1YFDTtjYtLu.x5LSq',
  '系统管理员',
  'ADMIN',
  0,
  0
) ON DUPLICATE KEY UPDATE
  `password` = VALUES(`password`),
  `real_name` = VALUES(`real_name`),
  `role` = VALUES(`role`),
  `must_change_password` = VALUES(`must_change_password`),
  `is_deleted` = VALUES(`is_deleted`);
