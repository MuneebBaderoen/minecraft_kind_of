web: bin/minecraft $PORT
sync: while true; do sleep ${AWS_SYNC_INTERVAL:-60}; bin/sync $DATABASE_URL; done