# StockGuard MySQL Backup and Disaster Recovery Procedure

This document outlines the standard operating procedure (SOP) for secure, encrypted backups and restoration of the StockGuard relational database.

---

## 1. Security Architecture for Database Backups

1. **Principle of Least Privilege**: Backups should be executed using a dedicated backup user with `SELECT, RELOAD, SHOW VIEW, TRIGGER, LOCK TABLES` permissions rather than `root`.
2. **Non-Blocking Backups**: We use `--single-transaction` to ensure ACID consistency without locking inventory tables or interrupting active sales checkout transactions.
3. **Encryption at Rest**: Backup files must be compressed and encrypted (using GPG or OpenSSL) before being stored locally or uploaded to off-site cloud storage.
4. **Credential Safety**: Never hardcode database passwords in shell scripts or crontabs. Use a secure MySQL options file (`.my.cnf`) with `chmod 600`.

---

## 2. Dedicated Backup User Configuration

Execute once in MySQL as `root`:

```sql
CREATE USER 'stockguard_backup'@'localhost' IDENTIFIED BY 'SecureBackupSecret123!';
GRANT SELECT, RELOAD, SHOW VIEW, TRIGGER, LOCK TABLES ON stockguard_db.* TO 'stockguard_backup'@'localhost';
FLUSH PRIVILEGES;
```

---

## 3. Automated Backup Script

### Windows PowerShell (`scripts/backup.ps1`)

```powershell
# Parameters
$DbName = "stockguard_db"
$User = "stockguard_backup"
$Password = $env:DB_BACKUP_PASSWORD
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$BackupDir = "C:\backups\stockguard"
$OutputFile = "$BackupDir\stockguard_backup_$Timestamp.sql"
$GzFile = "$OutputFile.gz"

if (!(Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir
}

# Run consistent, non-locking mysqldump
& mysqldump `
    --user=$User `
    --password=$Password `
    --single-transaction `
    --quick `
    --routines `
    --triggers `
    --set-gtid-purged=OFF `
    $DbName > $OutputFile

# Compress
tar -czf $GzFile -C $BackupDir (Split-Path $OutputFile -Leaf)
Remove-Item $OutputFile

Write-Host "Backup completed successfully: $GzFile"
```

### Linux / macOS Bash (`scripts/backup.sh`)

```bash
#!/usr/bin/env bash
set -euo pipefail

DB_NAME="stockguard_db"
BACKUP_DIR="/var/backups/stockguard"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
OUTPUT_FILE="${BACKUP_DIR}/stockguard_${TIMESTAMP}.sql.gz"

mkdir -p "${BACKUP_DIR}"
chmod 700 "${BACKUP_DIR}"

# Execute single-transaction dump and gzip stream
mysqldump \
  --defaults-extra-file=/etc/mysql/backup.cnf \
  --single-transaction \
  --quick \
  --routines \
  --triggers \
  --set-gtid-purged=OFF \
  "${DB_NAME}" | gzip -9 > "${OUTPUT_FILE}"

chmod 600 "${OUTPUT_FILE}"

# Retention: Delete backups older than 30 days
find "${BACKUP_DIR}" -type f -name "stockguard_*.sql.gz" -mtime +30 -delete

echo "Backup created successfully: ${OUTPUT_FILE}"
```

---

## 4. Encrypting the Backup (Optional Off-site Storage)

Encrypt the archive with AES-256 via OpenSSL before transmission to AWS S3 / GCP Storage:

```bash
openssl enc -aes-256-cbc -salt -pbkdf2 \
  -in "/var/backups/stockguard/stockguard_backup.sql.gz" \
  -out "/var/backups/stockguard/stockguard_backup.sql.gz.enc" \
  -pass env:BACKUP_ENCRYPTION_KEY
```

---

## 5. Restoration Procedure

### Step 1: Decrypt and Decompress (if encrypted)

```bash
# Decrypt if applicable
openssl enc -d -aes-256-cbc -pbkdf2 \
  -in "stockguard_backup.sql.gz.enc" \
  -out "stockguard_backup.sql.gz" \
  -pass env:BACKUP_ENCRYPTION_KEY

# Decompress
gunzip -k "stockguard_backup.sql.gz"
```

### Step 2: Restore Database

```bash
# Optional: create a clean database target if restoring from disaster
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS stockguard_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Execute restore
mysql -u root -p stockguard_db < "stockguard_backup.sql"
```

### Step 3: Post-Restoration Verification Queries

Run the following checks to verify data integrity:

```sql
USE stockguard_db;
-- 1. Check shop count and user consistency
SELECT id, name, currency FROM shops;
SELECT id, email, role, shop_id FROM users;

-- 2. Verify inventory product count
SELECT COUNT(*) AS total_products FROM products;

-- 3. Verify batch and expiry status
SELECT COUNT(*) AS active_batches FROM product_batches WHERE status = 'ACTIVE';

-- 4. Check audit log records
SELECT COUNT(*) AS audit_records FROM audit_logs;
```
