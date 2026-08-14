# CI/CD: GitHub → VPS (Docker Compose)

Mỗi lần **push** `main`/`master` lên GitHub, Actions SSH vào VPS → `git pull` → `docker compose up -d --build`.

## Kiến trúc trên VPS

```
~/flourish/
├── docker-compose.yml
├── .env                 # secrets — không commit
├── BE/                  # clone repo BE (có .git)
└── FE/                  # clone repo FE (có .git)
```

Nginx + SSL giữ nguyên trên host (`flourishtravelapp.khanhtn45.id.vn`).

## 1. Chuẩn bị VPS (một lần)

### 1.1. SSH key riêng cho deploy

```bash
# trên VPS
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/github_actions_deploy -N ""
cat ~/.ssh/github_actions_deploy.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# In PRIVATE key (copy vào GitHub Secret) — chỉ hiện 1 lần khi setup
cat ~/.ssh/github_actions_deploy
```

### 1.2. Repo trên VPS phải là git clone

Nếu `BE`/`FE` chỉ copy file (không có remote), clone lại:

```bash
cd ~/flourish
# backup nếu cần
mv BE BE.bak 2>/dev/null || true
mv FE FE.bak 2>/dev/null || true

git clone https://github.com/<ORG_HOAC_USER>/<REPO_BE>.git BE
git clone https://github.com/<ORG_HOAC_USER>/<REPO_FE>.git FE

# Giữ docker-compose.yml + .env ở ~/flourish
```

Repo **private**: dùng [Deploy key](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/managing-deploy-keys) (read-only) hoặc HTTPS + PAT trên VPS.

```bash
# Ví dụ deploy key cho BE (trên VPS)
ssh-keygen -t ed25519 -f ~/.ssh/github_be_readonly -N ""
# Thêm pubkey vào GitHub repo BE → Settings → Deploy keys (Allow read)
```

`~/.ssh/config`:

```
Host github.com-be
  HostName github.com
  User git
  IdentityFile ~/.ssh/github_be_readonly
  IdentitiesOnly yes

Host github.com-fe
  HostName github.com
  User git
  IdentityFile ~/.ssh/github_fe_readonly
  IdentitiesOnly yes
```

Remote:

```bash
cd ~/flourish/BE && git remote set-url origin git@github.com-be:<ORG>/<REPO_BE>.git
cd ~/flourish/FE && git remote set-url origin git@github.com-fe:<ORG>/<REPO_FE>.git
```

Test:

```bash
cd ~/flourish/BE && git fetch origin && git status
cd ~/flourish/FE && git fetch origin && git status
```

## 2. GitHub Secrets (cả repo BE và FE)

Repo → **Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Ví dụ |
|--------|--------|
| `VPS_HOST` | `103.74.103.112` |
| `VPS_USER` | `root` |
| `VPS_SSH_KEY` | toàn bộ nội dung `~/.ssh/github_actions_deploy` (private key) |
| `VPS_PORT` | `22` (tuỳ chọn) |

Làm **giống nhau** trên repo BE và repo FE.

## 3. Workflow files

- BE: `.github/workflows/deploy-vps.yml`
- FE: `.github/workflows/deploy-vps.yml`

Commit + push `main` → tab **Actions** sẽ chạy job deploy.

## 4. Kiểm tra

```bash
# trên VPS sau khi Actions xanh
docker compose ps
curl -sI https://flourishtravelapp.khanhtn45.id.vn/
curl -sI https://flourishtravelapp.khanhtn45.id.vn/api/health
```

## 5. Lưu ý

- `.env` trên VPS **không** bị ghi đè bởi CI; chỉ pull code + rebuild image.
- FE build dùng `FE/website/.env` trên VPS (`VITE_API_URL=https://flourishtravelapp.khanhtn45.id.vn/api`).
- `SEED_ENABLED=false` giữ trong `docker-compose.yml` / `.env` VPS.
- Không commit private key / `.env` lên Git.
