-- Payout used to be a flat mock rate computed on read; now it's snapshotted per-assignment at
-- creation time (scaled by order value and item count), so historical earnings stay accurate
-- even if the payout formula changes later.
ALTER TABLE delivery.delivery_assignments ADD COLUMN payout_amount numeric(10,2);
ALTER TABLE delivery.return_pickup_assignments ADD COLUMN payout_amount numeric(10,2);
