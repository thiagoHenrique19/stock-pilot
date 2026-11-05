CREATE SEQUENCE sale_receipt_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE sale
    ADD COLUMN status          VARCHAR(255)   NOT NULL DEFAULT 'COMPLETED',
    ADD COLUMN receipt_number  VARCHAR(255),
    ADD COLUMN amount_received NUMERIC(19, 2),
    ADD COLUMN change_amount   NUMERIC(19, 2),
    ADD COLUMN cancelled_at    TIMESTAMP(6) WITH TIME ZONE,
    ADD COLUMN version         BIGINT         NOT NULL DEFAULT 0;

UPDATE sale SET receipt_number = 'SALE-' || lpad(id::text, 6, '0');

ALTER TABLE sale
    ALTER COLUMN receipt_number SET NOT NULL,
    ADD CONSTRAINT uk_sale_receipt_number UNIQUE (receipt_number);

SELECT setval('sale_receipt_seq', COALESCE((SELECT max(id) FROM sale), 0) + 1, false);

CREATE INDEX idx_sale_status ON sale (status);
CREATE INDEX idx_sale_payment_method ON sale (payment_method);
