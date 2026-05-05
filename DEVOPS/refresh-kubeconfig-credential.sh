#!/bin/bash
# Refreshes the Jenkins 'kubeconfig' file credential with the current admin.conf
JENKINS="http://localhost:8086"
USER="aziz"
PASS="Joncina85738573"
COOKIE="/tmp/jcookie_kube.txt"
KUBECONFIG_PATH="/etc/kubernetes/admin.conf"

KUBECONFIG_CONTENT=$(sudo cat "$KUBECONFIG_PATH" 2>/dev/null)
if [ -z "$KUBECONFIG_CONTENT" ]; then
    echo "ERROR: Could not read $KUBECONFIG_PATH"
    exit 1
fi
echo "Read kubeconfig ($(echo "$KUBECONFIG_CONTENT" | wc -l) lines)"

# Get crumb + session
CRUMB_JSON=$(curl -s -u "$USER:$PASS" -c "$COOKIE" "$JENKINS/crumbIssuer/api/json")
CRUMB=$(echo "$CRUMB_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['crumb'])")
CRUMB_FIELD=$(echo "$CRUMB_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['crumbRequestField'])")
echo "Crumb: OK"

# Escape the kubeconfig for Groovy
KUBECONFIG_ESCAPED=$(echo "$KUBECONFIG_CONTENT" | sed "s/'/\\\\'/g" | awk '{printf "%s\\n", $0}')

GROOVY_SCRIPT=$(cat << HEREDOC
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl
import com.cloudbees.plugins.credentials.SecretBytes
import hudson.util.Secret

def store = SystemCredentialsProvider.getInstance().getStore()
def domain = Domain.global()

def existing = store.getCredentials(domain).find { it.id == 'kubeconfig' }
if (existing) {
    store.removeCredentials(domain, existing)
    println "Removed old kubeconfig credential"
}

def kubeconfigContent = '''${KUBECONFIG_CONTENT}'''
def secretBytes = SecretBytes.fromBytes(kubeconfigContent.getBytes("UTF-8"))
def newCred = new FileCredentialsImpl(
    CredentialsScope.GLOBAL,
    'kubeconfig',
    'K8s admin kubeconfig',
    'admin.conf',
    secretBytes
)
store.addCredentials(domain, newCred)
println "Added fresh kubeconfig credential"
HEREDOC
)

CODE=$(curl -s -o /tmp/kube_cred_response.txt -w "%{http_code}" \
    -u "$USER:$PASS" \
    -b "$COOKIE" \
    -H "$CRUMB_FIELD: $CRUMB" \
    -X POST "$JENKINS/scriptText" \
    --data-urlencode "script=${GROOVY_SCRIPT}")

echo "HTTP $CODE"
cat /tmp/kube_cred_response.txt | head -5
