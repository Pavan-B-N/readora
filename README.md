
Create the database:
```
psql -h localhost -p 5432 -U "$(whoami)" -d postgres -f dataset/create-database.sql -W
```

Seed data:
```
psql -h localhost -p 5432 -U readora -d readora -f dataset/seed.sql -W
```

Drop the database entirely:
```
psql -h localhost -p 5432 -U "$(whoami)" -d postgres -f dataset/drop-database.sql -W
```
