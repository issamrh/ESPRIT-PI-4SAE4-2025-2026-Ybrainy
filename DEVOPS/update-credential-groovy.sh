#!/bin/bash
# Updates sonar-token2 credential via Jenkins Groovy script console
JENKINS="http://localhost:8086"
USER="aziz"
PASS="Joncina85738573"
COOKIE="/tmp/jcookie3.txt"
NEW_TOKEN="${1}"

if [ -z "$NEW_TOKEN" ]; then
    echo "Usage: $0 <new-token>"
    exit 1
fi

# Get crumb + session
CRUMB_JSON=$(curl -s -u "$USER:$PASS" -c "$COOKIE" "$JENKINS/crumbIssuer/api/json")
CRUMB=$(echo "$CRUMB_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['crumb'])")
CRUMB_FIELD=$(echo "$CRUMB_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['crumbRequestField'])")
echo "Crumb: OK"

GROOVY_SCRIPT=$(cat << GROOVYEOF
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import hudson.util.Secret

def store = SystemCredentialsProvider.getInstance().getStore()
def domain = Domain.global()

// Remove old credential
def existing = store.getCredentials(domain).find { it.id == 'sonar-token2' }
if (existing) {
    store.removeCredentials(domain, existing)
    println "Removed old sonar-token2"
}

// Add new credential
def newCred = new StringCredentialsImpl(
    CredentialsScope.GLOBAL,
    'sonar-token2',
    'SonarQube jenkins-token',
    Secret.fromString('${NEW_TOKEN}')
)
store.addCredentials(domain, newCred)
println "Added sonar-token2 with new token"
GROOVYEOF
)

CODE=$(curl -s -o /tmp/groovy_response.txt -w "%{http_code}" \
    -u "$USER:$PASS" \
    -b "$COOKIE" \
    -H "$CRUMB_FIELD: $CRUMB" \
    -X POST "$JENKINS/scriptText" \
    --data-urlencode "script=${GROOVY_SCRIPT}")

echo "HTTP $CODE"
cat /tmp/groovy_response.txt
