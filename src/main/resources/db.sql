create table club
(
    club_id serial
        constraint club_pk
            primary key,
    name varchar(200) not null,
    city varchar(100) not null,
    email_address varchar(100),
    phone char(10),
    is_notified char(1) not null,
    year_of_foundation int
);

create table account
(
    account_id serial,
    balance float not null,
    last_statement_timestamp timestamp
);


create unique index account_account_id_uindex
    on account (account_id);

alter table account
    add constraint account_pk
        primary key (account_id);


create table club_account
(
    club_club_id int not null
        constraint club_account_pk
            primary key
        constraint club_account_club_club_id_fk
            references club,
    account_account_id int not null
        constraint club_account_account_account_id_fk
            references account
);

create table transaction
(
    transaction_id serial,
    account_id int not null
        constraint transaction_pk
            primary key
        constraint transaction_account_account_id_fk
            references account,
    credit float,
    debit float,
    creation_timestamp timestamp not null
);

create unique index transaction_transaction_id_uindex
    on transaction (transaction_id);


INSERT INTO club(name, address, email_address, phone, is_notified, year_of_foundation)
VALUES
    ('Manchester City', '83 Ducie St, Manchester M1 2JQ', 'manchester_city@gmail.com', '855100432', 'M', 1922),
    ('Paris Saint Germain', '24, Rue du Commandant-Guilbaud 75016 Paris', 'psg@gmail.com', '155101132', 'M', 1924),
    ('Real Madrid', 'Avenida de Concha Espina 1 Madrid', 'real@yahoo.com', '840177901', 'P', 1899),
    ('Legia Warszawa', 'Łazienkowska 3, 00-449 Warszawa', 'legia@interia.pl', '589101655', 'M', 1924),
    ('Czarni Deblin', 'Pułku Piechoty, 08-530 Dęblin', 'czarni@wp.pl', '732011854', 'P', 1931);

INSERT INTO account(balance, last_statement_timestamp)
VALUES
    (100000000, '2021-06-22 19:10:25-01'),
    (250000000, '2021-10-01 06:50:42-02'),
    (110000000, '2020-02-10 11:30:14-07'),
    (250000000, '2021-10-01 12:20:11-06'),
    (1950000, '2016-11-15 12:41:11-02'),
    (999000, '2019-08-22 14:41:21-06'),
    (50000, '2018-04-04 21:41:25-01');

INSERT INTO club_account(club_club_id, account_account_id)
VALUES
    (1, 1),
    (2, 2),
    (3, 4),
    (1, 3),
    (4, 5),
    (4, 6),
    (5, 7),
    (4, 7);