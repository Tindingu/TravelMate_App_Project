# Hướng dẫn Test Chức năng Forgot Password

## Cách test chức năng

### 1. Chuẩn bị
- Đảm bảo đã có tài khoản đăng ký trong Firebase
- Bật Email/Password Authentication trong Firebase Console
- Có kết nối internet

### 2. Test Flow

#### Bước 1: Mở app
- Khởi động app TravelMate
- Màn hình Welcome sẽ hiển thị

#### Bước 2: Đi tới Login
- Click "LOG IN" từ màn hình Welcome
- Màn hình Login hiển thị

#### Bước 3: Click Forgot Password
- Click vào text "Forgot your password?"
- Màn hình ForgotPassword hiển thị

#### Bước 4: Nhập email
**Test Case 1: Email hợp lệ**
```
Input: your-email@example.com
Expected: Dialog thông báo email đã được gửi
```

**Test Case 2: Email sai định dạng**
```
Input: invalidemail
Expected: Hiển thị lỗi "Please enter a valid email address"
```

**Test Case 3: Email rỗng**
```
Input: (để trống)
Expected: Hiển thị lỗi "Email is required"
```

**Test Case 4: Email không tồn tại**
```
Input: nonexistent@example.com
Expected: Toast "No account found with this email address"
```

#### Bước 5: Kiểm tra email
- Mở email inbox
- Tìm email từ Firebase (noreply@...)
- Click vào link reset password

#### Bước 6: Reset password
- Web interface của Firebase hiển thị
- Nhập mật khẩu mới
- Click "Save"

#### Bước 7: Đăng nhập với mật khẩu mới
- Quay về app
- Click "OK" trên dialog
- Đăng nhập với mật khẩu mới

### 3. Test Resend Email

#### Bước 1-3: Giống như trên

#### Bước 4: Click Resend
- Nhập email hợp lệ
- Click "VERIFY"
- Dialog hiển thị
- Click "Resend" thay vì "OK"
- Email mới được gửi lại

### 4. Test Navigation

#### Test Back Button (ImageView)
```
Action: Click vào icon Back ở bottom
Expected: Quay về màn hình Login
```

#### Test System Back
```
Action: Nhấn nút Back của hệ thống
Expected: Quay về màn hình Login
```

#### Test After Success
```
Action: Click "OK" trên success dialog
Expected: Quay về màn hình Login, activity stack được clear
```

## Kiểm tra Log

### ADB Logcat Filter
```bash
adb logcat | grep -i "ForgotPassword"
```

### Các log quan trọng:
- Firebase send email request
- Firebase response
- Activity lifecycle (onCreate, onDestroy)
- Click events

## Common Issues & Solutions

### Issue 1: Email không được gửi
**Solution:**
- Kiểm tra Firebase Console > Authentication
- Verify Email/Password provider đã enable
- Kiểm tra quota project

### Issue 2: App crash khi click
**Solution:**
- Verify ForgotPasswordActivity đã được thêm vào AndroidManifest.xml
- Verify tất cả view ID match với layout XML

### Issue 3: Dialog không hiển thị
**Solution:**
- Kiểm tra Firebase response trong logcat
- Verify internet connection

### Issue 4: Link trong email không hoạt động
**Solution:**
- Kiểm tra Firebase Console > Authentication > Templates
- Verify authorized domains

## Checklist hoàn chỉnh

- [ ] ForgotPasswordActivity.java đã được tạo
- [ ] AndroidManifest.xml đã có entry cho ForgotPasswordActivity
- [ ] LoginActivity đã có sự kiện click cho tvForgotPassword
- [ ] Layout activity_forgotpassword.xml có đủ các view cần thiết
- [ ] Firebase Authentication đã được enable
- [ ] google-services.json đã được thêm vào project
- [ ] Build.gradle có dependency firebase-auth
- [ ] Test với email hợp lệ → success
- [ ] Test với email không hợp lệ → error message
- [ ] Test với email không tồn tại → error message
- [ ] Test resend email → email mới được gửi
- [ ] Test navigation back → về Login
- [ ] Test click link trong email → web reset password hiển thị
- [ ] Test đăng nhập với password mới → success

## Video Demo Script

1. **Intro (0:00-0:10)**
   - "Chức năng Forgot Password của TravelMate App"

2. **Demo Flow (0:10-1:00)**
   - Mở app
   - Login → Click "Forgot your password?"
   - Nhập email
   - Click VERIFY
   - Show dialog success
   - Mở email
   - Click link
   - Reset password
   - Đăng nhập thành công

3. **Demo Error Cases (1:00-1:30)**
   - Email sai định dạng
   - Email rỗng
   - Email không tồn tại

4. **Demo Resend (1:30-1:45)**
   - Nhập email
   - Click VERIFY
   - Click Resend
   - Show toast "email sent again"

5. **Outro (1:45-2:00)**
   - "Hoàn thành chức năng Forgot Password với Firebase"

