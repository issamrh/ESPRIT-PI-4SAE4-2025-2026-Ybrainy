#!/bin/bash
# Resets Jenkins admin password to: ybrainy2026
# Run with: sudo bash /mnt/c/Users/azizs/Downloads/.../DEVOPS/reset-jenkins-admin.sh

set -e
ADMIN_DIR=$(ls -d /var/lib/jenkins/users/admin_* 2>/dev/null | head -1)

if [ -z "$ADMIN_DIR" ]; then
    echo "ERROR: Jenkins admin user directory not found"
    exit 1
fi

NEW_HASH=$(python3 -c "import bcrypt; print(bcrypt.hashpw(b'ybrainy2026', bcrypt.gensalt(10)).decode())" 2>/dev/null \
    || python3 -c "
import subprocess, sys
result = subprocess.run(['pip3', 'install', '--quiet', 'bcrypt'], capture_output=True)
import bcrypt
print(bcrypt.hashpw(b'ybrainy2026', bcrypt.gensalt(10)).decode())
" 2>/dev/null)

if [ -z "$NEW_HASH" ]; then
    echo "ERROR: could not generate bcrypt hash. Install python3-bcrypt:"
    echo "  sudo apt-get install -y python3-bcrypt"
    exit 1
fi

ESCAPED_HASH=$(echo "$NEW_HASH" | sed 's/[\/&]/\\&/g')
sed -i "s|#jbcrypt:[^<]*|#jbcrypt:${ESCAPED_HASH}|" "$ADMIN_DIR/config.xml"

echo "Password updated in $ADMIN_DIR/config.xml"
echo "Reloading Jenkins configuration..."
systemctl reload jenkins 2>/dev/null || systemctl restart jenkins

sleep 5
echo "Done. Jenkins admin password is now: ybrainy2026"
