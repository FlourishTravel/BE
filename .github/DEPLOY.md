# Deploy Backend lên môi trường chạy

GitHub **không chạy được** ứng dụng Java/Spring Boot. Bạn cần deploy BE lên một dịch vụ bên ngoài. Dưới đây là 2 cách phổ biến (free tier).

---

## 1. Railway (đơn giản, khuyến nghị)

1. Vào [railway.app](https://railway.app) → đăng nhập bằng **GitHub**.
2. **New Project** → **Deploy from GitHub repo** → chọn repo **BE** (FlourishTravel/BE).
3. Railway tự nhận Spring Boot, build Maven và chạy.
4. Trong **Variables** thêm biến môi trường (giống `.env`). Xem bảng dưới.
5. Nếu dùng PostgreSQL: trong Railway tạo **PostgreSQL** service → vào service Postgres → tab **Variables** (hoặc **Connect**) → dùng **Reference** để kéo biến sang service BE (xem bảng dưới).
6. Mỗi lần **push** lên `main`, Railway tự build và deploy lại.

### Điền Variables trên Railway

| Biến | Điền thế nào |
|------|----------------|
| **DB_HOST** | Không điền tay. Trong service **Postgres** → Variables/Connect → bấm **Add Reference** → chọn biến `PGHOST` (hoặc tương đương) của Postgres, reference sang BE. Hoặc nếu Railway cho **DATABASE_URL**, có thể dùng 1 biến `DATABASE_URL` và đổi code đọc từ URL. Cách nhanh: Postgres service thường có **Variables** → copy giá trị **PGHOST**, **PGPORT**, **PGUSER**, **PGPASSWORD**, **PGDATABASE** sang BE. |
| **DB_PORT** | Reference từ Postgres (vd. `PGPORT`, thường `5432`). |
| **DB_NAME** | Reference từ Postgres (vd. `PGDATABASE`). |
| **DB_USER** | Reference từ Postgres (vd. `PGUSER`). |
| **DB_PASSWORD** | Reference từ Postgres (vd. `PGPASSWORD`). **Quan trọng:** dùng reference, không gõ password local. |
| **JWT_SECRET** | Tạo chuỗi bí mật ≥ 32 ký tự (vd. random: `openssl rand -base64 32`), paste vào. **Đổi** giá trị mặc định. |
| **OPENROUTER_API_KEY** | API key từ [OpenRouter](https://openrouter.ai/keys). Chatbot dùng model `google/gemini-3-flash-preview`. |
| **OPENROUTER_MODEL** | (Tùy chọn) Mặc định `google/gemini-3-flash-preview`. |
| **SERVER_PORT** | Có thể **xóa** hoặc để `8080`. Railway tự set `PORT`; BE đã đọc `PORT` trước. |
| **FRONTEND_URL** | URL frontend thật khi deploy (vd. GitHub Pages: `https://username.github.io/FlourishTravel-FE` hoặc Vercel/Netlify). Không dùng localhost. |

**Gợi ý:** Trong project Railway, bấm vào service **Postgres** → tab **Variables** → mỗi biến có nút **Reference** (hoặc copy). Thêm reference đó vào service **BE** tương ứng (DB_HOST ← PGHOST, DB_PORT ← PGPORT, …).

---

## 2. Render

1. Vào [render.com](https://render.com) → đăng nhập bằng **GitHub**.
2. **New** → **Web Service** → kết nối repo **BE**.
3. Cấu hình:
   - **Build Command:** `mvn -B package -DskipTests`
   - **Start Command:** `java -jar target/flourish-travel-backend-1.0.0-SNAPSHOT.jar`
   - **Plan:** Free (có giới hạn).
4. Thêm **Environment Variables** (database URL, JWT secret, ...).
5. Có thể tạo **PostgreSQL** trên Render rồi dùng connection string cho BE.
6. Mỗi lần **push** lên branch đã chọn, Render tự deploy.

---

## 3. DigitalOcean App Platform (repo GitHub **BE**)

Repo **BE** có `pom.xml` ở root nhưng App Platform thường báo *No components detected* khi **chưa có `Dockerfile` trên GitHub** (file chỉ nằm local chưa push).

**Bước bắt buộc — push lên `main`:**

```bash
git add Dockerfile .dockerignore .do/app.yaml src/main/resources/application-cloud.yml
git commit -m "chore: add Dockerfile and DO App Platform spec"
git push origin main
```

**Tạo app:** **Apps → Create App → GitHub** → chọn repo **BE** (không phải monorepo FlourishTravel) → **Source directory** để **`/`** (root) → spec đọc từ **`BE/.do/app.yaml`** (`dockerfile_path: Dockerfile`). Set **`DB_PASSWORD`** và **`JWT_SECRET`** (Secret) trên dashboard. Thêm **`OPENROUTER_API_KEY`** (Secret) để bật chatbot LLM.

**Chỉnh tay (không dùng spec):** Web Service → Source directory **`/`** → Dockerfile **`Dockerfile`** → HTTP port **`8080`**.

**Health check (bắt buộc nếu báo `connection refused` hoặc 404):** Path **`/api/health`** (trả `{"status":"UP"}`), **Initial delay 300s**, Period 20s, Failure threshold 15. Ví dụ domain: `https://flourishtravel-rtdye.ondigitalocean.app/api`. Nếu app tạo trước khi có `.do/app.yaml`, vào **Component → Settings → Health Check** chỉnh tay (spec không tự ghi đè). Instance khuyến nghị **1 GB RAM** (`apps-s-1vcpu-1gb`).

**Database:** PostgreSQL DO (`application-cloud.yml`, profile `cloud`). Thêm **Trusted sources** cho App Platform vào cluster DB.

---

### Monorepo FlourishTravel (tuỳ chọn)

Nếu deploy từ repo gốc **FlourishTravel** (có thư mục `BE/`): dùng **`.do/app.yaml`** ở root monorepo (`source_dir: /BE`). Frontend **FEv2** deploy component riêng.

---

## 4. AWS (Spring Boot JAR trong container)

**Lưu ý (2026):** [AWS App Runner không còn nhận khách hàng mới](https://docs.aws.amazon.com/apprunner/latest/dg/apprunner-availability-change.html); tài khoản mới thường **không tạo được** service App Runner. AWS gợi ý dùng **Amazon ECS Express Mode** (tương đương độ đơn giản, vẫn container + ALB). Trong repo có **`BE/Dockerfile`**: build Maven multi-stage, chạy `app.jar`, port container **8080** (Spring vẫn đọc `PORT` nếu bạn set trên task).

### 4.1. Chuẩn bị

1. **Database:** PostgreSQL (vd. **DigitalOcean** — profile `cloud` trong `application-cloud.yml`). Security Group/task phải **cho phép outbound TCP** tới host DB (vd. `...ondigitalocean.com:25060`). Bật TLS: **`DB_SSL_MODE=require`** (đừng dùng `?sslmode=...` trong biến env — form ECS hay báo *Invalid value* vì ký tự `?`).
2. **Redis:** `application.yml` bật **spring-boot-starter-data-redis** (host mặc định `localhost`). Trên AWS bạn cần **ElastiCache (Redis)** trong VPC **hoặc** Redis có endpoint public, rồi set `REDIS_HOST` / `REDIS_PORT` (và `REDIS_PASSWORD` nếu có). Nếu chưa có Redis mà app lỗi health/startup, tạm thời dùng ElastiCache tối thiểu hoặc chỉnh cấu hình loại trừ Redis (tương tự profile `dev` trong `application-dev.yml`).
3. **Biến môi trường:** Không dùng file `.env` trong container. **`Dockerfile` luôn chạy** `--spring.profiles.active=${SPRING_PROFILES_ACTIVE:-cloud}` (nếu ECS để trống hoặc không set thì vẫn là `cloud`). **Bắt buộc** **`DB_PASSWORD`**. Xoá hẳn biến `SPRING_PROFILES_ACTIVE` trên task nếu trước đó set giá trị rỗng (dễ làm mất profile). Thêm `JWT_SECRET`, `FRONTEND_URL`, `API_BASE_URL`, MoMo URL, `REDIS_*`, v.v.
4. **Context path:** API nằm dưới **`/api`**. Health check ALB nên trỏ path trả 200, ví dụ **`/api/swagger-ui.html`** hoặc **`/api/actuator/health`** (nếu 403 thì đổi path hoặc nới security).

### 4.2. Đăng nhập Docker và push image lên **Amazon ECR**

`Dockerfile` nằm trong thư mục **`BE`** — build **từ trong `BE`** (không phải root monorepo). **Docker Desktop** phải đang chạy. Thay `ACCOUNT_ID`, `REGION`, tên repo nếu khác ví dụ dưới.

**Nếu PowerShell báo `aws` is not recognized:** chưa có [AWS CLI v2](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) — cài MSI hoặc `winget install Amazon.AWSCLI`, đóng mở terminal, chạy `aws --version`. Cách A (PowerShell-only) cần cài module `AWSPowerShell.NetCore`.

**Thứ tự bắt buộc:** `docker login` vào registry ECR **trước** `docker push`. Lỗi `no basic auth credentials` = chưa login hoặc token hết hạn (~12h) → chạy lại lệnh login.

**Cách A — AWS Tools for PowerShell** ([Registry authentication](https://docs.aws.amazon.com/AmazonECR/latest/userguide/registry_auth.html)):

```powershell
# Đăng nhập ECR (Region trùng repository của bạn, ví dụ us-east-1)
(Get-ECRLoginCommand -Region us-east-1).Password | docker login --username AWS --password-stdin 083011581293.dkr.ecr.us-east-1.amazonaws.com
```

**Cách B — AWS CLI v2** (nếu không dùng module PowerShell):

```powershell
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 083011581293.dkr.ecr.us-east-1.amazonaws.com
```

**Build, tag, push** (ví dụ repository `ftl-be`):

```powershell
cd D:\CN7\EXE\FlourishTravel\BE
docker build -t ftl-be .
docker tag ftl-be:latest 083011581293.dkr.ecr.us-east-1.amazonaws.com/ftl-be:latest
docker push 083011581293.dkr.ecr.us-east-1.amazonaws.com/ftl-be:latest
```

Token ECR hết hạn sau ~12 giờ — lỗi `unauthorized` khi push thì chạy lại bước `docker login`.

### 4.3. Amazon ECS Express Mode (thay App Runner, khuyến nghị cho tài khoản mới)

1. **ECR:** Tạo repository → build & push image từ `BE` (mục **3.2**).
2. Làm theo [Express Mode — Getting started (console hoặc CLI)](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/express-service-getting-started.html): một lần tạo service với **container image ECR**, **container port 8080**, IAM `ecsTaskExecutionRole` + `ecsInfrastructureRoleForExpressServices`, env giống bảng Railway.
3. AWS tự dựng **Fargate + Application Load Balancer** + URL mặc định; chỉ trả tiền tài nguyên (không phí riêng “Express Mode”).

CLI mẫu (đổi ARN, image, tên service): xem [Migration từ App Runner → ECS Express](https://docs.aws.amazon.com/apprunner/latest/dg/apprunner-availability-change.html) (mục `create-express-gateway-service`).

### 4.4. Elastic Beanstalk (Docker)

1. Tạo môi trường **Docker** trên Amazon Linux 2.
2. Deploy: đóng gói image lên ECR + `Dockerrun.aws.json` (v2) hoặc đẩy `Dockerfile` kèm context `BE` theo tài liệu Beanstalk Docker.
3. Cấu hình **instance role** nếu app cần gọi S3 (IAM), và **security group** mở inbound **8080** (hoặc port proxy mà Beanstalk dùng).

### 4.5. ECS Fargate (tự cấu hình cluster + service + ALB)

Phù hợp khi cần kiểm soát chi tiết **VPC**, nhiều service, ElastiCache chỉ private. Luồng: ECR → Task definition (port container **8080**) → Service + ALB. Cấu hình env giống trên.

---

## Lưu ý

- **Database:** Production nên dùng PostgreSQL. Railway/Render đều có PostgreSQL (free tier có giới hạn).
- **application.yml:** Dùng biến môi trường cho URL DB, secret, API key... không commit password vào repo.
- **CI trên GitHub:** Workflow `.github/workflows/ci.yml` chỉ **build + test** khi push; việc “chạy app trên internet” do Railway/Render đảm nhiệm sau khi bạn connect repo.
