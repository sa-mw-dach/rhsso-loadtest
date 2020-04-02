# Red Hat SSO Load Test Environment
This project aims at providing a load testing environment for Red Hat SSO.

## Components
The test environment contains the following components:

- Keycloak Prometheus Metrics SPI configuration for your RHSSO instance
- Prometheus instance (deployed via operator) for scraping the metrics from the Metrics SPI endpoint
- Grafana instance (deployed via operator) with a preconfigured Prometheus data source and a RHSSO metrics dashboard
- Quarkus based REST service to simulate the Service Provider connected to RHSSO via SAML2 or OIDC
- Fortio for creating parallel requests to the Service Provider 

