CREATE TABLE IF NOT EXISTS `operation_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '审计日志ID',
  `operator_id` BIGINT NULL COMMENT '操作人ID',
  `operator_username` VARCHAR(64) NULL COMMENT '操作人用户名',
  `operator_real_name` VARCHAR(100) NULL COMMENT '操作人姓名',
  `operator_role` VARCHAR(32) NULL COMMENT '操作人角色',
  `action` VARCHAR(64) NOT NULL COMMENT '操作类型',
  `target_type` VARCHAR(64) NOT NULL COMMENT '对象类型',
  `target_id` BIGINT NULL COMMENT '对象ID',
  `detail` VARCHAR(1000) NULL COMMENT '操作详情',
  `client_ip` VARCHAR(64) NULL COMMENT '客户端IP',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_audit_create_time` (`create_time`),
  KEY `idx_audit_action` (`action`),
  KEY `idx_audit_operator` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';
