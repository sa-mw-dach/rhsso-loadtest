#!/bin/bash
curl -L https://github.com/aerogear/keycloak-metrics-spi/releases/download/1.0.4/keycloak-metrics-spi-1.0.4.jar --output keycloak-metrics-spi-1.0.4.jar
oc create configmap keycloak-metrics-spi --from-file keycloak-metrics-spi-1.0.4.jar
oc patch dc sso --type=json --patch='[{"op": "add", "path": "/spec/template/spec/containers/0/volumeMounts/-", "value": {"name": "sso-providers-volume", "mountPath": "/opt/eap/providers"}}, {"op": "add", "path": "/spec/template/spec/volumes/-", "value": {"name": "sso-providers-volume", "configMap": {"name": "keycloak-metrics-spi"}}}]'
oc patch svc sso --type=json --patch='[{"op": "add", "path": "/spec/ports/0/name", "value":"web"}]'
