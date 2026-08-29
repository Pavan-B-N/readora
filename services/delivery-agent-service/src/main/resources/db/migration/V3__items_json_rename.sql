-- Was a human-readable "Title ×qty, Title ×qty" display string; now holds a JSON array of
-- {"title":"...","qty":N} objects instead, so the frontend can render a real itemized list
-- rather than parsing a comma-joined string. Renamed to match what it actually holds.
ALTER TABLE delivery.delivery_assignments RENAME COLUMN items_summary TO items_json;
ALTER TABLE delivery.return_pickup_assignments RENAME COLUMN items_summary TO items_json;
