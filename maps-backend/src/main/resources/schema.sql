drop table if exists promotions cascade;
drop table if exists coupons cascade;
drop table if exists owner_restaurant cascade;
drop table if exists owner_account cascade;
drop table if exists admin cascade;
drop table if exists restaurant cascade;
drop table if exists restaurantes cascade;
drop table if exists complaint cascade;
drop table if exists points_history cascade;
drop table if exists loyalty_accounts cascade;
drop table if exists client cascade;

create table restaurant (
    uuid uuid primary key,
    name varchar(120) not null,
    latitude double precision not null,
    longitude double precision not null,
    plan_suscription varchar(60),
    plan_expiration_date date,
    is_blocked boolean not null default false,
    description varchar(500),
    image_url varchar(500),
    category varchar(80)
);

create table admin (
    uuid uuid primary key,
    mail varchar(150) not null unique,
    password varchar(255) not null,
    is_deleted boolean not null default false,
    deleted_at timestamp
);

create table owner_account (
    uuid uuid primary key,
    mail varchar(150) not null unique,
    password varchar(255) not null
);

create table owner_restaurant (
    id_owner uuid not null,
    id_restaurant uuid not null,
    primary key (id_owner, id_restaurant),
    constraint fk_owner_restaurant_owner
        foreign key (id_owner)
            references owner_account (uuid)
            on delete cascade,
    constraint fk_owner_restaurant_restaurant
        foreign key (id_restaurant)
            references restaurant (uuid)
            on delete cascade
);

create table promotions (
    uuid uuid primary key,
    id_restaurant uuid not null,
    title varchar(120) not null,
    description varchar(500),
    percent_discount numeric(5, 2) not null,
    date_start_promotion date not null,
    date_end_promotion date not null,
    is_active_promotion boolean not null default true,
    constraint fk_promotions_restaurant
        foreign key (id_restaurant)
            references restaurant (uuid)
            on delete cascade,
    constraint chk_promotions_percent
        check (percent_discount >= 0 and percent_discount <= 100),
    constraint chk_promotions_dates
        check (date_end_promotion >= date_start_promotion)
);

create table client (
    uuid uuid primary key,
    mail varchar(150) not null unique,
    full_name varchar(150) not null,
    phone varchar(20)
);

create table coupons (
    uuid uuid primary key,
    restaurant_uuid uuid not null,
    client_uuid uuid,
    name varchar(120) not null,
    description varchar(500),
    start_date date not null,
    expiration_date date not null,
    max_quantity integer not null,
    discount_type varchar(50) not null,
    status varchar(50) not null default 'ACTIVE',
    qr_code varchar(500),
    created_at timestamp not null default current_timestamp,
    constraint fk_coupons_restaurant
        foreign key (restaurant_uuid)
            references restaurant (uuid)
            on delete cascade,
    constraint fk_coupons_client
        foreign key (client_uuid)
            references client (uuid)
            on delete set null,
    constraint chk_coupons_dates
        check (expiration_date >= start_date),
    constraint chk_coupons_quantity
        check (max_quantity > 0),
    constraint chk_coupons_status
        check (status in ('ACTIVE', 'PAUSED', 'EXPIRED', 'SOLD_OUT'))
);

create table loyalty_accounts (
    uuid uuid primary key,
    client_uuid uuid not null unique,
    accumulated_points integer not null default 0,
    current_level varchar(50) not null,
    constraint fk_loyalty_client
        foreign key (client_uuid)
            references client (uuid)
            on delete cascade,
    constraint chk_loyalty_points
        check (accumulated_points >= 0),
    constraint chk_loyalty_level
        check (current_level in ('BRONCE', 'PLATA', 'ORO'))
);

create table points_history (
    uuid uuid primary key,
    client_uuid uuid not null,
    points integer not null,
    reason varchar(150) not null,
    created_at timestamp not null default current_timestamp,
    constraint fk_points_history_client
        foreign key (client_uuid)
            references client (uuid)
            on delete cascade,
    constraint chk_points_history_points
        check (points > 0)
);

create table complaint (
    uuid uuid primary key,
    client_uuid uuid not null,
    type varchar(50) not null,
    target_uuid uuid not null,
    description varchar(1000) not null,
    status varchar(50) not null default 'PENDING',
    created_at timestamp not null default current_timestamp,
    constraint fk_complaint_client
        foreign key (client_uuid)
            references client (uuid)
            on delete cascade
);
