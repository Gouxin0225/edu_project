ALTER TABLE `sys_user`
  ADD COLUMN IF NOT EXISTS `must_change_password` TINYINT DEFAULT 0
  COMMENT '是否必须修改密码: 1-必须修改, 0-不需要'
  AFTER `created_by`;

UPDATE `sys_user`
SET `must_change_password` = 0
WHERE `must_change_password` IS NULL;
