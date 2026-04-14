drop table if exists promotions cascade;
drop table if exists owner_restaurant cascade;
drop table if exists owner_account cascade;
drop table if exists admin cascade;
drop table if exists restaurant cascade;
drop table if exists restaurantes cascade;

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
