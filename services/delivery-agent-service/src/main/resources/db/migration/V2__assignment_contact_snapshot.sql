ALTER TABLE delivery.delivery_assignments
    ADD COLUMN recipient_name character varying(255),
    ADD COLUMN recipient_phone character varying(255),
    ADD COLUMN items_summary text;

ALTER TABLE delivery.return_pickup_assignments
    ADD COLUMN recipient_name character varying(255),
    ADD COLUMN recipient_phone character varying(255),
    ADD COLUMN items_summary text;
