# 🎬 HHKungfu TV - Android & Android TV App

Ứng dụng xem phim hoạt hình Kungfu / Hoạt hình 3D Trung Quốc chuyên dụng cho **Smart TV (Android TV / Google TV / Sony / Xiaomi / TV Box)** và **Điện thoại Android**.

---

## 🌟 Tính Năng Nổi Bật

- 🎨 **Giao diện Netflix Cinema Native:** Thiết kế Dark Cinema trực quan, phóng to thẻ phim khi di chuyển Remote TV, điều hướng mượt mà 60fps.
- ⚡ **Tự Động Phát Video 100% (Autoplay):** Tự động bỏ qua rào cản Iframe và tự khởi chạy video ngay khi nạp tập phim.
- 🎙️ **Bộ lọc Thuyết Minh & Việt Sub độc lập:** Tách biệt rõ ràng tab `[🎙️ Thuyết Minh]` và `[📝 Việt Sub]`.
- 📅 **Lịch Chiếu Phim Hàng Ngày (7 Ngày Trong Tuần):** Hiển thị ngay tại Trang Chủ và tự động chọn đúng ngày hôm nay.
- 🕒 **Lưu Lịch Sử Xem 30 Ngày (Multi-Layer Backup):** Tự động đánh dấu tập đã xem `✓`, hỗ trợ nút "Xem Tiếp" thông minh 1 chạm và bảo toàn dữ liệu khi cập nhật app.
- 🔄 **Hệ Thống Tự Động Cập Nhật OTA:** Kiểm tra phiên bản mới từ máy chủ và tự động cài đè bản nâng cấp.
- 🎮 **Hỗ trợ Remote TV Chuyên Dụng:**
  - `[OK / Center]`: Phát / Tạm dừng
  - `[Trái / Phải]`: Tua ±10s
  - `[Lên / Xuống]`: Ẩn/Hiện thanh điều khiển & đổi Server chất lượng (`1080P V1`, `1080P V2`, `4K V1`, `4K V2`).

---

## 🛠️ Công Nghệ Sử Dụng

- **Ngôn ngữ:** Kotlin
- **UI Framework:** Jetpack Compose for TV & Material 3
- **Network & HTML Parser:** OkHttp 4 + Jsoup
- **Image Loading:** Coil Compose
- **Architecture:** MVVM + Kotlin Coroutines + StateFlow
- **Minimum SDK:** Android 7.0 (API 24)
- **Target SDK:** Android 14 (API 34)

---

## 📦 Cài Đặt & Build

```bash
# Clone repository
git clone https://github.com/vnnit/HHKungfuTV.git

# Build Debug APK
./gradlew assembleDebug

# Output APK: app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License

Dự án được phát triển phục vụ mục đích học tập và giải trí cá nhân.
