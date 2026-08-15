# Deploying Job Portal to AWS (Single EC2 Instance) — Full Guide

A complete, from-zero walkthrough for deploying this project's full stack to a single right-sized AWS EC2
instance, using the existing `docker-compose.yml` unmodified. Written so someone who has never touched AWS
before can follow it end to end — updated with real gotchas hit during an actual first deployment (Windows
SSH quirks, a security group mismatch, a CORS 403, and two application bugs), not just the happy path.

**Why single-instance, not AWS's free tier:** AWS's free tier (12 months, `t2.micro`/`t3.micro`) caps out at
**1 GB RAM**. This stack — Postgres, Kafka, Zookeeper, Redis, Zipkin, 7 Spring Boot services, and the
frontend — idles around 3.4–4 GB and has combined `mem_limit` ceilings near 7.2 GB across
`docker-compose.yml`. It will not run on the free tier, full stop. This guide right-sizes a real instance
instead — real (small) cost, but it will actually work. See [section 1](#1-what-this-actually-costs) for
exact numbers.

**No secrets in this document.** Wherever a step needs a value from your `.env` file (JWT secret, mail
password, API keys) or the seeded admin credentials, this guide tells you *where* that value lives in the
repo instead of printing it.

**Written:** August 2026. AWS pricing and console layouts change over time — the numbers in section 1 are
ballpark for `us-east-1`/`ap-south-1`; check the [AWS Pricing Calculator](https://calculator.aws) for current
numbers before committing.

---

## Table of contents

1. [What this actually costs](#1-what-this-actually-costs)
2. [How AWS's building blocks map to this deployment](#2-how-awss-building-blocks-map-to-this-deployment)
3. [Sizing the instance](#3-sizing-the-instance)
4. [Pre-flight checklist](#4-pre-flight-checklist)
5. [Step-by-step deployment](#5-step-by-step-deployment)
6. [Verification](#6-verification)
7. [Keeping it running](#7-keeping-it-running)
8. [Troubleshooting](#8-troubleshooting)
9. [Cost control (AWS Budgets + auto-stop)](#9-cost-control-aws-budgets--auto-stop)
10. [Stopping / pausing the deployment](#10-stopping--pausing-the-deployment)
11. [Resuming later — reconfiguring the IP](#11-resuming-later--reconfiguring-the-ip)
12. [Optional next steps](#12-optional-next-steps)

---

## 1. What this actually costs

Ballpark, on-demand pricing — **verify current numbers before committing**, this is not a quote:

| Item | Ballpark cost |
|---|---|
| `t3.large` (2 vCPU / 8 GB) or `t4g.large` (ARM/Graviton, same specs, usually a bit cheaper), on-demand, running 24/7 | ~$55–65/month |
| `t3.xlarge` / `t4g.xlarge` (4 vCPU / 16 GB), on-demand, running 24/7 | ~$110–130/month |
| 50 GB `gp3` EBS root volume | ~$4/month — bills whether the instance is running or stopped |
| **Elastic IP** | **~$3.60/month (≈$0.005/hour) as long as it's allocated to your account — regardless of whether it's attached to a running instance, a stopped instance, or nothing at all.** AWS changed this in Feb 2024; older guides/blog posts saying "free while attached to a running instance" are describing the pre-2024 rule and are wrong today. See [section 10](#10-stopping--pausing-the-deployment) for what this means when you stop the instance. |
| Data transfer out | first 100 GB/month free, then ~$0.09/GB — irrelevant at low traffic |
| Security groups | **always free**, no matter what |

Two levers that cut this significantly without changing anything else in this guide:
- **A 1-year Compute Savings Plan** or **Reserved Instance** — roughly 30–40% off on-demand for a
  no-flexibility-needed workload like this one.
- **Stop the instance when you're not using it** (e.g. a personal project you only demo occasionally) — EC2
  compute bills per-second while running, $0 while stopped. The EBS volume and (if kept) the Elastic IP keep
  billing regardless — see section 10 for the exact numbers and trade-offs.

If this needs to cost close to $0, the honest options are the [existing OCI guide](OCI_DEPLOYMENT_GUIDE.md)
(genuinely free, more setup) or trimming this stack down to fit AWS's free-tier 1GB — the latter is a
re-architecture (dropping Kafka/Zookeeper/Zipkin, replacing with lighter alternatives), not a deploy config
change, and isn't what this guide does.

---

## 2. How AWS's building blocks map to this deployment

If you've only used OCI or a PaaS before, here's the AWS vocabulary for the same concepts:

- **EC2 Instance** — the VM. Runs all 15 containers via plain `docker compose up -d`, same as running this
  stack locally.
- **AMI (Amazon Machine Image)** — the OS image. This guide uses **Ubuntu Server 24.04 LTS**, same as the
  OCI guide, so the Docker install and OS commands below are identical to that guide's. Works on both the
  x86_64 build (paired with `t3.*` instance types) and the arm64 build (paired with `t4g.*` — see section 3).
- **Default VPC** — unlike OCI, you do **not** need to build a VPC from scratch. Every AWS account gets a
  default VPC per region with a public subnet and Internet Gateway already wired up. Use it — there's no
  reason to build a custom one for a single instance.
- **Security Group** — AWS's equivalent of OCI's Security List: a stateful firewall attached to the
  instance itself (not the subnet). Only one is needed here since everything lives on one host. Always free.
- **Key Pair** — same concept as OCI's SSH key pair: generated once, private half (`.pem`) downloaded once,
  no password auth.
- **Elastic IP** — a static public IP you allocate and attach to the instance. Without one, a stopped/started
  instance gets a **new** public IP each time, which breaks any DNS record or bookmark pointing at it.
  Allocating one is worth it for convenience, but it is **not free** — see section 1's corrected pricing.
- **EBS Volume** — the instance's disk (equivalent to OCI's boot volume). `gp3` is the current
  general-purpose default — cheaper and faster than the older `gp2`.
- **IAM** — mostly not needed for running the stack itself (it pulls public/self-built images, not private
  ECR images, so no instance role is required for that). It *is* used later in section 9 to let AWS Budgets
  auto-stop the instance on your behalf — the console creates that role for you, you don't hand-write it.

**The mental model:** Internet → Security Group → instance's public/Elastic IP → OS firewall (`ufw`, off by
default on Ubuntu, worth checking with `sudo ufw status` rather than assuming) → the port the app listens
on. Because this is one instance running the unmodified `docker-compose.yml`, there's no cross-host hop to
reason about — `frontend` reaches `api-gateway` over the local Docker network exactly like it does when you
run this stack locally.

---

## 3. Sizing the instance

`docker-compose.yml`'s `mem_limit` values sum to ~7.2 GB across all 15 containers (these are hard caps, not
guaranteed reservations — most services idle well under their cap). Two reasonable choices, in either the
x86 (`t3`) or ARM/Graviton (`t4g`) family — this stack runs fine on both; every image it uses (Postgres,
Kafka, Zookeeper, Redis, Zipkin) has ARM64 builds available, and Maven/the JVM compile and run natively on
either:

- **`t3.large` / `t4g.large` (2 vCPU / 8 GB)** — the recommended default. Comfortably fits idle usage with
  headroom for Kafka/Postgres under real traffic. `t4g` (Graviton/ARM) is usually a bit cheaper than `t3`
  for the same specs and works without any changes to this guide — pick whichever the console defaults you
  toward, or `t4g` deliberately for the small cost saving.
  - Both families are **burstable** — sustained 100% CPU (e.g. the initial multi-service Maven build) draws
    down a CPU credit balance. For a low-traffic personal deployment this is a non-issue; if you're
    consistently pegging CPU, move to an `m`/`m7g` or `c`/`c7g` family instance (non-burstable, no credit
    mechanic).
- **`t3.xlarge` / `t4g.xlarge` (4 vCPU / 16 GB)** — meaningfully more headroom, ~2x the cost. Worth it if
  you'll run this under real concurrent traffic rather than solo/demo use.

Either way: **50 GB `gp3` root volume** — enough for the OS, Docker images, and the 7-service Maven build's
layer cache, with room to spare.

---

## 4. Pre-flight checklist

- [ ] An AWS account with a payment method attached (this is not free-tier eligible, per section 1).
- [ ] Know your local project path: `d:/PRACTICE/New folder` — you'll `scp` `.env` from here. This file
      already has your real secrets in it; nothing further to fill in.
- [ ] A place to store the `.pem` private key you're about to download — shown/downloadable exactly once.
- [ ] Confirm the repo is pushed to GitHub and up to date: `origin https://github.com/OmNaphade/job-hunt.git`.
- [ ] Pick a region (e.g. `ap-south-1`) — all steps below are region-agnostic otherwise.
- [ ] **Windows only:** know that your `.pem` file needs its permissions locked down with `icacls` before
      `ssh` will accept it — covered in step 5.3, this trips up most first-time Windows users.

---

## 5. Step-by-step deployment

### Step 5.1 — Launch the instance

1. AWS Console → **EC2 → Launch Instance**.
2. Name it (e.g. `job-portal-prod`).
3. **AMI**: search **Ubuntu** → select **Ubuntu Server 24.04 LTS**. If you're going with a `t4g.*` (ARM)
   instance type, make sure the arm64 build is what gets selected — the console usually does this
   automatically once you pick the shape below.
4. **Instance type**: `t3.large`/`t4g.large` (or the `.xlarge` variants — see section 3).
5. **Key pair**: **Create new key pair** → type **RSA**, format **.pem** → give it a name → **Create key
   pair** — this downloads the private key immediately, save it permanently (e.g. your Downloads folder).
6. **Network settings**: leave the default VPC and a public subnet selected (this is already correct — no
   custom VPC needed, per section 2). Click **Edit** on the security group and configure:
   - **Create new security group**.
   - Rule 1: SSH (port 22), source **My IP** (not `0.0.0.0/0` — restrict SSH to your own address). Note:
     your ISP may rotate this IP over time — see step 8's troubleshooting entry if SSH later times out.
   - Rule 2: HTTP (port 80), source **Anywhere (0.0.0.0/0)** — this is the public-facing port.
   - (Leave 443 out for now; add it in [section 12](#12-optional-next-steps) if you set up a domain.)
7. **Storage**: change the root volume to **50 GB**, type **gp3**.
8. **Launch instance**.
9. Wait for **Instance state: Running** *and* **Status check: 2/2 (or 3/3) checks passed** before trying to
   connect — a couple of minutes. SSH will time out if you try earlier, even though the instance looks
   "Running."

### Step 5.2 — Allocate and attach an Elastic IP

Without this, restarting the instance later changes its public IP. (This has a small ongoing cost — see
section 1 — which is why section 10 covers releasing it if you'd rather not pay it while stopped.)

1. **EC2 → Network & Security → Elastic IPs → Allocate Elastic IP address** → **Allocate**.
2. Select the new address → **Actions → Associate Elastic IP address** → **Resource type: Instance** → pick
   your instance from the dropdown → **Associate**.
3. Confirm it worked: back on the Elastic IPs list, the **Associated instance ID** column should show your
   instance's ID, not a dash.
4. Note this IP — it's `<INSTANCE_IP>` for the rest of this guide.

### Step 5.3 — Connect via SSH

**Windows-specific first:** your `.pem` file needs locked-down permissions or `ssh` refuses to use it. In
PowerShell (once):
```powershell
icacls "C:\path\to\your-key.pem" /inheritance:r
$acl = "$env:USERNAME" + ":R"
icacls "C:\path\to\your-key.pem" /grant:r $acl
```
Don't try to inline `"$env:USERNAME:R"` directly as one string — PowerShell's parser mishandles the colon
right after the variable name and silently produces an empty value, which then makes `icacls` reject the
whole command with `Invalid parameter "/grant:r"`. The two-line `$acl` form above avoids that.

Then connect:
```powershell
ssh -i "C:\path\to\your-key.pem" ubuntu@<INSTANCE_IP>
```
First connection only, you'll be asked to confirm the host's fingerprint:
```
Are you sure you want to continue connecting (yes/no/[fingerprint])?
```
Type the full word **`yes`** — a bare `y` is rejected.

(Ubuntu AMIs use the default user `ubuntu`, not `ec2-user`.)

### Step 5.4 — Install Docker

Identical to the OCI guide's equivalent step, since both use Ubuntu 24.04:

```bash
sudo apt update
sudo apt install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo usermod -aG docker $USER
newgrp docker
docker compose version   # sanity check — should print a version, not "command not found"
sudo ufw status           # confirm inactive, or explicitly allow 80/tcp if active
```

### Step 5.5 — Get the code and `.env` onto the host

On the server:
```bash
git clone https://github.com/OmNaphade/job-hunt.git
cd job-hunt
```

From your **local** machine, in a **separate** terminal window (don't close the SSH session) — `.env` is
gitignored on purpose and must be copied separately:
```powershell
scp -i "C:\path\to\your-key.pem" "d:\PRACTICE\New folder\.env" ubuntu@<INSTANCE_IP>:~/job-hunt/.env
```

Confirm it arrived, back in the SSH session, without printing the actual secret values:
```bash
test -f ~/job-hunt/.env && echo "present" && wc -l ~/job-hunt/.env
```

### Step 5.6 — Allow your deployed address through CORS

**This step is easy to miss and causes a 403 "Invalid CORS request" on every login/API call if skipped** —
worth doing now, before the first launch, rather than discovering it later. `api_gateway`'s CORS filter
(`GatewayCorsFilter`) only allows origins listed in `CORS_ALLOWED_ORIGINS`, which by default only lists
`localhost` dev origins.

Append your instance's address to it (this adds a new line to `.env`; Docker Compose uses the *last* value
for a repeated key, so this overrides the default without needing to edit the existing line):
```bash
echo 'CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173,http://<INSTANCE_IP>' >> ~/job-hunt/.env
```
Replace `<INSTANCE_IP>` with your actual Elastic IP from step 5.2. If you later add a domain + HTTPS
(section 12), add `https://yourdomain.com` here too at that point.

### Step 5.7 — Build and launch

```bash
cd ~/job-hunt
docker compose up -d --build
watch docker compose ps
```
Give the first build **15–30 minutes** — compiling 7 Spring Boot services from source. Long stretches with
no visible output are normal, not a hang. If your SSH session drops mid-build, just reconnect and re-run the
same command — Docker resumes/reuses what it already built.

---

## 6. Verification

From your **local** machine (PowerShell — use `curl.exe`, not plain `curl`, which is aliased to something
else in PowerShell):
```powershell
curl.exe -i http://<INSTANCE_IP>/                # expect the frontend HTML, not an error
curl.exe -i http://<INSTANCE_IP>/api/jobs         # expect 401 (auth required)
```

Better yet, open `http://<INSTANCE_IP>/` in an actual browser — that's the real end-to-end check.

On the instance, confirm every container is actually healthy, not just running:
```bash
docker compose ps -a
docker logs job-service --tail 50    # spot-check one service for startup errors
docker logs frontend --tail 30       # confirm Caddy started cleanly
```

To confirm the full auth flow works (not just that the page renders), log in with the seeded admin account —
its email/password are defined as `ADMIN_SEED_EMAIL` / `ADMIN_SEED_PASSWORD` in `auth-service`'s environment
block in `docker-compose.yml` (not a secret file, safe to look up directly in the repo).

---

## 7. Keeping it running

- **`restart: unless-stopped` is already set on every service** in `docker-compose.yml` — a container crash
  or instance reboot self-heals without SSH access.
- **Docker starts on boot automatically** (enabled during install in step 5.4).
- **The Elastic IP stays associated across a stop/start of the same instance** (as long as you don't
  explicitly disassociate it — see section 10) — so a plain stop/start keeps the same public IP. It's only
  "reconfigure the IP" work if you go further and release it, covered in section 11.
- **Watch resource usage**:
  ```bash
  docker stats --no-stream
  ```
  If Kafka's memory percentage creeps toward its `mem_limit` cap (currently `512m` in `docker-compose.yml`)
  under real usage, that's the first value to raise.

---

## 8. Troubleshooting

### 8.1 — Issues you may still hit

**SSH connection refused / times out**
Almost always the security group's port 22 rule not matching your *current* public IP — home/mobile ISPs
rotate this, and "My IP" is captured once at rule-creation time, not re-evaluated live. Check your current
IP at `https://checkip.amazonaws.com`, compare it against the SSH rule's **Source** value on the security
group's **Inbound rules** tab, and update it (**Edit inbound rules** → set the port 22 source to your
current IP, or pick **My IP** again to re-capture it) if they differ. This is the single most common cause
of "everything looks Running but I can't connect."

**`icacls` fails with `Invalid parameter "/grant:r"` (Windows)**
See the note in step 5.3 — don't inline `"$env:USERNAME:R"`, use the two-line `$acl` variable form instead.

**Login/API calls fail with a browser "Network Error" and a failed CORS preflight in DevTools**
If you skipped step 5.6, this is `CORS_ALLOWED_ORIGINS` not including your instance's address. Run the
`echo ... >> ~/job-hunt/.env` command from step 5.6, then `docker compose up -d api-gateway` to pick up the
new value (env var changes need a container recreate, which `up -d` handles).

**Frontend crashes with "This page hit an unexpected error" / `TypeError: crypto.randomUUID is not a
function`, specifically on pages with loading skeletons or toasts**
`crypto.randomUUID()` only exists in secure browser contexts (HTTPS or `localhost`) — a plain-HTTP
deployment by IP address doesn't qualify, so the browser throws. **Already fixed in `main`** as of
`frontend/src/lib/id.js` (a `generateId()` helper with a fallback for non-secure contexts, used by
`StateBlocks.jsx` and `ToastProvider.jsx`) — a fresh `git clone` today won't hit this. Only relevant if
you're deploying from an older commit; if so, `git pull origin main` before building picks up the fix.

**Instance unreachable on port 80**
Check, in order: (a) the security group has an inbound rule for port 80 from `0.0.0.0/0`, (b) the Elastic IP
is actually associated with the instance (not just allocated — check the "Associated instance ID" column),
(c) `sudo ufw status` on the instance itself, (d) `docker compose ps` shows `frontend` as `Up`/healthy.

**Build runs out of memory / gets OOM-killed mid-compile**
`.large`'s 8 GB is normally enough, but if you sized down, Maven compiling 7 services concurrently is the
most memory-hungry moment in this whole guide. Either upsize the instance temporarily for the first build,
or build one service at a time with `docker compose build <service>` instead of `--build` on the full `up`.

**Kafka container exits immediately with `NodeExistsException` / `KeeperErrorCode = NodeExists`**
Zookeeper's image declares anonymous volumes not listed in `docker-compose.yml`, so stale broker state can
survive a plain container recreate:
```bash
docker compose rm -f -s -v kafka zookeeper   # -v removes the anonymous volumes too
docker compose up -d
```

**A service logs `violates check constraint "jobs_source_check"` when importing external jobs**
The CHECK constraint Hibernate generated on first table creation hasn't widened for newly added `JobSource`
enum values. Fix directly on the instance:
```bash
docker exec jobportal-postgres psql -U postgres -d jobapp_db -c "
ALTER TABLE jobs DROP CONSTRAINT jobs_source_check;
ALTER TABLE jobs ADD CONSTRAINT jobs_source_check CHECK (source IN (
  'RECRUITER','ADZUNA','HIMALAYAS','ARBEITNOW',
  'AI_DEV_JOBS','ARTIFICIAL_INTELLIGENCE_JOBS','FREEHIRE','FINDWORK','JOBDATALAKE',
  'REMOTIVE','JOBICY'
));
"
```

### 8.2 — Already fixed in `main` (context only, no action needed on a fresh clone)

- **API calls silently went to `localhost:8080` instead of the deployed host** —
  `frontend/src/lib/api.js` used `import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'`. Since `""`
  (the deliberate production value, meaning "same-origin") is falsy in JavaScript, `||` always fell back to
  `localhost:8080` regardless. Fixed by switching to `??` (nullish coalescing), which only falls back on a
  truly-unset value.
- **`crypto.randomUUID` crash on plain-HTTP deployments** — see 8.1 above; same fix, already merged.

---

## 9. Cost control (AWS Budgets + auto-stop)

Unlike OCI Budgets (alert-only), **AWS Budgets supports real automated actions** — worth using since, unlike
the OCI guide, this deployment is not free by default.

1. **Console → Billing and Cost Management → Budgets → Create budget → Customize (advanced) → Cost budget**.
2. **Budget details**: Monthly, recurring, fixed amount a bit above your expected spend from section 1 (e.g.
   $70) — padded so normal usage doesn't trip it, but a real overrun does.
3. **Alerts**: add thresholds at 80% and 100% of the budgeted amount, notify your email.
4. **Add action**: **Run an SSM automation on your EC2 instance in case of alert breach** → automation rule
   **AWS-StopEC2Instance** → target your instance's ID → **Execution role: Create a new role** (the console
   builds one scoped to just this action) → **Approval: Automatic** (or **Manual** if you'd rather review
   before it fires the first few times) → trigger threshold **100%**.
5. Confirm the email subscription if AWS sends a confirmation link.
6. **Test it** by temporarily setting the budget to something trivially low (e.g. $0.01) to confirm the
   alert/action actually fires, then set it back to your real number.

This covers **compute cost** specifically. It doesn't touch the EBS volume or a kept Elastic IP — both are
small (section 1), covered by section 10 if you want to eliminate them entirely while paused.

---

## 10. Stopping / pausing the deployment

### Step 10.1 — Stop the instance

EC2 Console → **Instances** → select your instance → **Instance state → Stop instance**. This halts compute
billing immediately (the ~$55–65/month line in section 1). Everything else — disk contents, container
definitions, the Elastic IP's association — is preserved untouched.

### Step 10.2 — Decide what to do about the Elastic IP

**This is the part that's easy to get wrong:** stopping the instance does **not** stop the Elastic IP's
~$3.60/month charge — that bills as long as the address is allocated to your account, running instance or
not (section 1). You have two honest options:

**Option A — Keep it (simplest, ~$3.60/month while paused)**
Do nothing further. Next time you start the instance, it comes back with the exact same public IP
automatically — no reconfiguration needed anywhere (section 11 is a no-op in this case). Reasonable if
you'll restart this within a few weeks and the small charge doesn't matter.

**Option B — Release it (zero IP cost, but the address changes next time)**
1. EC2 Console → **Network & Security → Elastic IPs** → select your IP.
2. **Actions → Disassociate Elastic IP address** → confirm. (Required first — AWS won't let you release an
   address that's still attached to something, even a stopped instance.)
3. With it still selected, **Actions → Release Elastic IP addresses** → confirm.
4. The charge stops immediately. Next time you start the instance, you'll need to allocate a fresh Elastic
   IP and reassociate it — section 11 walks through exactly that.

**Security groups need no decision here** — they never cost anything, stopped instance or not, so there's
nothing to clean up on that front.

### Step 10.3 — Leave the Budget alert running

Keep the AWS Budget from section 9 active even while stopped — the EBS volume and any kept Elastic IP still
accrue small charges, and the alert costs nothing to leave in place.

---

## 11. Resuming later — reconfiguring the IP

What you need to do next depends entirely on what you chose in section 10.2.

### If you kept the Elastic IP (Option A)

1. EC2 Console → **Instances** → select your instance → **Instance state → Start instance**.
2. Wait for **Running** + status checks to pass (same as step 5.1.9).
3. The same public IP comes back automatically — no Elastic IP work needed.
4. **Check the security group's SSH rule** — your home/mobile IP has likely changed since you last connected
   (same check as the troubleshooting entry in section 8.1: `https://checkip.amazonaws.com`, compare, update
   the port 22 rule if different).
5. SSH in and confirm the stack came back up on its own:
   ```bash
   cd ~/job-hunt
   docker compose ps
   ```
   `restart: unless-stopped` plus Docker starting on boot means everything should already be `Up` without
   you running `docker compose up` again. If something's missing, `docker compose up -d` brings it back
   without rebuilding (images are already on disk).
6. Browse to `http://<INSTANCE_IP>/` — same address as before, should just work.

### If you released the Elastic IP (Option B)

1. Start the instance (same as above) — it gets a **new**, different dynamic public IP automatically.
2. **EC2 → Network & Security → Elastic IPs → Allocate Elastic IP address → Allocate.**
3. Select it → **Actions → Associate Elastic IP address** → **Resource type: Instance** → pick your instance
   → **Associate**.
4. Note this new address — call it `<NEW_INSTANCE_IP>`; it will differ from whatever you used before.
5. **Update the security group's SSH rule** to your current IP, same check as above.
6. SSH in via the **new** IP:
   ```powershell
   ssh -i "C:\path\to\your-key.pem" ubuntu@<NEW_INSTANCE_IP>
   ```
7. **Update `CORS_ALLOWED_ORIGINS`** on the server to include the new IP (old entries can stay, they're
   harmless, or you can drop the previous IP if you want a clean list):
   ```bash
   echo 'CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173,http://<NEW_INSTANCE_IP>' >> ~/job-hunt/.env
   cd ~/job-hunt
   docker compose up -d api-gateway
   ```
8. Confirm everything else is already running (no rebuild needed, only the IP and CORS config changed):
   ```bash
   docker compose ps
   ```
9. Browse to `http://<NEW_INSTANCE_IP>/` and verify the login flow works, same as section 6.

If you expect to stop/start repeatedly, Option A (keeping the Elastic IP) avoids repeating steps 2–7 every
single time — worth weighing against the ~$3.60/month.

---

## 12. Optional next steps

- **Domain + HTTPS** — Caddy already fronts the app on port 80 (`frontend/Caddyfile`). Point a domain's A
  record at the Elastic IP, change the Caddyfile's `:80` to your domain name, add `https://yourdomain.com`
  to `CORS_ALLOWED_ORIGINS` (step 5.6), open port 443 in the security group, restart the `frontend`
  container. Caddy handles Let's Encrypt automatically. This also makes `crypto.randomUUID` work natively
  again (HTTPS is a secure context), though the fallback from section 8.2 means that's no longer required.
- **Move Postgres/Redis to managed services** — RDS (Postgres) and ElastiCache (Redis) offload
  backup/patching/failover from the single instance, at extra cost, and would let you drop the instance size
  since those two containers currently hold two of the largest `mem_limit` values.
- **Reserved Instance or Savings Plan** — once you know you're keeping this running long-term, this is the
  single highest-leverage cost cut available (section 1), with zero changes to anything in this guide.
- **Stop building on the instance entirely** — the project's GitHub Actions pipeline already builds and
  pushes backend images to `ghcr.io` on every push to `main`. Switching this instance to
  `docker compose pull && docker compose up -d` instead of `--build` removes all Maven compile load from the
  EC2 instance itself.
