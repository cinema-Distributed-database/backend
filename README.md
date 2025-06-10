# Backend Rạp chiếu phim

Dự án này là dịch vụ backend cho một hệ thống đặt vé xem phim, được xây dựng bằng Spring Boot và sử dụng MongoDB làm cơ sở dữ liệu. Backend hỗ trợ các tính năng như quản lý phim, rạp chiếu, lịch chiếu, đặt vé, và thanh toán qua cổng VNPay.

## Yêu cầu hệ thống

* Java 21 hoặc cao hơn
* Maven 3.3+
* MongoDB

## 1. Cài đặt MongoDB Replica Set

Để đảm bảo tính sẵn sàng cao và khả năng chịu lỗi, ứng dụng này được cấu hình để kết nối đến một MongoDB Replica Set.

### a. Cấu hình file `mongod.conf`

Trên mỗi server (node) trong Replica Set của bạn, hãy cấu hình tệp `/etc/mongod.conf` (hoặc vị trí tương ứng trên hệ điều hành của bạn) như sau. Đảm bảo mỗi node có cấu hình này.

```yaml
# /etc/mongod.conf

# Cấu hình log
systemLog:
  destination: file
  logAppend: true
  path: /var/log/mongodb/mongod.log

# Cấu hình lưu trữ
storage:
  dbPath: /var/lib/mongodb # Hoặc /data/mongodb tùy vào cài đặt của bạn

# Cấu hình mạng
net:
  port: 27017
  bindIp: 0.0.0.0 # Cho phép kết nối từ bất kỳ IP nào

# Cấu hình Replication
replication:
  replSetName: "cinestar-replica" # Tên của Replica Set
```

### b. Khởi tạo Replica Set

Sau khi khởi động `mongod` service trên tất cả các node, kết nối đến một trong các node bằng `mongosh` và thực hiện các lệnh sau:

1.  **Khởi tạo Replica Set:** Lệnh này chỉ chạy trên node đầu tiên. Nó sẽ cấu hình node hiện tại làm `PRIMARY`.

    ```javascript
    rs.initiate({
        _id: "cinestar-replica",
        members: [
            { _id: 0, host: "your_primary_ip:27017" }
        ]
    })
    ```
    *(Thay thế `your_primary_ip` bằng địa chỉ IP của node chính)*

2.  **Thêm các node khác (SECONDARY):** Chạy các lệnh này trên `PRIMARY` để thêm các node khác vào Replica Set.

    ```javascript
    rs.add("your_secondary_ip_1:27017")
    rs.add("your_secondary_ip_2:27017")
    // Thêm các node khác nếu cần
    ```
    *(Thay thế bằng địa chỉ IP của các node phụ)*

3.  **Kiểm tra trạng thái:** Bạn có thể kiểm tra trạng thái của Replica Set bất cứ lúc nào bằng lệnh:

    ```javascript
    rs.status()
    ```

    Kết quả sẽ hiển thị trạng thái của các member (`PRIMARY`, `SECONDARY`).

## 2. Cấu hình ứng dụng Spring Boot

Tạo hoặc chỉnh sửa tệp `src/main/resources/application.properties` với các thông tin cấu hình dưới đây.

**Lưu ý:** Tuyệt đối không đưa các giá trị nhạy cảm (secret keys, password) vào mã nguồn công khai. Hãy sử dụng biến môi trường hoặc các dịch vụ quản lý cấu hình (như Spring Cloud Config, HashiCorp Vault) cho môi trường production.

```properties
# Tên ứng dụng
spring.application.name=cinema

# Cấu hình kết nối MongoDB Replica Set
# Thay thế các your_replica_ip và cinestar-replica nếu bạn đặt tên khác
spring.data.mongodb.uri=mongodb://your_replica_ip_1:27017,your_replica_ip_2:27017,your_replica_ip_3:27017/cinema_booking?replicaSet=cinestar-replica&w=majority
spring.data.mongodb.database=cinema_booking

# Cấu hình Actuator để kiểm tra health
management.endpoints.web.exposure.include=health,info,mappings
management.endpoint.mappings.enabled=true

# Cấu hình Server
server.port=8080

# Cấu hình Logging
logging.level.com.cinema=DEBUG
logging.level.org.springframework.data.mongodb=DEBUG # Bật DEBUG để xem query MongoDB

# Cấu hình Jackson (Date/Time)
spring.jackson.time-zone=Asia/Ho_Chi_Minh
spring.jackson.serialization.write-dates-as-timestamps=false

# Cấu hình Transaction
spring.data.mongodb.auto-index-creation=false

# Cấu hình ứng dụng
cinema.seat-hold.expiry-minutes=5
cinema.booking.confirmation-code.prefix=CINESTAR

# --- CẤU HÌNH VNPAY (Sử dụng biến môi trường trong production) ---
vnpay.tmn-code=[YOUR_VNPAY_TMN_CODE]
vnpay.hash-secret=[YOUR_VNPAY_HASH_SECRET]
vnpay.pay-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.default-return-url=http://localhost:3000/payment/result

# --- CẤU HÌNH URL FRONTEND ---
frontend.payment.success-url=http://localhost:3000/payment-success
frontend.payment.failure-url=http://localhost:3000/payment-failure
```

## 3. Biên dịch và Chạy ứng dụng

1.  **Biên dịch dự án:** Mở terminal tại thư mục gốc của backend và chạy lệnh Maven sau. Lệnh này sẽ bỏ qua các bài test và đóng gói ứng dụng thành một tệp `.jar`.

    ```bash
    mvn install -DskipTests=true
    ```

2.  **Chạy ứng dụng:** Sau khi biên dịch thành công, một tệp `.jar` sẽ được tạo trong thư mục `target`. Chạy ứng dụng bằng lệnh sau:

    ```bash
    java -jar ./target/cinema-0.0.1-SNAPSHOT.jar
    ```

Sau khi khởi động thành công, ứng dụng sẽ chạy trên cổng `8080` và sẵn sàng nhận các yêu cầu API.