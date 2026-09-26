package com.example.ecommerce.repository.mongo;

import com.example.ecommerce.document.ActivityLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ActivityLogRepository extends MongoRepository<ActivityLog, String> {
    List<ActivityLog> findByUserIdOrderByTimestampDesc(Long userId);
}
