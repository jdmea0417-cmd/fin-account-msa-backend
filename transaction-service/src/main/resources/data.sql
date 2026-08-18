insert into transaction (from_account_id, to_account_id, amount, type, status, created_at)
values (null, 1, 1000, 'DEPOSIT', 'SUCCESS', current_timestamp);

insert into transaction (from_account_id, to_account_id, amount, type, status, created_at)
values (2, null, 1000, 'WITHDRAW', 'SUCCESS', current_timestamp);

insert into transaction (from_account_id, to_account_id, amount, type, status, created_at)
values (1, 2, 1000, 'TRANSFER', 'SUCCESS', current_timestamp);