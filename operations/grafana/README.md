# Grafana Dashboard Assets

Last updated: 2026-07-25

## Dashboards

- dashboards/service-overview.json
- dashboards/kafka-db-overview.json

## Import Steps

1. Open Grafana.
2. Dashboards -> Import.
3. Upload JSON files from this folder.
4. Bind Prometheus datasource.

## Coverage

- service request rate and errors
- CPU and heap health
- Kafka lag
- DB connection saturation
