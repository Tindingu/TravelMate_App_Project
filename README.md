# 🌏 TRAVELMATE – ỨNG DỤNG KHÁM PHÁ ĐỊA ĐIỂM GẦN VỊ TRÍ NGƯỜI DÙNG

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android)
![Java](https://img.shields.io/badge/Language-Java-ED8B00?style=flat&logo=openjdk)
![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?style=flat&logo=firebase)
![Status](https://img.shields.io/badge/Status-Completed-success)

**Môn học:** NT118.Q13 – Phát triển ứng dụng trên thiết bị di động  
**Trường:** Đại học Công Nghệ Thông Tin – ĐHQG-HCM  
**Giảng viên hướng dẫn:** Trần Hồng Nghi  

---

## 👨‍💻 Thành viên nhóm

| STT | Họ và tên | MSSV |
|----:|-----------|------|
| 1 | Nguyễn Thanh Tín | 23521593 |
| 2 | Nguyễn Minh Trí | 23521643 |
| 3 | Vũ Đăng Khoa | 22520693 |

---

## 🧭 Giới thiệu dự án

**TravelMate** không chỉ là ứng dụng tìm kiếm địa điểm mà là một **Mạng xã hội du lịch thu nhỏ**. Ứng dụng hỗ trợ người dùng **khám phá** địa điểm qua AI, **lên lịch trình** chi tiết và **kết nối** với bạn bè qua các nhóm chat thời gian thực.

Điểm nổi bật là khả năng **đồng bộ lịch trình vào nhóm chat**, giúp việc đi du lịch theo nhóm trở nên dễ dàng và thống nhất.

---

## 🎯 Mục tiêu

- 🤖 **Thông minh:** Ứng dụng AI để gợi ý địa điểm phù hợp.
- 🗺️ **Tiện lợi:** Tìm kiếm, chỉ đường và lên kế hoạch trên cùng một nền tảng.
- 💬 **Kết nối:** Tương tác thời gian thực, chat nhóm, chia sẻ vị trí và lịch trình.
- 🔌 **Thực tế:** Ứng dụng các kỹ thuật xử lý dữ liệu và Realtime Database.

---

## ⚙️ Công nghệ & Kỹ thuật

| Lĩnh vực | Công nghệ sử dụng |
|----------|-------------------|
| **Ngôn ngữ** | Java (Android Native) |
| **Bản đồ & Định vị** | Google Maps SDK, OSRM (Open Source Routing Machine) |
| **Backend** | Firebase Authentication, Firestore (NoSQL), Storage |
| **Trí tuệ nhân tạo** | **Gemini AI API** (Gợi ý địa điểm) |
| **Giao tiếp mạng** | Retrofit / Volley |
| **Realtime** | Firestore Snapshot Listener (Chat, Vị trí) |
| **Giao diện** | Material Design, Lottie Animations |

---

## 🧩 Chức năng chi tiết

### 1️⃣ Quản lý người dùng & Mạng xã hội
- Đăng ký / Đăng nhập (Email, Google, Facebook).
- Quản lý hồ sơ cá nhân.
- **Kết bạn:** Tìm kiếm và kết bạn với người dùng khác.

### 2️⃣ Địa điểm & Bản đồ thông minh
- Hiển thị Google Maps với vị trí hiện tại.
- Tìm kiếm địa điểm (OpenStreetMap Data).
- **Chỉ đường:** Vẽ lộ trình di chuyển, tính toán khoảng cách và thời gian bằng **OSRM**.
- **Gemini AI:** Chatbot tư vấn địa điểm giải trí/ăn uống dựa trên sở thích.

### 3️⃣ Cộng đồng & Trò chuyện (Chat Module) 🌟
- **Chat 1-1:** Nhắn tin riêng tư với bạn bè.
- **Chat Nhóm:**
  - Tạo nhóm chat, thêm thành viên.
  - Gửi tin nhắn văn bản, hình ảnh, **gửi vị trí hiện tại**.
  - **Gắn chuyến đi (Trip) vào nhóm:** Tất cả thành viên cùng xem và theo dõi lịch trình chung.

### 4️⃣ Lịch trình cá nhân & Nhóm (Trip Management)
- Tạo chuyến đi (Tên, Ngày bắt đầu - kết thúc).
- Thêm địa điểm vào lịch trình chi tiết (Ngày, Giờ, Ghi chú).
- **Check trùng giờ:** Cảnh báo nếu thêm địa điểm bị trùng thời gian với lịch trình có sẵn.
- **Đồng bộ:** Lưu trip từ nhóm chat về lịch trình cá nhân.

### 5️⃣ Review & Wishlist
- Đánh giá, bình luận địa điểm (kèm ảnh).
- Lưu địa điểm yêu thích (Wishlist).
- Đồng bộ trên nhiều thiết bị.

### 6️⃣ Thông báo & Nhắc nhở
- **AlarmManager:** Nhắc nhở khi sắp đến giờ khởi hành/tham quan địa điểm.
- Thông báo khi có tin nhắn mới hoặc được thêm vào nhóm.

---

## 🛠️ Hướng dẫn cài đặt & Cấu hình (Setup Guide)

Để chạy được dự án **TravelMate** trên máy cá nhân, bạn cần thực hiện các bước cấu hình sau do ứng dụng sử dụng các dịch vụ bảo mật của Google và Firebase.

---

### 1️⃣ Yêu cầu hệ thống

- **Android Studio:** Iguana 2023.2.1 hoặc mới hơn *(khuyên dùng)*
- **JDK:** Java 11 hoặc Java 17
- **Thiết bị:** Máy ảo hoặc máy thật chạy **Android 8.0 (API level 26)** trở lên

---

### 2️⃣ Cài đặt Source Code

1. Clone dự án về máy:

```bash
git clone https://github.com/Tindingu/TravelMate_App_Project.git
```

2. Mở **Android Studio** → **Open** → Chọn thư mục dự án vừa clone

---

### 3️⃣ Cấu hình Firebase (**Bắt buộc ⚠️**)

Dự án sử dụng **Firebase Authentication**, **Firestore** và **Firebase Storage**.  
Bạn cần cấu hình Firebase và file `google-services.json`.

#### Các bước thực hiện:

1. Truy cập **Firebase Console** và tạo **project mới**
2. Thêm **Android App** với:
   - **Package name:** `com.example.travelmate`  
     *(Kiểm tra trong `AndroidManifest.xml` nếu khác)*
3. Tải file `google-services.json`
4. Copy file vào đường dẫn:

```text
TravelMate/app/google-services.json
```

#### Kích hoạt các dịch vụ sau trong Firebase Console:

- **Authentication**
  - Email/Password
  - Google Sign-in
- **Firestore Database**
  - Tạo database
  - Chọn **Test Mode**
- **Firebase Storage**
  - Tạo bucket lưu ảnh

---

### 4️⃣ Cấu hình API Keys

Ứng dụng sử dụng:
- **Google Maps API**
- **Gemini API**

#### 🔐 Cách 1: Dùng `local.properties` (Khuyên dùng)

Mở (hoặc tạo mới) file `local.properties` tại thư mục gốc project và thêm:

```properties
MAPS_API_KEY=AIzaSyDxxxxxxxxxxxxxxxxxxxxxxxxxxxx
GEMINI_API_KEY=AIzaSyCxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

Sau đó nhấn **Sync Project with Gradle Files** (biểu tượng 🐘).

---

#### ⚠️ Cách 2: Khai báo trực tiếp trong `AndroidManifest.xml` (demo nhanh)

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="PASTE_YOUR_GOOGLE_MAPS_KEY_HERE" />
```

> ⚠️ Không khuyến khích khi public source code

---

### 5️⃣ Build & Run

1. Đợi **Gradle Sync** hoàn tất
2. Kết nối thiết bị thật hoặc mở **Android Emulator**
3. Nhấn **Run ▶️** trong Android Studio

---

✅ Hoàn tất! Ứng dụng **TravelMate** đã sẵn sàng để chạy.

