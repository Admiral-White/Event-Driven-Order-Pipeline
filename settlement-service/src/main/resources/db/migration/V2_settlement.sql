-- Create settlement_records table
CREATE TABLE IF NOT EXISTS settlement_records (
    id BIGSERIAL PRIMARY KEY,
    settlement_id VARCHAR(100) NOT NULL UNIQUE,
    order_id VARCHAR(100) NOT NULL UNIQUE,
    order_number VARCHAR(50) NOT NULL,
    customer_id VARCHAR(100) NOT NULL,
    settlement_amount NUMERIC(19, 2) NOT NULL,
    fee_amount NUMERIC(19, 2) NOT NULL,
    net_amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50) NOT NULL,
    merchant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(500)
);

-- Create settlement_audit_trail table for tracking changes
CREATE TABLE IF NOT EXISTS settlement_audit_trail (
    id BIGSERIAL PRIMARY KEY,
    settlement_id VARCHAR(100) NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by VARCHAR(100),
    change_reason VARCHAR(500),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_settlement_status ON settlement_records(status);
CREATE INDEX IF NOT EXISTS idx_settlement_order_id ON settlement_records(order_id);
CREATE INDEX IF NOT EXISTS idx_settlement_customer_id ON settlement_records(customer_id);
CREATE INDEX IF NOT EXISTS idx_settlement_merchant_id ON settlement_records(merchant_id);
CREATE INDEX IF NOT EXISTS idx_settlement_created_at ON settlement_records(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_trail_settlement_id ON settlement_audit_trail(settlement_id);
