#!/bin/bash
# YBrainy Jenkins Webhook + SCM Polling Setup
# Run after startup.sh: sudo bash .../DEVOPS/setup-webhook-polling.sh
#
# This script does two things:
#   1. Configures all 4 CI pipelines to poll GitHub every 1 minute (immediate fallback)
#   2. Optionally starts ngrok to expose Jenkins for real GitHub webhooks

JENKINS="http://localhost:8086"
JENKINS_USER="aziz"
JENKINS_PASS="Joncina85738573"
REPO_PATH="/mnt/c/Users/azizs/Downloads/ESPRIT-PI-4SAE4-2025-2026-Ybrainy"

JOBS=("course-service-CI" "lesson-service-CI" "quiz-service-CI" "enrollment-service-CI")

echo "============================================="
echo " YBrainy Jenkins Webhook/Polling Setup"
echo "============================================="

# ── Get Jenkins crumb ─────────────────────────────────────────────
echo ""
echo "[1/3] Getting Jenkins crumb..."
CRUMB_JSON=$(curl -s "$JENKINS/crumbIssuer/api/json")
CRUMB=$(echo "$CRUMB_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['crumb'])" 2>/dev/null)
CRUMB_FIELD=$(echo "$CRUMB_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['crumbRequestField'])" 2>/dev/null)

if [ -z "$CRUMB" ]; then
  echo "  WARNING: Could not get crumb. Jenkins may still be starting."
  echo "  Continuing with SCM polling via config.xml..."
fi
echo "  → Crumb obtained"

# ── Configure SCM polling for each CI job ────────────────────────
echo ""
echo "[2/3] Configuring SCM polling (every 1 minute) for all CI jobs..."

for JOB in "${JOBS[@]}"; do
  CONFIG_PATH="/var/lib/jenkins/jobs/$JOB/config.xml"
  if [ ! -f "$CONFIG_PATH" ]; then
    echo "  SKIP: $JOB (config.xml not found — job may not exist yet)"
    continue
  fi

  # Add or update SCM polling trigger to every 1 minute
  if grep -q '<spec>' "$CONFIG_PATH"; then
    sed -i 's|<spec>.*</spec>|<spec>* * * * *</spec>|g' "$CONFIG_PATH"
    echo "  → Updated polling for $JOB"
  elif grep -q 'SCMTrigger' "$CONFIG_PATH"; then
    sed -i 's|<spec/>|<spec>* * * * *</spec>|' "$CONFIG_PATH"
    echo "  → Updated polling for $JOB"
  else
    # Insert polling trigger before </triggers> or after <triggers/>
    if grep -q '<triggers/>' "$CONFIG_PATH"; then
      sed -i 's|<triggers/>|<triggers><hudson.triggers.SCMTrigger><spec>* * * * *</spec><ignorePostCommitHooks>false</ignorePostCommitHooks></hudson.triggers.SCMTrigger></triggers>|' "$CONFIG_PATH"
    elif grep -q '</triggers>' "$CONFIG_PATH"; then
      sed -i 's|</triggers>|<hudson.triggers.SCMTrigger><spec>* * * * *</spec><ignorePostCommitHooks>false</ignorePostCommitHooks></hudson.triggers.SCMTrigger></triggers>|' "$CONFIG_PATH"
    fi
    echo "  → Added polling trigger to $JOB"
  fi
done

# Reload Jenkins configuration
systemctl reload jenkins 2>/dev/null || curl -s -X POST "$JENKINS/reload" -H "$CRUMB_FIELD: $CRUMB" > /dev/null
echo "  → Jenkins configuration reloaded"

# ── Optionally start ngrok for real webhook ───────────────────────
echo ""
echo "[3/3] GitHub Webhook via ngrok (optional — for live demo)..."

if command -v ngrok &> /dev/null; then
  echo "  ngrok is installed. Starting tunnel on port 8086..."
  # Kill existing ngrok
  pkill ngrok 2>/dev/null || true
  sleep 1
  ngrok http 8086 --log=stdout > /tmp/ngrok.log 2>&1 &
  sleep 4
  NGROK_URL=$(curl -s http://localhost:4040/api/tunnels 2>/dev/null | python3 -c "import sys,json; tunnels=json.load(sys.stdin)['tunnels']; print([t['public_url'] for t in tunnels if t['proto']=='https'][0])" 2>/dev/null)
  if [ -n "$NGROK_URL" ]; then
    echo ""
    echo "  ╔══════════════════════════════════════════════╗"
    echo "  ║  NGROK TUNNEL ACTIVE                         ║"
    echo "  ║  Jenkins public URL: $NGROK_URL              ║"
    echo "  ║                                              ║"
    echo "  ║  Add this to GitHub webhooks:                ║"
    echo "  ║  ${NGROK_URL}/github-webhook/               ║"
    echo "  ║                                              ║"
    echo "  ║  GitHub → repo → Settings → Webhooks →      ║"
    echo "  ║  Add webhook → Payload URL above            ║"
    echo "  ║  Content type: application/json             ║"
    echo "  ║  Event: Just the push event                  ║"
    echo "  ╚══════════════════════════════════════════════╝"
  else
    echo "  ngrok started but URL not yet available. Check: curl http://localhost:4040/api/tunnels"
  fi
else
  echo "  ngrok not found. To install:"
  echo "    curl -s https://ngrok-agent.s3.amazonaws.com/ngrok.asc | sudo tee /etc/apt/trusted.gpg.d/ngrok.asc"
  echo "    echo 'deb https://ngrok-agent.s3.amazonaws.com buster main' | sudo tee /etc/apt/sources.list.d/ngrok.list"
  echo "    sudo apt update && sudo apt install ngrok"
  echo "    ngrok config add-authtoken <your-token-from-dashboard.ngrok.com>"
  echo ""
  echo "  NOTE: SCM polling (every 1 min) is already configured above."
  echo "  On git push, Jenkins will auto-trigger within 60 seconds."
  echo "  For a LIVE demo, do: git push, then wait ~30s, Jenkins triggers."
fi

echo ""
echo "============================================="
echo " SETUP COMPLETE"
echo "  • SCM polling: every 1 minute (all 4 CI jobs)"
echo "  • Webhook: $([ -n "$NGROK_URL" ] && echo "$NGROK_URL/github-webhook/" || echo "Run ngrok for live webhook")"
echo "============================================="
