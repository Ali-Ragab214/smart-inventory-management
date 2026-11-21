create table tenants
(
    deleted         boolean      not null,
    created_at      timestamp(6) not null,
    updated_at      timestamp(6),
    updated_by      varchar(255),
    admin_email     varchar(255) not null,
    admin_full_name varchar(255) not null,
    admin_password  varchar(255) not null,
    admin_username  varchar(255) not null,
    company_code    varchar(255) not null,
    company_name    varchar(255) not null,
    email           varchar(255) not null,
    id              varchar(255) not null
        primary key,
    status          varchar(255) not null
        constraint tenants_status_check
            check (status in ('PENDING', 'ACTIVE', 'SUSPENDED', 'INACTIVE'))
);

create unique index ux_tenants_company_code on tenants (company_code);
create unique index ux_tenants_company_code_lower on tenants (lower(company_code));
create unique index ux_tenants_admin_email on tenants (admin_email) where deleted = false;
create unique index ux_tenants_admin_username on tenants (admin_username) where deleted = false;
create unique index ux_tenants_email on tenants (email) where deleted = false;
create index idx_tenants_deleted on tenants (deleted);

create table users
(
    deleted    boolean      not null,
    enabled    boolean      not null default true,
    created_at timestamp(6) not null,
    updated_at timestamp(6),
    created_by varchar(255) not null,
    email      varchar(255) not null,
    first_name varchar(255) not null,
    id         varchar(255) not null
        primary key,
    last_name  varchar(255) not null,
    password   varchar(255) not null,
    role       varchar(255) not null
        constraint users_role_check
            check (role in ('ROLE_PLATFORM_ADMIN', 'ROLE_COMPANY_ADMIN', 'ROLE_ADMINISTRATOR', 'ROLE_USER', 'ROLE_SALES_OPERATOR')),
    tenant_id  varchar(255)
        constraint fk_user_tenant_id
            references tenants (id) on delete cascade,
    updated_by varchar(255),
    username   varchar(255) not null
);

create unique index ux_users_username_active on users (username) where deleted = false;
create unique index ux_users_email_active on users (email) where deleted = false;
create index idx_users_tenant_id on users (tenant_id);
create index idx_users_deleted on users (deleted);
