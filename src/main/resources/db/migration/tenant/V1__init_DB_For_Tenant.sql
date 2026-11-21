create table categories
(
    id          varchar(255) not null
        primary key,
    created_at  timestamp(6) not null,
    created_by  varchar(255) not null,
    deleted     boolean      not null,
    updated_at  timestamp(6),
    updated_by  varchar(255),
    description text,
    name        varchar(255) not null
);

create unique index ux_categories_name_active
    on categories (lower(name))
    where deleted = false;
create index idx_categories_deleted on categories (deleted);

create table products
(
    id              varchar(255)   not null
        primary key,
    created_at      timestamp(6)   not null,
    created_by      varchar(255)   not null,
    deleted         boolean        not null,
    updated_at      timestamp(6),
    updated_by      varchar(255),
    alert_threshold integer        not null,
    description     text,
    name            varchar(255)   not null,
    price           numeric(38, 2) not null,
    reference       varchar(255)   not null,
    category_id     varchar(255)
        constraint fk_category_id
            references categories (id) on delete set null
);

create unique index ux_products_reference_active
    on products (reference)
    where deleted = false;
create index idx_products_category_id on products (category_id);
create index idx_products_deleted on products (deleted);

create table stock_mvts
(
    id         varchar(255) not null
        primary key,
    created_at timestamp(6) not null,
    created_by varchar(255) not null,
    deleted    boolean      not null,
    updated_at timestamp(6),
    updated_by varchar(255),
    comment    text,
    date_mvt   date         not null,
    quantity   integer      not null,
    type_mvt   varchar(255) not null
        constraint stock_mvts_type_mvt_check
            check (type_mvt in ('IN', 'OUT')),
    product_id varchar(255)
        constraint fk_product_id
            references products (id) on delete cascade
);

create index idx_stock_mvts_product_id on stock_mvts (product_id);
create index idx_stock_mvts_deleted on stock_mvts (deleted);
