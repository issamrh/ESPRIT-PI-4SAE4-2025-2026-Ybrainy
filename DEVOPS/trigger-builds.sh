#!/bin/bash
# Triggers Jenkins CI builds for the strongly-connected services (course + lesson)
# quiz-service and enrollment-service use GitHub Actions instead
JENKINS="http://localhost:8086"
USER="aziz"
PASS="Joncina85738573"
COOKIE_JAR="/tmp/jenkins_session.txt"

# Get crumb with session cookie
CRUMB_JSON=$(curl -s -u "$USER:$PASS" -c "$COOKIE_JAR" "$JENKINS/crumbIssuer/api/json")
CRUMB=$(echo "$CRUMB_JSON" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['crumb'])")
CRUMB_FIELD=$(echo "$CRUMB_JSON" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['crumbRequestField'])")

echo "Crumb field: $CRUMB_FIELD = $CRUMB"

for job in course-service-CI lesson-service-CI; do
    echo -n "Triggering $job ... "
    CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -u "$USER:$PASS" \
        -b "$COOKIE_JAR" \
        -H "$CRUMB_FIELD: $CRUMB" \
        -X POST "$JENKINS/job/$job/build")
    echo "HTTP $CODE"
done

echo ""
echo "Builds queued. Monitor at: $JENKINS"
