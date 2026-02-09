CREATE TABLE transactions (
                              id BIGSERIAL PRIMARY KEY,
                              user_id UUID NOT NULL,
                              instrument_id BIGINT NOT NULL,
                              type VARCHAR(10) NOT NULL,
                              price NUMERIC(19,6) NOT NULL,
                              quantity NUMERIC(19,6) NOT NULL,
                              total_amount NUMERIC(19,6) NOT NULL,
                              created_at TIMESTAMP NOT NULL,

                              CONSTRAINT fk_tx_user FOREIGN KEY (user_id) REFERENCES users(id),
                              CONSTRAINT fk_tx_instrument FOREIGN KEY (instrument_id) REFERENCES instruments(id)
);