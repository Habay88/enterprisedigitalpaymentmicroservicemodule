-- Insert default GL accounts for the chart of accounts
INSERT INTO gl_accounts (id, account_code, account_name, account_type, normal_balance, tenant_id, active, balance)
VALUES 
    -- ASSET ACCOUNTS
    (gen_random_uuid(), '1000', 'Cash - NGN', 'ASSET', 'DEBIT', 'DEFAULT_TENANT', true, 0),
    (gen_random_uuid(), '1100', 'Customer Asset Account', 'ASSET', 'DEBIT', 'DEFAULT_TENANT', true, 0),
    (gen_random_uuid(), '1200', 'Merchant Asset Account', 'ASSET', 'DEBIT', 'DEFAULT_TENANT', true, 0),
    (gen_random_uuid(), '1300', 'Settlement Account', 'ASSET', 'DEBIT', 'DEFAULT_TENANT', true, 0),
    
    -- LIABILITY ACCOUNTS
    (gen_random_uuid(), '2000', 'Customer Deposits', 'LIABILITY', 'CREDIT', 'DEFAULT_TENANT', true, 0),
    (gen_random_uuid(), '2100', 'Merchant Payables', 'LIABILITY', 'CREDIT', 'DEFAULT_TENANT', true, 0),
    (gen_random_uuid(), '2200', 'Settlement Payables', 'LIABILITY', 'CREDIT', 'DEFAULT_TENANT', true, 0),
    
    -- EQUITY ACCOUNTS
    (gen_random_uuid(), '3000', 'Retained Earnings', 'EQUITY', 'CREDIT', 'DEFAULT_TENANT', true, 0),
    (gen_random_uuid(), '3100', 'Share Capital', 'EQUITY', 'CREDIT', 'DEFAULT_TENANT', true, 0),
    
    -- REVENUE ACCOUNTS
    (gen_random_uuid(), '4000', 'Transaction Fees', 'REVENUE', 'CREDIT', 'DEFAULT_TENANT', true, 0),
    (gen_random_uuid(), '4100', 'Interest Income', 'REVENUE', 'CREDIT', 'DEFAULT_TENANT', true, 0),
    
    -- EXPENSE ACCOUNTS
    (gen_random_uuid(), '5000', 'Processing Fees', 'EXPENSE', 'DEBIT', 'DEFAULT_TENANT', true, 0),
    (gen_random_uuid(), '5100', 'Operating Expenses', 'EXPENSE', 'DEBIT', 'DEFAULT_TENANT', true, 0);