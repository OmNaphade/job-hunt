# Deploying Job Portal to Oracle Cloud (Always Free) — Full Guide

A complete, from-zero walkthrough for deploying this project's `docker-compose.yml` stack to a genuinely
free, permanent Oracle Cloud Infrastructure (OCI) VM. Written so someone who has never touched OCI before
can follow it end to end.

**Written:** August 2026. OCI's Always Free terms have changed before and will change again — the
"Critical current constraints" section below has today's real numbers; verify them against
[oracle.com/cloud/free](https://www.oracle.com/cloud/free/) if you're reading this much later.

---

## Table of contents

1. [Critical current constraints — read this first](#1-critical-current-constraints--read-this-first)
2. [How OCI actually works (concepts)](#2-how-oci-actually-works-concepts)
3. [What this application needs from a VM](#3-what-this-application-needs-from-a-vm)
4. [Pre-flight checklist](#4-pre-flight-checklist)
5. [Step-by-step deployment](#5-step-by-step-deployment)
6. [Verification](#6-verification)
7. [Keeping it running forever](#7-keeping-it-running-forever)
8. [Troubleshooting — real issues you may hit](#8-troubleshooting--real-issues-you-may-hit)
9. [Optional next steps](#9-optional-next-steps)

---

## 1. Critical current constraints — read this first

Oracle silently halved its Always Free ARM allowance in June 2026. Starting **August 18, 2026**, Oracle
auto-terminates any Ampere A1 instance that exceeds the new limit. Most tutorials online still describe
the old numbers — ignore them.

| Resource | Old (pre-June 2026) | **Current (use this)** |
|---|---|---|
| Ampere A1 OCPUs | 4 | **2** |
| Ampere A1 memory | 24 GB | **12 GB** |
| Block storage | 200 GB total | 200 GB total (unchanged) |

Always provision exactly **2 OCPU / 12 GB** on shape `VM.Standard.A1.Flex`. The OCI console shows an
**"Always Free-eligible"** badge next to the shape when your sliders are within the current limit — treat
that badge as your live confirmation, not the number in any guide (including this one, eventually).

---

## 2. How OCI actually works (concepts)

If you've only used shared hosting or a PaaS like Render/Vercel before, OCI's building blocks need a
quick explanation — skipping this is why most people get stuck on networking.

- **Compute Instance** — the actual virtual machine. This is what runs your Docker containers.
- **Shape** — a template defining the instance's CPU/RAM. `VM.Standard.A1.Flex` (Ampere/ARM) is the
  Always-Free-eligible shape with enough resources for this stack; `VM.Standard.E2.1.Micro` (AMD) is
  Oracle's *other* free shape but only has 1GB RAM — not enough for this project.
- **Image** — the operating system installed on the instance (we're using Oracle Linux 9, aarch64 build).
- **VCN (Virtual Cloud Network)** — a private network your instance lives inside. Nothing reaches your
  instance from the internet unless the VCN is configured to allow it.
- **Subnet** — a subdivision of a VCN. A *public* subnet has a route to the internet via an Internet
  Gateway; a *private* subnet doesn't. Your instance needs to be in a public subnet to be reachable.
- **Public IP** — the address the outside world uses to reach your instance. Without one explicitly
  assigned, your instance is unreachable from anywhere but OCI's own internal network.
- **Security List (or NSG)** — OCI's cloud-level firewall. Even with a public IP, traffic to a port is
  blocked unless a Security List rule explicitly allows it.
- **The instance's own OS firewall** — a *second*, independent firewall running inside the VM itself
  (`firewalld` on Oracle Linux). Both this and the Security List must allow a port, or traffic is silently
  dropped with no error message telling you which layer blocked it.
- **SSH key pair** — how you authenticate into the instance. There's no password login by default. The
  private key is generated once and shown once — lose it and you lose access permanently (short of
  console-based recovery workarounds).
- **Boot volume** — the instance's disk (block storage). Default size (~50GB) comfortably fits inside the
  200GB Always Free block-storage allowance.

**The mental model:** Internet → Security List (cloud firewall) → instance's public IP → OS firewall
(`firewalld`) → the port your app is actually listening on. All four have to line up.

---

## 3. What this application needs from a VM

This is a **14-container** stack (`docker-compose.yml`): 7 Spring Boot services, Kafka, Zookeeper,
Postgres, Zipkin, Eureka (`service-registry`), `config-server`, and now a `frontend` service (React app
built and served by Caddy). Concretely, the VM needs:

- **Docker Engine + the Compose plugin** (not just `docker`, specifically `docker compose` v2).
- **~2 vCPU / 12GB RAM** — measured locally, the backend alone idles around 3.7GB RAM with the memory caps
  already set in `docker-compose.yml`; 12GB leaves real headroom, and the frontend container adds very
  little (a static file server, capped at 256MB). CPU is the tighter resource — 2 OCPUs is enough to run
  it, but the initial `docker compose up -d --build` (compiling 7 Maven projects + one npm build) will be
  noticeably slower than on a beefier dev machine. That's expected, not a failure.
  Kafka & Zookeeper images are natively multi-arch (confirmed ARM64 builds exist for the `7.6.0` tag this
  project pins), so no image changes are needed for ARM.
- **One open inbound port: 80** (the frontend/Caddy container — this is now the single public entry
  point). Caddy serves the built React app *and* reverse-proxies `/api/*` to `api-gateway:8080` internally
  over the Docker network, so the browser only ever talks to one origin. This sidesteps CORS entirely for
  the deployed app — there's no cross-origin request for the gateway's CORS filter to even need to allow.
  Port 8080 stays mapped for direct debugging from the VM itself, but doesn't need to be (and shouldn't be)
  opened to the public internet.
- **`git`** to pull the code, and a way to get your `.env` file onto the box (it's gitignored on purpose —
  never committed, so it has to be copied separately).

---

## 4. Pre-flight checklist

Before you start the OCI console flow, have these ready:

- [ ] An OCI account with the compute quota to create an Ampere A1 instance (you already have this).
- [ ] Know your local project path: `d:/PRACTICE/New folder` — you'll `scp` `.env` from here.
- [ ] A place to safely store the SSH private key you're about to download (e.g. `~/.ssh/oci_job_portal.key`
      or your Windows equivalent) — it is shown/downloadable exactly once.
- [ ] Confirm the repo is pushed to GitHub and up to date: `origin https://github.com/OmNaphade/job-hunt.git`.

---

## 5. Step-by-step deployment

### Step 5.1a — Create the network first (don't use the inline shortcut)

The instance-creation page offers an inline "Create new virtual cloud network" shortcut, but it's a
reduced-functionality path — it does **not** reliably let you enable a public IP, because it can't
guarantee an Internet Gateway is actually attached yet. Oracle's own console tells you this in a warning
box. Set the network up properly first, once, using the dedicated wizard:

1. Console → **Networking → Virtual Cloud Networks** → **Start VCN Wizard**.
2. Choose **"Create VCN with Internet Connectivity"** — *not* the bare "Create VCN" option. This preset
   creates everything correctly wired in one shot: a public subnet, a private subnet, an Internet Gateway,
   a NAT Gateway, and the route tables/security lists connecting them.
3. Name it (e.g. `job-portal-vcn`), leave every other field at its default, click **Create**.

Use:
- **"Create VCN with Internet Connectivity"** wizard preset ✅
- Defaults for CIDR blocks, subnets, route tables, security lists — don't hand-edit any of it.

Skip:
- The bare **"Create VCN"** option (no subnets/gateways — you'd have to wire it all up manually).
- The inline "Create new virtual cloud network" shortcut on the instance-creation page.

### Step 5.1b — Create the compute instance

1. OCI Console → **Compute → Instances → Create Instance**.
2. Name it (e.g. `job-portal-prod`).
3. **Image and shape** → confirm/select image **Oracle Linux 9** (aarch64 build — verify via the expand
   arrow next to the image name, should show under "Compatible shapes" once the Ampere shape is picked).
4. **Change shape** → **Ampere** tab → **VM.Standard.A1.Flex** → set **OCPU = 2, Memory = 12 GB**.
   Confirm the **"Always Free-eligible"** badge is showing.
5. **Security** step: leave both **Shielded instance** and **Confidential computing** toggles **off** —
   neither is needed for this deployment.
6. **Networking** step — use the VCN you just built, not the inline shortcut:
   - Primary network: **"Select existing virtual cloud network"** → pick `job-portal-vcn`.
   - Subnet: **"Select existing subnet"** → pick the one named `public subnet-job-portal-vcn` (the wizard
     names it after your VCN — pick the one that says **public**, not the private one).
   - **"Automatically assign public IPv4 address"** → turn this **on**. With a real public subnet selected,
     it should now toggle cleanly (often pre-checked by default). If it's still stuck, you picked the
     private subnet by mistake — go back and re-select the public one.
   - **Add SSH keys**: leave "Generate a key pair for me" selected, then click **Download private key**
     immediately and save it somewhere permanent. Click **Download public key** too, for your records.
7. **Storage** step: leave the boot volume at its default size. Don't increase it — larger volumes are
   what actually start costing real money outside the free allowance.
8. **Review**, confirm everything (2 OCPU/12GB, Oracle Linux 9, VCN = `job-portal-vcn`, public subnet,
   public IP on, SSH key attached), then **Create**.

> If creation fails with "Out of host capacity" — this is regional Ampere A1 demand, not a rejection of
> your account. Just retry every few minutes.

### Step 5.2 — Open the firewall (both layers)

**Layer 1 — OCI Security List** (cloud firewall):
Console → your instance → click the **Subnet** link → **Security Lists** → default security list →
**Add Ingress Rules**:
- Source CIDR: `0.0.0.0/0`, IP Protocol: TCP, Destination Port Range: `80`

Port 8080 does **not** need a public ingress rule — the frontend container on port 80 is the only public
entry point; it reaches `api-gateway:8080` internally over the Docker network, which OCI's Security List
has no say over.

**Layer 2 — the instance's own firewall** (do this after you SSH in, in Step 5.4):
```bash
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --reload
```

### Step 5.3 — Find the public IP and connect

Once the instance shows **Running**, copy its **Public IP Address** from the instance details page.

```bash
ssh -i /path/to/your/private_key opc@<YOUR_PUBLIC_IP>
```
(Oracle Linux images use the default user `opc`, not `ubuntu`.)

### Step 5.4 — Install Docker

Oracle Linux 9 is RHEL9-compatible, so we use Docker's official RHEL repository:

```bash
sudo dnf install -y dnf-utils
sudo dnf config-manager --add-repo https://download.docker.com/linux/rhel/docker-ce.repo
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
newgrp docker
docker compose version   # sanity check — should print a version, not "command not found"
```

Now apply the OS-level firewall rule from Step 5.2:
```bash
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --reload
```

### Step 5.5 — Get the code and your secrets onto the VM

On the VM:
```bash
git clone https://github.com/OmNaphade/job-hunt.git
cd job-hunt
```

From your **local** machine (not the VM) — `.env` is gitignored on purpose and must be copied separately:
```bash
scp -i /path/to/your/private_key "d:/PRACTICE/New folder/.env" opc@<YOUR_PUBLIC_IP>:~/job-hunt/.env
```

### Step 5.6 — Build and launch

```bash
cd ~/job-hunt
docker compose up -d --build
watch docker compose ps
```

Give the first build **20-40 minutes** — 2 OCPUs compiling 7 Spring Boot services from source is
noticeably slower than a typical dev machine. This is expected.

---

## 6. Verification

From your **local** machine, open **`http://<YOUR_PUBLIC_IP>/`** in a browser — you should get the actual
job portal UI, not just a JSON response. That's the real end-to-end check: it means the frontend built
correctly, Caddy is serving it, and the SPA can reach the API through the same-origin proxy.

Then confirm the API path specifically:
```bash
curl -i http://<YOUR_PUBLIC_IP>/api/jobs   # expect 401 (auth required) — same as local
```

On the VM, confirm every container is actually healthy, not just running:
```bash
docker compose ps -a
docker logs job-service --tail 50   # spot-check one service's logs for startup errors
docker logs frontend --tail 30      # confirm Caddy started and isn't erroring on the Caddyfile
```

---

## 7. Keeping it running forever

- **`restart: unless-stopped` is already set on all 13 services** in `docker-compose.yml` — a VM reboot or
  a transient container crash self-heals without you needing to SSH back in.
- **Docker itself starts on boot** (`systemctl enable docker`, done in Step 5.4) — so a full VM reboot
  (e.g. after an OS update) brings the whole stack back up automatically.
- **Don't let Oracle reclaim the account.** Always Free resources can be reclaimed after extended account
  inactivity. Log into the OCI console occasionally (once a month is enough) even if you never touch the
  VM directly.
- **Watch your Cost Analysis** (Console → Billing → Cost Analysis) once or twice in the first week to
  confirm actual charges are $0, not just the estimator's placeholder numbers.
- **Watch resource usage** the same way we did locally:
  ```bash
  docker stats --no-stream
  ```
  If Kafka's memory percentage creeps toward its cap under real usage, its `mem_limit` in
  `docker-compose.yml` (currently `512m`) is the first thing to raise — it was already the tightest margin
  in local testing (~85% of its cap at idle).

---

## 8. Troubleshooting — real issues you may hit

These are documented from issues actually encountered testing this exact stack, not hypothetical ones.

**Kafka container exits immediately with `NodeExistsException` / `KeeperErrorCode = NodeExists`**
Zookeeper's Docker image declares anonymous volumes (`/var/lib/zookeeper/data`, `/var/lib/zookeeper/log`)
that are *not* listed in `docker-compose.yml`. These silently survive a plain container recreate, so stale
broker-registration state can persist even after you think you've done a clean restart. Fix:
```bash
docker compose rm -f -s -v kafka zookeeper   # -v removes the anonymous volumes too
docker compose up -d
```

**A service logs `violates check constraint "jobs_source_check"` when importing external jobs**
Hibernate's `ddl-auto=update` generates a Postgres CHECK constraint from the `JobSource` enum once, when
the table is first created — it never widens that constraint as new enum values get added later. If this
happens on a database that already existed before a code change added new `JobSource` values, fix it
directly:
```bash
docker exec jobportal-postgres psql -U postgres -d jobapp_db -c "
ALTER TABLE jobs DROP CONSTRAINT jobs_source_check;
ALTER TABLE jobs ADD CONSTRAINT jobs_source_check CHECK (source IN (
  'RECRUITER','ADZUNA','HIMALAYAS','ARBEITNOW',
  'AI_DEV_JOBS','ARTIFICIAL_INTELLIGENCE_JOBS','FREEHIRE','FINDWORK','JOBDATALAKE'
));
"
```
This only needs to happen once per pre-existing database — a genuinely fresh database created after the
code change already has the correct constraint from the start.

**A container can't be stopped: `PID ... is zombie and can not be killed`**
Rare, and generally a Docker Desktop/host quirk rather than an application bug. Force it:
```bash
docker kill <container>
docker rm -f <container>
docker compose up -d
```

**"Out of host capacity" when creating the instance**
Not a rejection — Ampere A1 free-tier demand in your region is high. Retry every few minutes.

**Everything looks "Running" but nothing is reachable from outside**
99% of the time this is one of: (a) "Assign public IPv4 address" wasn't turned on, (b) the OCI Security
List rule for port 8080 wasn't added, or (c) the instance's own `firewalld` wasn't opened. Check all three
— curl from your local machine, then SSH in and `curl localhost:8080` from inside the VM to narrow down
which layer is blocking.

---

## 9. Optional next steps

- **Domain + HTTPS** — Caddy is already fronting the app on port 80 (`frontend/Caddyfile`), so getting free
  automatic HTTPS is now just: point a domain's A record at your public IP, change the Caddyfile's `:80` to
  your domain name (e.g. `jobportal.example.com {`), open port 443 at both firewall layers the same way you
  opened port 80, and restart the `frontend` container. Caddy handles the Let's Encrypt certificate
  provisioning and renewal automatically — no certbot, no manual renewal cron job.
- **Stop building on the VM entirely** — the project's GitHub Actions pipeline already builds and pushes
  backend images to `ghcr.io` on every push to `main`. A follow-up optimization is a compose variant that
  does `docker compose pull && docker compose up -d` on the VM instead of `--build` — much faster deploys,
  and zero compile load on the free-tier CPUs. The frontend would need a similar step added to that
  pipeline first, since it isn't currently part of the GHCR image build.
