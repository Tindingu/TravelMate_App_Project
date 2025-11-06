# 🚀 Quick Start Guide - Forgot Password Feature

## Bắt đầu nhanh trong 5 phút

### 1️⃣ Kiểm tra Files (30 giây)

Đảm bảo các file sau đã có trong project:

```
✅ app/src/main/java/com/example/travelmate_app/ForgotPasswordActivity.java
✅ app/src/main/AndroidManifest.xml (có entry cho ForgotPasswordActivity)
✅ app/src/main/res/layout/activity_forgotpassword.xml
✅ app/build.gradle.kts (có Firebase Auth dependency)
✅ app/google-services.json
```

### 2️⃣ Build Project (2 phút)

```bash
cd C:\Users\Admin\Documents\UIT\NT118\TravelMate_App_Project
gradlew clean assembleDebug
```

### 3️⃣ Kiểm tra Firebase Console (1 phút)

1. Mở [Firebase Console](https://console.firebase.google.com)
2. Chọn project "TravelMate"
3. Authentication → Sign-in method
4. Verify "Email/Password" đã được **Enabled** ✅

### 4️⃣ Run App (30 giây)

```bash
gradlew installDebug
```

Hoặc click "Run" trong Android Studio

### 5️⃣ Test Flow (1 phút)

1. Mở app → Click "LOG IN"
2. Click "Forgot your password?"
3. Nhập email: `test@example.com`
4. Click "VERIFY"
5. Kiểm tra dialog thành công hiển thị ✅

## 📧 Test với Email thật

### Chuẩn bị:
- Tạo tài khoản test trong app (SignUp)
- Sử dụng email thật của bạn

### Test Steps:
1. **Forgot Password Flow:**
   - Login → "Forgot your password?"
   - Nhập email đã đăng ký
   - Click VERIFY
   - Dialog hiển thị ✅

2. **Check Email:**
   - Mở email inbox
   - Tìm email từ "noreply@..."
   - Click link reset password

3. **Reset Password:**
   - Web page Firebase hiển thị
   - Nhập mật khẩu mới (min 6 ký tự)
   - Click "Save"
   - Success message hiển thị ✅

4. **Login với Password mới:**
   - Quay về app
   - Login với mật khẩu mới
   - Success! 🎉

## ⚡ Quick Test Commands

### Build & Install:
```bash
gradlew clean build installDebug
```

### Check Logs:
```bash
adb logcat | findstr "ForgotPassword"
```

### Check Firebase Connection:
```bash
adb logcat | findstr "Firebase"
```

## 🐛 Quick Troubleshooting

### Issue: Build Error
```bash
gradlew clean
gradlew build
```

### Issue: Firebase Error
- Verify `google-services.json` trong `app/` folder
- Sync project: File → Sync Project with Gradle Files

### Issue: Email không gửi
- Check Firebase Console → Authentication → Users
- Verify email tồn tại trong danh sách
- Check quota: Firebase Console → Usage

### Issue: App crash
```bash
adb logcat | findstr "AndroidRuntime"
```

## 📱 Quick UI Check

### Màn hình Forgot Password phải có:
- ✅ Logo "TRAVELMATE"
- ✅ Tagline "IF NOT NOW, WHEN?"
- ✅ Title "Forgot Password?"
- ✅ Instructions text
- ✅ Email input field
- ✅ "VERIFY" button (orange)
- ✅ Back icon (bottom)

### Dialog Success phải có:
- ✅ Title: "Email Sent Successfully"
- ✅ Message với email address
- ✅ Button "OK"
- ✅ Button "Resend"

## 🎯 Quick Feature Checklist

Test nhanh 3 scenarios chính:

### ✅ Scenario 1: Happy Path (2 phút)
```
1. Email hợp lệ → Success dialog
2. Click OK → Về Login screen
3. Check email → Link nhận được
4. Click link → Reset page mở
5. Reset password → Success
```

### ✅ Scenario 2: Invalid Email (30 giây)
```
1. Email = "abc" → Error "Please enter a valid email"
2. Email = "" → Error "Email is required"
```

### ✅ Scenario 3: Resend (1 phút)
```
1. Email hợp lệ → Success dialog
2. Click Resend → Toast "sending..."
3. Check email → Email mới nhận được
```

## 📚 Tài liệu đầy đủ

Nếu cần thông tin chi tiết hơn:

- **Full Documentation:** `FORGOT_PASSWORD_README.md`
- **Testing Guide:** `FORGOT_PASSWORD_TESTING.md`
- **Implementation Summary:** `FORGOT_PASSWORD_SUMMARY.md`
- **Complete Checklist:** `IMPLEMENTATION_CHECKLIST.md`

## 🎉 Done!

Chức năng Forgot Password đã sẵn sàng sử dụng!

**Thời gian setup: ~5 phút**  
**Thời gian test: ~5 phút**  
**Total: ~10 phút** ⚡

---

**Need Help?**
- Check logs: `adb logcat`
- Review code: `ForgotPasswordActivity.java`
- Firebase Console: Authentication section

