package com.example.eom.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

// Cache TTL and key serialization configured in T-020 (Product Catalog)
@Configuration
@EnableCaching
public class RedisConfig {
}
