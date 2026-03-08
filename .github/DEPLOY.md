# Deploy Backend lên môi trường chạy

GitHub **không chạy được** ứng dụng Java/Spring Boot. Bạn cần deploy BE lên một dịch vụ bên ngoài. Dưới đây là 2 cách phổ biến (free tier).

---

## 1. Railway (đơn giản, khuyến nghị)

1. Vào [railway.app](https://railway.app) → đăng nhập bằng **GitHub**.
2. **New Project** → **Deploy from GitHub repo** → chọn repo **BE** (FlourishTravel/BE).
3. Railway tự nhận Spring Boot, build Maven và chạy.
4. Trong **Variables** thêm biến môi trường (giống `.env`):
   - `PORT` (Railway thường tự set)
   - Các biến trong `application.yml` cần đổi khi chạy production (DB, JWT secret, ...).
5. Nếu dùng PostgreSQL: trong Railway tạo **PostgreSQL** service, copy `DATABASE_URL` hoặc `JDBC_URL` vào biến môi trường của service BE.
6. Mỗi lần **push** lên `main`, Railway tự build và deploy lại.

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
