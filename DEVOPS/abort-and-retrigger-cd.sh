#!/bin/bash
JENKINS="http://localhost:8086"
USER="aziz"
PASS="Joncina85738573"
COOKIE="/tmp/jcookie_cd.txt"

CRUMB_JSON=$(curl -s -u "$USER:$PASS" -c "$COOKIE" "$JENKINS/crumbIssuer/api/json")
CRUMB=$(echo "$CRUMB_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['crumb'])")
CRUMB_FIELD=$(echo "$CRUMB_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['crumbRequestField'])")

for job in course-service-CD lesson-service-CD; do
    BUILD_NUM=$(curl -s -u "$USER:$PASS" "http://localhost:8086/job/$job/lastBuild/api/json" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['number']) if d.get('building') else print('')" 2>/dev/null)
    if [ -n "$BUILD_NUM" ]; then
        echo -n "Aborting $job #$BUILD_NUM ... "
        CODE=$(curl -s -o /dev/null -w "%{http_code}" \
            -u "$USER:$PASS" -b "$COOKIE" \
            -H "$CRUMB_FIELD: $CRUMB" \
            -X POST "$JENKINS/job/$job/$BUILD_NUM/stop")
        echo "HTTP $CODE"
        sleep 3
    fi

    echo -n "Triggering $job ... "
    CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -u "$USER:$PASS" -b "$COOKIE" \
        -H "$CRUMB_FIELD: $CRUMB" \
        -X POST "$JENKINS/job/$job/build")
    echo "HTTP $CODE"
done
