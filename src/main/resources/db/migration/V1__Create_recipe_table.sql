create table if not exists recipes (
    id uuid not null primary key default uuid_generate_v4(),
    name varchar(250) not null unique
);