# Installation 


## 1. Installation without OCP user workload monitoring

These steps explain how to setup the load test environment for OCP without user workload monitoring enabled, i.e. Prometheus is not provided by OpenShift and will be installed as part of this project.

The steps will produce the following resources in OpenShift:

- Your own loadtest project
- RHSSO 
- Prometheus and Grafana with RHSSO loadtest dashboard
- BuildConfig, Pod and Route for Fortio loadtest server 



1. Create a new OpenShift project: `oc new-project $OCP_PROJECT_NAME`
1. Install RHSSO, e.g. via service catalog or operator
1. Install the Prometheus metrics spi and prepare the service (name port to web to be scraped by service monitor): `sh install-metrics-spi.sh`
1. Create realm called loadtest
1. In Events -> Config add metrics-listener to Event Listeners and turn on login events.
1. Create first user with credentials $USER and assign admin right in realm loadtest
1. Create the OpenShift templates: oc create -f build.yaml,loadtest.yaml,monitoring.yaml
1. Create a Java keystore with password $TRUSTSTORE_PASS for trusted certificates (router certificate for sso): `keytool -keystore truststore.jks -importcert -file <certfile> -alias <alias> -trustcacerts`
1. Create configmap with truststore: `sh install-truststore.sh`
1. Create the build configs: `oc process sso-loadtest-builds -p REST_CLIENT_BASE_URL=$SSO_URI -p CLIENT_ADAPTER_BINDING_URL=$SSO_URI -p REDIRECT_URI=$LOAD_TEST_SP_URI -p TRUSTSTORE_PASSWORD=$TRUSTSTORE_PASS| oc create -f -`
1. Install the Prometheus and Grafana operators to your project
1. Create the monitoring components: `oc process sso-loadtest-monitoring -p RHSSO_NAMESPACE=$OCP_PROJECT_NAME | oc create -f -`
1. Create the SSO Service Provider components: `oc process sso-loadtest-sp -p IMAGE_SOURCE_NAMESPACE=$OCP_PROJECT_NAME | oc create -f -`
1. Create initial access token $TOKEN1
1. Create saml client via GET request to api/saml/test/register/client/$TOKEN1
1. Sometimes Client Signature required is switched on. Please switch off via admin ui.
1. Create initial access token $TOKEN2
1. Create oidc client via GET request to api/oidc/test/register/client/$TOKEN2
1. Send GET request to load users api/testdata/users/$NUM_USERS to create $NUM_USERS test users
1. Send GET request to load users api/testdata/loadusers
1. Send GET request to /api/saml/test/request to send a SAML request 
1. Check in SSO Admin UI Events whether login event occurred
1. Send Fortio request via route /fortio/?labels=Fortio&url=http%3A%2F%2Fsso-loadtest-sp%3A8080%2Fapi%2Fsaml%2Ftest%2Frequest%3Fdelay%3D250us%3A30%2C5ms%3A5%26status%3D503%3A0.5%2C429%3A1.5&qps=4&t=3s&n=&c=8&p=50%2C+75%2C+90%2C+99%2C+99.9&r=0.0001&H=User-Agent%3A+fortio.org%2Ffortio-1.3.1-pre&H=&H=&H=&runner=http&grpc-ping-delay=0&save=on&load=Start 

## 2. Installation with OCP user workload monitoring

The steps will produce the following resources in OpenShift:

- Your own loadtest project
- RHSSO 
- Grafana whith RHSSO loadtest dashboard
- BuildConfig, Pod and Route for Fortio loadtest server 



