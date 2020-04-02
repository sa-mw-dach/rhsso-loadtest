# Installation 

1. Create a new OpenShift project: `oc new-project $OCP_PROJECT_NAME`
1. Install RHSSO, e.g. via service catalog or operator
1. Install the Prometheus metrics spi and prepare the service (name port to web to be scraped by service monitor): `sh install_metrics_spi.sh`
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
