#!/bin/bash
# Updates the sonar-token2 Jenkins credential with a fresh SonarQube token
JENKINS="http://localhost:8086"
USER="aziz"
PASS="Joncina85738573"
COOKIE="/tmp/jcookie2.txt"
SONAR_TOKEN="${1}"

if [ -z "$SONAR_TOKEN" ]; then
    echo "Usage: $0 <sonar-token>"
    exit 1
fi

# Get crumb + session
CRUMB_JSON=$(curl -s -u "$USER:$PASS" -c "$COOKIE" "$JENKINS/crumbIssuer/api/json")
CRUMB=$(echo "$CRUMB_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['crumb'])")
CRUMB_FIELD=$(echo "$CRUMB_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['crumbRequestField'])")
echo "Crumb: $CRUMB_FIELD = $CRUMB"

# Build the JSON payload
cat > /tmp/cred_payload.json << EOF
{"": "0", "credentials": {"scope": "GLOBAL", "id": "sonar-token2", "secret": "${SONAR_TOKEN}", "description": "SonarQube jenkins-token", "\$class": "org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl"}}
EOF

CODE=$(curl -s -o /tmp/cred_response.txt -w "%{http_code}" \
    -u "$USER:$PASS" \
    -b "$COOKIE" \
    -H "$CRUMB_FIELD: $CRUMB" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -X POST "$JENKINS/credentials/store/system/domain/_/credential/sonar-token2/updateSubmit" \
    --data-urlencode "json@/tmp/cred_payload.json")

echo "HTTP $CODE"
if [ "$CODE" != "200" ] && [ "$CODE" != "302" ]; then
    echo "Response body:"
    cat /tmp/cred_response.txt | head -20
fi
