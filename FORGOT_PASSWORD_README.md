# Chức năng Forgot Password - TravelMate App

## Tổng quan
File `ForgotPasswordActivity.java` xử lý toàn bộ quy trình reset mật khẩu với Firebase Authentication.

## Quy trình hoạt động

### 1. **User nhập email**
   - User mở màn hình Login
   - Click vào "Forgot your password?"
   - Được chuyển đến `ForgotPasswordActivity`
   - Nhập email đã đăng ký

### 2. **Validate email**
   - Kiểm tra email không được rỗng
   - Kiểm tra định dạng email hợp lệ (sử dụng `Patterns.EMAIL_ADDRESS`)
   - Hiển thị lỗi ngay tại EditText nếu không hợp lệ

### 3. **Gửi email reset password**
   - Gọi `FirebaseAuth.sendPasswordResetEmail(email)`
   - Firebase tự động gửi email chứa link reset password
   - Disable nút VERIFY trong lúc xử lý để tránh spam click

### 4. **Xử lý kết quả**

   #### Thành công:
   - Hiển thị AlertDialog thông báo email đã được gửi
   - Có 2 option:
     - **OK**: Quay về màn hình Login
     - **Resend**: Gửi lại email nếu chưa nhận được
   - Hướng dẫn user kiểm tra cả spam folder

   #### Thất bại:
   - Hiển thị Toast với thông báo lỗi cụ thể:
     - "No account found with this email address" - Email chưa được đăng ký
     - "Network error" - Lỗi kết nối
     - "Too many requests" - Spam quá nhiều request
     - Hoặc thông báo lỗi từ Firebase

### 5. **User reset password**
   - User mở email và click vào link
   - Firebase tự động xử lý việc redirect đến trang reset password
   - User nhập mật khẩu mới (xử lý bởi Firebase Web Interface)
   - Sau khi reset thành công, user có thể đăng nhập với mật khẩu mới

## Các tính năng chính

### 1. **Validation Email**
```java
private boolean validateEmail(String email)
```
- Kiểm tra email rỗng
- Kiểm tra định dạng email với `Patterns.EMAIL_ADDRESS`
- Hiển thị lỗi trực tiếp trên EditText

### 2. **Gửi Email Reset**
```java
private void sendPasswordResetEmail(String email)
```
- Gọi Firebase Authentication API
- Xử lý callback thành công/thất bại
- Enable/Disable button để tránh spam

### 3. **Xử lý Lỗi Thông minh**
```java
private void handleResetEmailFailed(Exception exception)
```
- Parse thông báo lỗi từ Firebase
- Chuyển đổi sang thông báo dễ hiểu cho user
- Xử lý các trường hợp phổ biến

### 4. **Dialog Thông báo**
```java
private void showSuccessDialog(String email)
```
- Hiển thị thông báo chi tiết
- Option để resend email
- Hướng dẫn user kiểm tra spam folder

### 5. **Navigation**
```java
private void navigateToLogin()
```
- Quay về LoginActivity sau khi hoàn tất
- Clear activity stack để tránh quay lại bằng nút Back

## Cấu hình Firebase

Đảm bảo Firebase Authentication đã được cấu hình trong project:

1. **Firebase Console**:
   - Enable Email/Password Authentication
   - Tùy chỉnh email template (optional)
   - Thiết lập domain cho reset password link

2. **google-services.json**:
   - File này phải được thêm vào `app/` folder
   - Chứa cấu hình Firebase project

3. **build.gradle**:
   ```gradle
   implementation 'com.google.firebase:firebase-auth:22.x.x'
   ```

## Kết nối với các Activity khác

### LoginActivity
```java
// Thêm sự kiện click cho TextView "Forgot your password?"
tvForgotPassword.setOnClickListener(v -> {
    Intent intent = new Intent(this, ForgotPasswordActivity.class);
    startActivity(intent);
});
```

### AndroidManifest.xml
```xml
<activity android:name=".ForgotPasswordActivity" />
```

## UI/UX Features

1. **Disable button khi đang xử lý**:
   - Tránh user spam click
   - Tự động enable lại sau khi hoàn tất

2. **Error hiển thị trực tiếp trên EditText**:
   - User biết chính xác lỗi ở đâu
   - Không cần đọc Toast

3. **Dialog có 2 option**:
   - OK: Quay về Login
   - Resend: Gửi lại email nếu cần

4. **Hướng dẫn chi tiết**:
   - Thông báo user kiểm tra email
   - Nhắc nhở check spam folder
   - Hiển thị địa chỉ email đã gửi

## Security Features

1. **Không tiết lộ thông tin**:
   - Nếu email không tồn tại, Firebase vẫn trả về success (tùy cấu hình)
   - Tránh attacker dò tìm email đã đăng ký

2. **Rate limiting**:
   - Firebase tự động giới hạn số lần request
   - Tránh spam và abuse

3. **Link có thời hạn**:
   - Link reset password trong email có thời gian hết hạn
   - Chỉ sử dụng được 1 lần

## Testing

### Test case 1: Email hợp lệ đã đăng ký
- Input: Email đã đăng ký trong Firebase
- Expected: Hiển thị dialog thành công, email được gửi

### Test case 2: Email không hợp lệ
- Input: "invalidemail"
- Expected: Hiển thị lỗi "Please enter a valid email address"

### Test case 3: Email rỗng
- Input: ""
- Expected: Hiển thị lỗi "Email is required"

### Test case 4: Email chưa đăng ký
- Input: Email hợp lệ nhưng chưa có trong Firebase
- Expected: Hiển thị "No account found with this email address"

### Test case 5: Không có internet
- Input: Email hợp lệ, không có kết nối
- Expected: Hiển thị "Network error. Please check your connection"

### Test case 6: Spam requests
- Input: Gửi nhiều request liên tiếp
- Expected: Hiển thị "Too many requests. Please try again later"

## Customization

### Tùy chỉnh email template:
1. Vào Firebase Console
2. Authentication > Templates > Password reset
3. Chỉnh sửa nội dung email theo ý muốn

### Tùy chỉnh thông báo:
- Sửa các string trong `showSuccessDialog()`
- Sửa các error message trong `handleResetEmailFailed()`

### Tùy chỉnh UI:
- Chỉnh sửa `activity_forgotpassword.xml`
- Thay đổi màu sắc, font, spacing

## Troubleshooting

### Lỗi: "Email not sent"
- Kiểm tra Firebase Authentication có được enable
- Kiểm tra internet connection
- Kiểm tra quota của Firebase project

### Lỗi: "Invalid email"
- Email phải đúng định dạng
- Không có khoảng trắng thừa

### User không nhận được email:
- Kiểm tra spam folder
- Kiểm tra email có đúng không
- Đợi vài phút (đôi khi email bị delay)

## Notes

1. Firebase sẽ tự động xử lý việc reset password thông qua web interface
2. Không cần tạo activity `NewPasswordActivity` vì Firebase xử lý
3. Activity `EmailVerifyActivity` dùng cho verify email khi đăng ký, không phải cho reset password
4. Link reset password chỉ valid trong 1 giờ (default của Firebase)

## Future Improvements

1. Thêm loading animation khi đang gửi email
2. Thêm retry mechanism nếu network fail
3. Thêm analytics để track success rate
4. Thêm option để user chọn phương thức reset khác (SMS, etc.)
5. Thêm timer countdown cho resend button

