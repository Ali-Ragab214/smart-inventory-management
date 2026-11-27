alter table products
    add column unit_of_measure varchar(20) not null default 'UNIT',
    add column active boolean not null default true;

-- idx_products_category_id and ux_products_reference_active already cover category/reference lookups.
create index idx_products_deleted_active on products (deleted, active);
