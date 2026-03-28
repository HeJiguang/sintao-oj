-- Follow-up fix after runtime smoke:
-- pure email login needs a larger email column and phone must no longer be required.

USE bitoj_dev;

ALTER TABLE tb_user
    MODIFY COLUMN phone varchar(20) NULL COMMENT 'optional phone after the email-login pivot',
    MODIFY COLUMN email varchar(100) NULL COMMENT 'email used by email-code login';
