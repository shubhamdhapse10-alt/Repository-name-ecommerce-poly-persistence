package com.example.ecommerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Enables @Async on OrderService.logActivityAsync(...)
    // For production, define a custom ThreadPoolTaskExecutor bean here
    // instead of relying on Spring's default SimpleAsyncTaskExecutor.
}
