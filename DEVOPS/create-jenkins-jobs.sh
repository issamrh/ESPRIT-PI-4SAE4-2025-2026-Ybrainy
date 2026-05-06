#!/bin/bash
# Creates all 4 CI+CD Jenkins pipeline jobs via Jenkins REST API
# Run once: sudo bash .../DEVOPS/create-jenkins-jobs.sh
# Prerequisites: Jenkins running at localhost:8086, git repo checked out

JENKINS="http://localhost:8086"
REPO_URL="https://github.com/issamrh/ESPRIT-PI-4SAE4-2025-2026-Ybrainy.git"
BRANCH="*/3la-5atr-houssem"

echo "============================================="
echo " YBrainy Jenkins Jobs Creator"
echo "============================================="

# ── Get crumb ─────────────────────────────────────────────────────
CRUMB_JSON=$(curl -s "$JENKINS/crumbIssuer/api/json")
CRUMB=$(echo "$CRUMB_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['crumb'])" 2>/dev/null)
CRUMB_FIELD=$(echo "$CRUMB_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['crumbRequestField'])" 2>/dev/null)
echo "Crumb: $CRUMB_FIELD = $CRUMB"

create_pipeline_job() {
  local JOB_NAME="$1"
  local JENKINSFILE_PATH="$2"

  echo ""
  echo "Creating job: $JOB_NAME (Jenkinsfile: $JENKINSFILE_PATH)"

  # Check if job exists
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$JENKINS/job/$JOB_NAME/")
  if [ "$STATUS" = "200" ]; then
    echo "  → Job already exists, skipping"
    return
  fi

  CONFIG_XML=$(cat <<EOF
<?xml version='1.1' encoding='UTF-8'?>
<flow-definition plugin="workflow-job">
  <actions/>
  <description>YBrainy $JOB_NAME pipeline</description>
  <keepDependencies>false</keepDependencies>
  <properties>
    <org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
      <triggers>
        <hudson.triggers.SCMTrigger>
          <spec>* * * * *</spec>
          <ignorePostCommitHooks>false</ignorePostCommitHooks>
        </hudson.triggers.SCMTrigger>
      </triggers>
    </org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
  </properties>
  <definition class="org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition" plugin="workflow-cps">
    <scm class="hudson.plugins.git.GitSCM" plugin="git">
      <configVersion>2</configVersion>
      <userRemoteConfigs>
        <hudson.plugins.git.UserRemoteConfig>
          <url>$REPO_URL</url>
        </hudson.plugins.git.UserRemoteConfig>
      </userRemoteConfigs>
      <branches>
        <hudson.plugins.git.BranchSpec>
          <name>$BRANCH</name>
        </hudson.plugins.git.BranchSpec>
      </branches>
      <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
      <submoduleCfg class="empty-list"/>
      <extensions/>
    </scm>
    <scriptPath>$JENKINSFILE_PATH</scriptPath>
    <lightweight>true</lightweight>
  </definition>
  <triggers/>
  <disabled>false</disabled>
</flow-definition>
EOF
)

  HTTP_CODE=$(curl -s -o /tmp/jenkins_create_response.txt -w "%{http_code}" \
    -X POST "$JENKINS/createItem?name=$JOB_NAME" \
    -H "$CRUMB_FIELD: $CRUMB" \
    -H "Content-Type: application/xml" \
    --data-binary "$CONFIG_XML")

  if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
    echo "  → Created: $JOB_NAME (HTTP $HTTP_CODE)"
  else
    echo "  ERROR creating $JOB_NAME (HTTP $HTTP_CODE)"
    cat /tmp/jenkins_create_response.txt | head -5
  fi
}

# Create all 4 CI jobs
create_pipeline_job "course-service-CI"    "YBRAINY/Course/tp-foyer/Jenkinsfile"
create_pipeline_job "lesson-service-CI"    "YBRAINY/Lesson/Jenkinsfile"
create_pipeline_job "quiz-service-CI"      "YBRAINY/Quiz/quiz-service/Jenkinsfile"
create_pipeline_job "enrollment-service-CI" "YBRAINY/Enrollment/enrollment-service/Jenkinsfile"

echo ""
echo "============================================="
echo " DONE. Jobs created."
echo " Go to http://localhost:8086 to verify."
echo " Each job has SCM polling every 1 minute."
echo "============================================="
