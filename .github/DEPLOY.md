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
| **GEMINI_API_KEY** | Nếu dùng chatbot với Gemini: lấy key tại [Google AI Studio](https://aistudio.google.com/apikey), paste vào. Không dùng thì để trống (có thể lỗi khi gọi AI). |
| **OPENAI_API_KEY** | Nếu dùng chatbot với OpenAI: lấy key tại [OpenAI API Keys](https://platform.openai.com/api-keys), paste vào. Không dùng thì để trống. |
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

## Lưu ý

- **Database:** Production nên dùng PostgreSQL. Railway/Render đều có PostgreSQL (free tier có giới hạn).
- **application.yml:** Dùng biến môi trường cho URL DB, secret, API key... không commit password vào repo.
- **CI trên GitHub:** Workflow `.github/workflows/ci.yml` chỉ **build + test** khi push; việc “chạy app trên internet” do Railway/Render đảm nhiệm sau khi bạn connect repo.