1. Create a new OpenShift project: `oc new-project $OCP_PROJECT_NAME`
1. Install RHSSO, e.g. via service catalog or operator
1. Install the Prometheus metrics spi and prepare the service (name port to web to be scraped by service monitor): `sh install-metrics-spi.sh`
1. Create realm called loadtest
1. In Events -> Config add metrics-listener to Event Listeners and turn on login events.
1. Create first user with credentials $USER and assign admin right in realm loadtest
1. Create the OpenShift templates: oc create -f build.yaml,loadtest.yaml,grafana-sa.yaml,grafana-ocp-userworkload.yaml
1. Create a Java keystore with password $TRUSTSTORE_PASS for trusted certificates (router certificate for sso): `keytool -keystore truststore.jks -importcert -file <certfile> -alias <alias> -trustcacerts`
1. Create configmap with truststore: `sh install-truststore.sh`
1. Create the build configs: `oc process sso-loadtest-builds -p REST_CLIENT_BASE_URL=$SSO_URI -p CLIENT_ADAPTER_BINDING_URL=$SSO_URI -p REDIRECT_URI=$LOAD_TEST_SP_URI -p TRUSTSTORE_PASSWORD=$TRUSTSTORE_PASS| oc create -f -`
1. Install the Grafana operator to your project
1. Create the Grafana Service Account: `oc create -f -n $OCP_PROJECT_NAME grafana-serviceaccount`
1. Copy the SA token and paste for the GRAFANA_SA_TOKEN parameter and run `oc process sso-loadtest-grafana -p RHSSO_NAMESPACE=$OCP_PROJECT_NAME | oc create -f -`
1. Create the SSO Service Provider components: `oc process sso-loadtest-sp -p IMAGE_SOURCE_NAMESPACE=$OCP_PROJECT_NAME | oc create -f -`
1. Create initial access token $TOKEN1
1. Create saml client via GET request to api/saml/test/register/client/$TOKEN1
1. Sometimes Client Signature required is switched on. Please switch off via admin ui.
1. Create initial access token $TOKEN2
1. Create oidc client via GET request to api/oidc/test/register/client/$TOKEN2
1. Send GET request to load users api/testdata/users/$NUM_USERS to create $NUM_USERS test users
1. Send GET request to load users api/testdata/loadusers
1. Send GET request to /api/saml/test/request to send a SAML request 
1. Check in SSO Admin UI Events whether login event occurred
1. Send Fortio request via route /fortio/?labels=Fortio&url=http%3A%2F%2Fsso-loadtest-sp%3A8080%2Fapi%2Fsaml%2Ftest%2Frequest%3Fdelay%3D250us%3A30%2C5ms%3A5%26status%3D503%3A0.5%2C429%3A1.5&qps=4&t=3s&n=&c=8&p=50%2C+75%2C+90%2C+99%2C+99.9&r=0.0001&H=User-Agent%3A+fortio.org%2Ffortio-1.3.1-pre&H=&H=&H=&runner=http&grpc-ping-delay=0&save=on&load=Start 


## 3. Installation with Fortio operator and user workload monitoring

1. Install the fortio operator `kubectl create -n $OCP_PROJECT_NAME -f https://raw.githubusercontent.com/verfio/fortio-operator/master/deploy/fortio.yaml`
1. Because config map creation will fail due to namespaced installation create an empty configmap named fortio-data-dir.
1. (Optional) Create the fortio oc create -f fortio-operator.yaml
1. (Optional) Create the fortio server component to be able to look into the fortio reports: `oc process fortio-operator-components | oc create -f -` 
1. Create a new OpenShift project: `oc new-project $OCP_PROJECT_NAME`
1. Install RHSSO, e.g. via service catalog or operator
1. Install the Prometheus metrics spi and prepare the service (name port to web to be scraped by service monitor): `sh install-metrics-spi.sh`
1. Create realm called loadtest
1. In Events -> Config add metrics-listener to Event Listeners and turn on login events.
1. Create first user with credentials $USER and assign admin right in realm loadtest
1. Create the OpenShift templates: oc create -f build.yaml,loadtest.yaml,grafana-sa.yaml,grafana-ocp-userworkload.yaml
1. Create a Java keystore with password $TRUSTSTORE_PASS for trusted certificates (router certificate for sso): `keytool -keystore truststore.jks -importcert -file <certfile> -alias <alias> -trustcacerts`
1. Create configmap with truststore: `sh install-truststore.sh`
1. Create the build configs: `oc process sso-loadtest-builds -p REST_CLIENT_BASE_URL=$SSO_URI -p CLIENT_ADAPTER_BINDING_URL=$SSO_URI -p REDIRECT_URI=$LOAD_TEST_SP_URI -p TRUSTSTORE_PASSWORD=$TRUSTSTORE_PASS| oc create -f -`
1. Install the Grafana operator to your project
1. Create the Grafana Service Account: `oc create -f -n $OCP_PROJECT_NAME grafana-serviceaccount`
1. Copy the SA token and paste for the GRAFANA_SA_TOKEN parameter and run `oc process sso-loadtest-grafana -p RHSSO_NAMESPACE=$OCP_PROJECT_NAME | oc create -f -`
1. Create the SSO Service Provider components: `oc process sso-loadtest-sp -p IMAGE_SOURCE_NAMESPACE=$OCP_PROJECT_NAME | oc create -f -`
1. Create initial access token $TOKEN1
1. Create saml client via GET request to api/saml/test/register/client/$TOKEN1
1. Sometimes Client Signature required is switched on. Please switch off via admin ui.
1. Create initial access token $TOKEN2
1. Create oidc client via GET request to api/oidc/test/register/client/$TOKEN2
1. Send GET request to load users api/testdata/users/$NUM_USERS to create $NUM_USERS test users
1. Send GET request to load users api/testdata/loadusers
1. Send GET request to /api/saml/test/request to send a SAML request 
1. Check in SSO Admin UI Events whether login event occurred
1. Create a new LoadTest CR for example: 

```yaml
apiVersion: fortio.verf.io/v1alpha1
kind: LoadTest
metadata:
  name: rhsso-loadtest
spec:
  duration: 60s
  url: "http://sso-loadtest-sp:8080/api/saml/test/request"
  qps: "50"
  action: load
```

