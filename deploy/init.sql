账号密码

所有表名小写，并且多个单词使用下划线隔开，全部用tb开头

create table tb_sys_user (
    user_id      bigint unsigned not null comment'用户id（主键）',
    user_account varchar(20) not null  comment '账号',
    password     varchar(20) not null  comment '密码',
    create_by    bigint unsigned not null comment '创建人',
    create_time  datetime   not null   comment '创建时间',
    update_by    bigint unsigned       comment '更新人',
    update_time  datetime              comment '更新时间',
    primary key ('user_id'),
    unique key 'idx_user_account' ('user_account')
)

字符串：char varchar 区别：
    char 定长 char(10)
    varchar 动态开辟空间 varchar(10)