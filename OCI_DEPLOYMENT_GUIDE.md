# Deploying Job Portal to Oracle Cloud (Always Free) — Full Guide

A complete, from-zero walkthrough for deploying this project's stack to Oracle Cloud Infrastructure (OCI)
across **three genuinely free VMs** instead of one — spreading the app across both of OCI's separate
Always Free compute pools (the ARM allowance and the two fixed AMD Micro instances) for more real headroom
than a single VM gives you, while also taking the entire backend off the public internet. Written so
someone who has never touched OCI before can follow it end to end.

**Written:** August 2026. OCI's Always Free terms have changed before and will change again — the
"Critical current constraints" section below has today's real numbers, verified against Oracle's own
Always Free resource reference (docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm)
— check that page directly if you're reading this much later.

---

## Table of contents

1. [Critical current constraints — read this first](#1-critical-current-constraints--read-this-first)
2. [How OCI actually works (concepts)](#2-how-oci-actually-works-concepts)
3. [What this application needs, per VM](#3-what-this-application-needs-per-vm)
4. [Pre-flight checklist](#4-pre-flight-checklist)
5. [Step-by-step deployment](#5-step-by-step-deployment)
6. [Verification](#6-verification)
7. [Keeping it running forever](#7-keeping-it-running-forever)
8. [Troubleshooting — real issues you may hit](#8-troubleshooting--real-issues-you-may-hit)
9. [Cost safety net — automatic shutdown if you ever exceed free](#9-cost-safety-net--automatic-shutdown-if-you-ever-exceed-free)
10. [Optional next steps](#10-optional-next-steps)

---

## 1. Critical current constraints — read this first

Oracle silently halved its Always Free ARM allowance in June 2026. Starting **August 18, 2026**, Oracle
auto-terminates any Ampere A1 instance that exceeds the new limit. Most tutorials online still describe
the old numbers — ignore them. The numbers below are current, verified against Oracle's Always Free
resource reference:

| Resource | Allowance |
|---|---|
| `VM.Standard.A1.Flex` (Ampere/ARM) | 1,500 OCPU-hours + 9,000 GB-hours/month ≈ **2 OCPU / 12 GB sustained**, splittable across up to 4 instances |
| `VM.Standard.E2.1.Micro` (AMD) | **Exactly 2 instances**, fixed 1/8 OCPU + 1 GB RAM each — a separate pool, not hour-metered, unaffected by the ARM cut above |
| VCNs | Up to 2 per tenancy |
| Block storage | 200 GB total (boot + block volumes combined), separate from compute metering, unconditional (doesn't matter if instances are running or stopped) |

This guide uses **both pools together**: one ARM instance at exactly 2 OCPU / 12 GB, plus both AMD Micro
instances. The OCI console shows an **"Always Free-eligible"** badge next to a shape when your sliders are
within the current limit — treat that badge as your live confirmation, not the numbers in any guide
(including this one, eventually).

---

## 2. How OCI actually works (concepts)

If you've only used shared hosting or a PaaS like Render/Vercel before, OCI's building blocks need a quick
explanation — skipping this is why most people get stuck on networking. This section also covers the
extra concepts this 3-VM layout introduces beyond a single-instance deployment.

- **Compute Instance** — the actual virtual machine. This is what runs your Docker containers.
- **Shape** — a template defining the instance's CPU/RAM. `VM.Standard.A1.Flex` (Ampere/ARM) is one
  Always-Free-eligible shape; `VM.Standard.E2.1.Micro` (AMD) is the other. They draw from **separate**
  free allowances (see section 1) — using both is not "splitting" the same budget.
- **Image** — the operating system installed on the instance. This guide uses **Ubuntu Server 24.04**
  (Always-Free-eligible on both shapes — the OS choice doesn't affect free eligibility, only the shape
  does).
- **VCN (Virtual Cloud Network)** — a private network your instances live inside. Nothing reaches an
  instance from the internet unless the VCN is configured to allow it.
- **Subnet** — a subdivision of a VCN. A *public* subnet has a route to the internet via an Internet
  Gateway; a *private* subnet doesn't (it reaches the internet only outbound, via a NAT Gateway, for things
  like `apt update` or `git clone`). **This layout uses both**: the internet-facing VM goes in the public
  subnet, everything else goes in the private subnet and is unreachable from outside the VCN.
- **Public IP** — the address the outside world uses to reach an instance. Only the public-subnet instance
  gets one; the private-subnet instances have none at all.
- **VCN-internal DNS** — instances in the same VCN can resolve each other by display name
  (`job-portal-core.<subnet>.<vcn>.oraclevcn.com`) without needing a hardcoded private IP, as long as "Use
  DNS hostnames in this VCN" is enabled (on by default when you use the VCN wizard in step 5.1).
- **Security List (or NSG)** — OCI's cloud-level firewall, one per subnet. Even with a public IP, traffic
  to a port is blocked unless a rule explicitly allows it. With two subnets, you now configure **two
  different rule sets** — the public subnet's rules are internet-facing, the private subnet's rules only
  need to trust traffic *from the public subnet's CIDR block*, not the whole internet.
- **The instance's own OS firewall** — a *second*, independent firewall running inside the VM itself. On
  Ubuntu cloud images this is typically `ufw`, and it's usually inactive by default (unlike Oracle Linux's
  `firewalld`, which is active by default) — worth explicitly checking with `sudo ufw status` rather than
  assuming either way.
- **SSH key pair** — how you authenticate into an instance. There's no password login by default. The
  private key is generated once and shown once — lose it and you lose access permanently (short of
  console-based recovery workarounds). This guide reuses **one** keypair across all three instances for
  simplicity.
- **SSH jump host (`ProxyJump`)** — since `job-portal-core` and `job-portal-aux` have no public IP, you
  can't SSH into them directly from your laptop. You SSH into the public instance first, and it forwards
  the connection onward — one command, no extra OCI service required (see step 5.3).
- **Boot volume** — an instance's disk (block storage). Default size (~50 GB) is fine for `job-portal-core`;
  the two AMD instances can use a smaller custom size since they run one lightweight container each. All
  volumes across all three instances must stay under the shared 200 GB Always Free cap (see section 1).

**The mental model, per instance:** Internet → (public subnet only) Security List → instance's public IP →
OS firewall → the port the app is listening on. Private-subnet instances skip the first two hops entirely
— they're simply not reachable from the internet, full stop.

---

## 3. What this application needs, per VM

This is a **14-container** stack (`docker-compose.yml`, now split into `docker-compose.core.yml` and
`docker-compose.edge.yml`): 7 Spring Boot services, Kafka, Zookeeper, Postgres, Zipkin, Eureka
(`service-registry`), `config-server`, and a `frontend` service (React app built and served by Caddy).

### `job-portal-core` (ARM, `VM.Standard.A1.Flex`, 2 OCPU / 12 GB, **private subnet**)

Everything except `frontend`: Postgres, Zookeeper, Kafka, Zipkin, `service-registry`, `config-server`,
`api-gateway`, and all 6 domain services (auth/user/job/company/application/notification).

- **Docker Engine + Compose plugin v2.**
- **~2 vCPU / 12GB RAM** — measured locally, this subset idles around 3.4GB RAM with the memory caps
  already set in `docker-compose.core.yml`; 12GB leaves real headroom (more than before, since `frontend`'s
  small footprint moved off this host entirely). CPU is the tighter resource — the initial
  `docker compose -f docker-compose.core.yml up -d --build` (compiling 7 Maven projects) will be noticeably
  slower than on a beefier dev machine. That's expected, not a failure. Kafka & Zookeeper images are
  natively multi-arch (confirmed ARM64 builds exist for the `7.6.0` tag this project pins), so no image
  changes are needed for ARM.
- **One inbound port, scoped to the VCN only: 8080** (`api-gateway`) — allowed from the public subnet's
  CIDR block, never from `0.0.0.0/0`. This host has no public IP, so this rule is defense-in-depth on top
  of that, not the only thing standing between it and the internet.
- **`git`**, and a way to get your `.env` file onto the box (it's gitignored on purpose — copy it
  separately via `scp` through the SSH jump host, see step 5.5).

### `job-portal-edge` (AMD, `VM.Standard.E2.1.Micro`, 1/8 OCPU / 1 GB, **public subnet**)

Just `frontend` (Caddy serving the built React app).

- Same Docker Engine + Compose plugin requirement, but the image is tiny — `frontend`'s `mem_limit` is
  256m, comfortably inside 1GB with OS overhead included.
- **One open inbound port: 80** (add 443 later if you set up a domain + HTTPS, see section 10). This is
  now the **only** publicly reachable host in the whole deployment. Caddy serves the built React app *and*
  reverse-proxies `/api/*` to `job-portal-core`'s internal DNS name (`CORE_UPSTREAM` in
  `docker-compose.edge.yml`) — the browser only ever talks to this one origin, so there's no cross-origin
  request for CORS to apply to.
- 1/8 OCPU is heavily oversubscribed/burstable — fine for a static file server + reverse proxy, not
  something you'd run a JVM process on.

### `job-portal-aux` (AMD, `VM.Standard.E2.1.Micro`, 1/8 OCPU / 1 GB, **private subnet**, optional)

Not required. A second free AMD instance you can leave unprovisioned, or use later for something genuinely
lightweight and standalone (this guide doesn't assign it a role — Zipkin already runs on `job-portal-core`
since it's part of the core stack's tracing, and 1GB isn't enough headroom to be worth relocating it just
for the sake of using this instance).

---

## 4. Pre-flight checklist

Before you start the OCI console flow, have these ready:

- [ ] An OCI account with compute quota for one Ampere A1 instance and two AMD Micro instances (you
      already have this under Always Free).
- [ ] Know your local project path: `d:/PRACTICE/New folder` — you'll `scp` `.env` from here, through the
      jump host, to `job-portal-core`.
- [ ] A place to safely store the SSH private key you're about to download (e.g.
      `~/.ssh/oci_job_portal.key` or your Windows equivalent) — it is shown/downloadable exactly once.
      This guide reuses one keypair for all three instances.
- [ ] Confirm the repo is pushed to GitHub and up to date: `origin https://github.com/OmNaphade/job-hunt.git`.
- [ ] Decide now whether you're provisioning `job-portal-aux` or leaving it for later — it doesn't block
      anything either way.

---

## 5. Step-by-step deployment

### Step 5.1 — Create the network first (don't use the inline shortcut)

The instance-creation page offers an inline "Create new virtual cloud network" shortcut, but it's a
reduced-functionality path — it does **not** reliably let you enable a public IP, because it can't
guarantee an Internet Gateway is actually attached yet. Oracle's own console tells you this in a warning
box. Set the network up properly first, once, using the dedicated wizard:

1. Console → **Networking → Virtual Cloud Networks** → **Start VCN Wizard**.
2. Choose **"Create VCN with Internet Connectivity"** — *not* the bare "Create VCN" option. This preset
   creates everything correctly wired in one shot: a **public subnet**, a **private subnet**, an Internet
   Gateway, a NAT Gateway, and the route tables/security lists connecting them.
3. Name it (e.g. `job-portal-vcn`), leave every other field at its default (including "Use DNS hostnames in
   this VCN", which should already be checked — this is what makes VCN-internal DNS names resolve later),
   click **Create**.

This one VCN, with its two subnets, is all you need — don't create a second VCN. `job-portal-edge` goes in
the **public** subnet; `job-portal-core` and `job-portal-aux` go in the **private** subnet.

### Step 5.2 — Create `job-portal-core` (ARM, private subnet)

1. OCI Console → **Compute → Instances → Create Instance**.
2. Name it `job-portal-core`.
3. **Image and shape** → **Change image** → **Ubuntu** → **Ubuntu 24.04** (confirm the aarch64/ARM build is
   selected — it should be automatic once the Ampere shape is picked below).
4. **Change shape** → **Ampere** tab → **VM.Standard.A1.Flex** → set **OCPU = 2, Memory = 12 GB**. Confirm
   the **"Always Free-eligible"** badge is showing.
5. **Security** step: leave **Shielded instance** and **Confidential computing** both **off**.
6. **Networking** step:
   - Primary network: **"Select existing virtual cloud network"** → `job-portal-vcn`.
   - Subnet: **"Select existing subnet"** → the one named **private subnet-job-portal-vcn**. This is the
     key difference from a single-VM setup — pick **private**, not public.
   - **"Automatically assign public IPv4 address"** → this option should be greyed out/unavailable once a
     private subnet is selected. That's correct — this host must not get a public IP.
   - **Add SSH keys**: "Generate a key pair for me" → **Download private key** immediately, save it
     permanently. You'll reuse this same key for the other two instances.
7. **Storage** step: leave the boot volume at its default size (~50GB).
8. **Review**, confirm (2 OCPU/12GB, Ubuntu 24.04, VCN = `job-portal-vcn`, **private** subnet, no public
   IP, SSH key attached), then **Create**.

> If creation fails with "Out of host capacity" — this is regional Ampere A1 demand, not a rejection of
> your account. Retry every few minutes.

### Step 5.3 — Create `job-portal-edge` (AMD, public subnet)

1. **Create Instance** again. Name it `job-portal-edge`.
2. **Image and shape** → **Ubuntu 24.04** (this time the standard x86_64 build, since AMD).
3. **Change shape** → **Specialty and previous generation** → **VM.Standard.E2.1.Micro**. This shape is
   fixed-size — no OCPU/memory sliders. Confirm the **"Always Free-eligible"** badge.
4. **Networking** step:
   - VCN: `job-portal-vcn`.
   - Subnet: **public subnet-job-portal-vcn**.
   - **"Automatically assign public IPv4 address"** → turn this **on**.
   - **SSH keys**: reuse the same key downloaded in step 5.2 — **"Upload public key file"** or paste the
     public key contents (the `.pub` file downloaded alongside the private key).
5. **Storage**: default is fine, or shrink it (this instance runs one small container) — keep in mind the
   shared 200GB cap across all three instances if you also provision `job-portal-aux` at default size.
6. **Review** and **Create**.

### Step 5.4 — Create `job-portal-aux` (AMD, private subnet, optional)

Same as step 5.3, except: name it `job-portal-aux`, and pick the **private subnet-job-portal-vcn**
(no public IP option will be available, matching `job-portal-core`). Skip this step entirely if you're not
using the third instance yet — nothing else in this guide depends on it existing.

### Step 5.5 — Open the firewalls (two different rule sets)

**Public subnet's Security List** (attached to `job-portal-edge`'s subnet):
Console → **Networking → Virtual Cloud Networks** → `job-portal-vcn` → **public subnet-job-portal-vcn** →
**Security Lists** → default security list → **Add Ingress Rules**:
- Source CIDR: `0.0.0.0/0`, IP Protocol: TCP, Destination Port Range: `80`
- Source CIDR: **your own IP** (not `0.0.0.0/0`) `/32`, IP Protocol: TCP, Destination Port Range: `22` —
  restricting SSH to your own address, not the whole internet, closes off a real attack surface the
  original single-VM version of this guide left open.

**Private subnet's Security List** (attached to `job-portal-core`'s and `job-portal-aux`'s subnet):
Same navigation, but on **private subnet-job-portal-vcn** → **Add Ingress Rules**:
- Source CIDR: the **public subnet's CIDR block** (shown on the public subnet's details page, typically
  something like `10.0.0.0/24`) — not `0.0.0.0/0`. IP Protocol: TCP, Destination Port Range: `8080`.
- Source CIDR: same public subnet CIDR, IP Protocol: TCP, Destination Port Range: `22` — so the SSH jump
  host (step 5.6) can reach in.

**Each instance's own OS firewall** (do this after you SSH in, step 5.7):
```bash
sudo ufw status   # check first — Ubuntu cloud images often have no active firewall by default
# only if ufw is active, open the port this instance actually needs:
sudo ufw allow 80/tcp    # on job-portal-edge
sudo ufw allow 8080/tcp  # on job-portal-core
```

### Step 5.6 — Find IPs and connect via the SSH jump host

Once `job-portal-edge` shows **Running**, copy its **Public IP Address** from the instance details page.
For `job-portal-core`, copy its **Private IP Address** instead (it has no public one).

```bash
# Direct connection to the edge instance:
ssh -i /path/to/your/private_key ubuntu@<EDGE_PUBLIC_IP>

# Jump through edge to reach core (no public IP needed for core):
ssh -i /path/to/your/private_key -J ubuntu@<EDGE_PUBLIC_IP> ubuntu@<CORE_PRIVATE_IP>
```
(Ubuntu images use the default user `ubuntu`, not `opc` as on Oracle Linux.)

### Step 5.7 — Install Docker (Ubuntu, on each instance you're using)

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
```

Then apply the OS-level firewall rule from step 5.5 if `ufw` is active.

### Step 5.8 — Get the code onto each host

On **both** `job-portal-core` and `job-portal-edge` (and `job-portal-aux` if you're using it):
```bash
git clone https://github.com/OmNaphade/job-hunt.git
cd job-hunt
```

From your **local** machine — `.env` is gitignored on purpose and must be copied separately, through the
jump host, to `job-portal-core` only (that's where the services that read it live):
```bash
scp -i /path/to/your/private_key -o "ProxyJump=ubuntu@<EDGE_PUBLIC_IP>" \
  "d:/PRACTICE/New folder/.env" ubuntu@<CORE_PRIVATE_IP>:~/job-hunt/.env
```

### Step 5.9 — Build and launch, per host

On `job-portal-core`:
```bash
cd ~/job-hunt
docker compose -f docker-compose.core.yml up -d --build
watch docker compose -f docker-compose.core.yml ps
```
Give the first build **20-40 minutes** — 2 OCPUs compiling 7 Spring Boot services from source is
noticeably slower than a typical dev machine. This is expected.

On `job-portal-edge`, once `job-portal-core`'s `api-gateway` is healthy:
```bash
cd ~/job-hunt
CORE_UPSTREAM="job-portal-core.<private-subnet-name>.job-portal-vcn.oraclevcn.com:8080" \
  docker compose -f docker-compose.edge.yml up -d --build
```
Replace the `CORE_UPSTREAM` value with `job-portal-core`'s actual VCN-internal DNS name — visible on its
instance details page under "Fully qualified domain name" (FQDN). To make this permanent rather than
typed on every restart, add it to a `.env` file next to `docker-compose.edge.yml` on `job-portal-edge`
instead of exporting it inline.

---

## 6. Verification

From your **local** machine, open **`http://<EDGE_PUBLIC_IP>/`** in a browser — you should get the actual
job portal UI, not just a JSON response. That's the real end-to-end check: it means the frontend built
correctly, Caddy is serving it, and the SPA can reach the API through the cross-host proxy.

Then confirm the API path specifically:
```bash
curl -i http://<EDGE_PUBLIC_IP>/api/jobs   # expect 401 (auth required) — same as before
```

From **inside** `job-portal-edge`, confirm the cross-host hop directly before trusting the public path:
```bash
curl -i http://job-portal-core.<private-subnet-name>.job-portal-vcn.oraclevcn.com:8080/actuator/health
```

**Confirm the private subnet is actually private** — from your local machine (outside the VCN):
```bash
curl -i --max-time 5 http://<CORE_PRIVATE_IP>:8080/actuator/health   # should time out, not connect
```
If this succeeds, something is misconfigured — `job-portal-core` should be unreachable from outside the VCN
entirely.

On `job-portal-core`, confirm every container is actually healthy, not just running:
```bash
docker compose -f docker-compose.core.yml ps -a
docker logs job-service --tail 50   # spot-check one service's logs for startup errors
```
On `job-portal-edge`:
```bash
docker compose -f docker-compose.edge.yml ps -a
docker logs frontend --tail 30      # confirm Caddy started and isn't erroring on the Caddyfile
```

---

## 7. Keeping it running forever

- **`restart: unless-stopped` is set on every service** in both `docker-compose.core.yml` and
  `docker-compose.edge.yml` — a VM reboot or a transient container crash self-heals without you needing to
  SSH back in.
- **Docker itself starts on boot** (enabled during install in step 5.7) — a full VM reboot brings each
  host's stack back up automatically.
- **Don't let Oracle reclaim the account.** Always Free resources can be reclaimed after extended account
  inactivity. Log into the OCI console occasionally (once a month is enough) — this one login covers the
  whole tenancy, not per-instance.
- **Watch your Cost Analysis** (Console → Billing → Cost Analysis) once or twice in the first week to
  confirm actual charges are $0. Section 9 below sets up something stronger than watching manually.
- **Watch resource usage** the same way locally, per host:
  ```bash
  docker stats --no-stream
  ```
  If Kafka's memory percentage creeps toward its cap under real usage, its `mem_limit` in
  `docker-compose.core.yml` (currently `512m`) is the first thing to raise — it was already the tightest
  margin in local testing.

---

## 8. Troubleshooting — real issues you may hit

These are documented from issues actually encountered testing this stack, not hypothetical ones.

**Kafka container exits immediately with `NodeExistsException` / `KeeperErrorCode = NodeExists`**
Zookeeper's Docker image declares anonymous volumes (`/var/lib/zookeeper/data`, `/var/lib/zookeeper/log`)
that are *not* listed in `docker-compose.core.yml`. These silently survive a plain container recreate, so
stale broker-registration state can persist even after you think you've done a clean restart. Fix:
```bash
docker compose -f docker-compose.core.yml rm -f -s -v kafka zookeeper   # -v removes the anonymous volumes too
docker compose -f docker-compose.core.yml up -d
```

**A service logs `violates check constraint "jobs_source_check"` when importing external jobs**
Hibernate's `ddl-auto=update` generates a Postgres CHECK constraint from the `JobSource` enum once, when
the table is first created — it never widens that constraint as new enum values get added later. If this
happens on a database that already existed before a code change added new `JobSource` values, fix it
directly on `job-portal-core`:
```bash
docker exec jobportal-postgres psql -U postgres -d jobapp_db -c "
ALTER TABLE jobs DROP CONSTRAINT jobs_source_check;
ALTER TABLE jobs ADD CONSTRAINT jobs_source_check CHECK (source IN (
  'RECRUITER','ADZUNA','HIMALAYAS','ARBEITNOW',
  'AI_DEV_JOBS','ARTIFICIAL_INTELLIGENCE_JOBS','FREEHIRE','FINDWORK','JOBDATALAKE'
));
"
```
This only needs to happen once per pre-existing database.

**"Out of host capacity" when creating `job-portal-core`**
Not a rejection — Ampere A1 free-tier demand in your region is high. Retry every few minutes.

**Frontend loads but every API call fails / spins forever**
99% of the time this is the cross-host hop, not the app. In order: (a) confirm `CORE_UPSTREAM` on
`job-portal-edge` is set to `job-portal-core`'s actual FQDN (typos here fail silently — Caddy just can't
resolve the name), (b) confirm the private subnet's Security List allows port 8080 **from the public
subnet's CIDR**, (c) SSH into `job-portal-core` directly and `curl localhost:8080/actuator/health` to rule
out the app itself, (d) from `job-portal-edge`, `curl` the core FQDN on 8080 to isolate the cross-host hop
specifically.

**SSH jump host fails with `channel 0: open failed`**
Usually means the private subnet's Security List doesn't allow port 22 from the public subnet's CIDR yet
— add that rule (step 5.5) if it's missing.

**Everything looks "Running" but `job-portal-edge` itself is unreachable**
Check: (a) "Assign public IPv4 address" was turned on when creating it, (b) the public subnet's Security
List allows port 80 from `0.0.0.0/0`, (c) `ufw` on the instance itself, if active, allows 80.

---

## 9. Cost safety net — automatic shutdown if you ever exceed free

OCI Budgets can only **email an alert** — unlike AWS Budgets Actions or GCP's billing-disable pattern,
nothing on OCI stops automatically when you cross a spending threshold. This section closes that gap with
two layers: a native OCI alert (early warning), and a self-built killswitch (actual enforcement).

### 9.1 — Notification Topic + email alert (Console, one-time)

1. Console → **Developer Services → Notifications → Topics → Create Topic**. Name it `job-portal-alerts`.
2. Open the topic → **Create Subscription** → Protocol: **Email** → enter
   `pariharharish723@gmail.com` → **Create**. Confirm the subscription via the email Oracle sends —
   it won't deliver anything until you do.
3. Copy the topic's **OCID** — you'll need it for both the Budget alert rule below and the
   `OCI_NOTIFICATION_TOPIC_ID` GitHub secret in 9.3.

### 9.2 — Budget + Alert Rule (Console, one-time — early warning only)

1. Console → **Billing → Budgets → Create Budget**. Set the amount to **$1** (not $0 — Always Free usage
   can show tiny non-zero forecast noise even at genuinely $0 actual cost; $1 is a real trip-wire, not
   noise).
2. **Add Alert Rule**: threshold 50% and 100%, based on **actual spend**. Set the alert's **message** field
   to exactly: `resources exceed free limit`. Destination: the `job-portal-alerts` topic from 9.1.
3. This fires on Oracle's own internal schedule (cost data can lag actual spend by hours) — treat it as an
   early warning, not the thing that stops anything. That's what 9.3 does.

### 9.3 — The killswitch itself (already in this repo)

- **`scripts/oci-cost-killswitch.sh`** — queries OCI's Usage API for month-to-date cost. If it exceeds
  `OCI_COST_LIMIT_USD`, it **stops** (not terminates — boot volumes stay intact under the separate,
  unconditional 200GB free storage allowance, so nothing is lost) every instance named `job-portal-core`,
  `job-portal-edge`, and `job-portal-aux` (skipping any that don't exist), then publishes
  `"resources exceed free limit"` to the Notification Topic from 9.1 — the same message, so whichever
  channel reaches you first says the same thing.
- **`.github/workflows/oci-cost-killswitch.yml`** — runs this script every 15 minutes via a scheduled
  GitHub Action, reusing the same OCI auth secrets already set up for `oci-provision-retry.yml`
  (`OCI_USER_OCID`, `OCI_FINGERPRINT`, `OCI_PRIVATE_KEY`, `OCI_TENANCY_OCID`, `OCI_REGION`), plus two new
  ones you need to add:

| Secret / variable | Value |
|---|---|
| `OCI_NOTIFICATION_TOPIC_ID` (secret) | The topic OCID from 9.1 |
| `OCI_COST_LIMIT_USD` (repo variable, not secret — it's not sensitive) | e.g. `1.00`, matching the Budget amount in 9.2 |

- On trip, the workflow also opens a GitHub issue and disables itself (matching
  `oci-provision-retry.yml`'s existing pattern), so you get an in-repo record and it doesn't keep
  re-triggering against an already-stopped deployment.

### 9.4 — What this does and doesn't cover

- **It's a backstop, not a guarantee.** Usage/cost data lags actual spend by hours on every cloud provider.
  Some small overage before it fires is theoretically possible.
- **It covers compute, not every resource type.** Stopping instances addresses OCPU/GB-hour cost. It does
  not protect against exceeding the 200GB storage cap, the 2-VCN cap, or provisioning an unrelated paid
  resource (e.g. a Load Balancer beyond the 1 free one) — none of those can be "stopped" the same way, so
  they'd need their own separate Budget alert rules if you're worried about them specifically.
- **The watcher is itself a new failure point.** If the scheduled Action stops running (secret expiry,
  GitHub outage, Actions disabled on the repo), there's silent zero protection despite believing there is.
  Confirm GitHub's default "email me on workflow failure" is enabled and reaching the right inbox.
- **Test it safely before trusting it**: run `scripts/oci-cost-killswitch.sh` locally with
  `OCI_COST_LIMIT_USD=0.00 DRY_RUN=true` — it should detect "over budget" immediately (since any real usage
  is >$0) and log what it *would* stop and notify, without actually doing either.

---

## 10. Optional next steps

- **Domain + HTTPS** — Caddy is already fronting the app on port 80 (`frontend/Caddyfile`), so getting free
  automatic HTTPS is now just: point a domain's A record at `job-portal-edge`'s public IP, change the
  Caddyfile's `:80` to your domain name (e.g. `jobportal.example.com {`), open port 443 at both firewall
  layers on the **public subnet only** (`job-portal-core` never needs 443), and restart the `frontend`
  container. Caddy handles Let's Encrypt certificate provisioning and renewal automatically.
- **Stop building on the VMs entirely** — the project's GitHub Actions pipeline already builds and pushes
  backend images to `ghcr.io` on every push to `main`. A follow-up optimization is compose variants that do
  `docker compose pull && docker compose up -d` on each host instead of `--build` — much faster deploys,
  and zero compile load on the free-tier CPUs. The frontend would need a similar step added to that
  pipeline first, since it isn't currently part of the GHCR image build.
- **Use `job-portal-aux`** — if you provisioned it in step 5.4, it's sitting idle. A candidate: a status
  page or lightweight health-check dashboard, kept off `job-portal-core` so it can't be affected by the
  core stack's resource usage.
