drop table if exists books;

create table books (
  id integer primary key,
  code varchar(50),
  description varchar(30)
);