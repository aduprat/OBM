CREATE INDEX opush_synced_item_sync_state_id_fkey ON opush_synced_item(sync_state_id);

CREATE INDEX opush_sync_state_device_id_collection_id_fkey ON opush_sync_state(device_id, collection_id);