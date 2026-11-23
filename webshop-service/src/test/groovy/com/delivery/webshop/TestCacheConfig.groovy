package com.delivery.webshop

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.CacheManager
import org.springframework.cache.support.NoOpCacheManager
import org.springframework.context.annotation.Bean

/** Satisfies @EnableCaching in @WebMvcTest slices where CacheAutoConfiguration is not loaded. */
@TestConfiguration
class TestCacheConfig {
    @Bean
    CacheManager cacheManager() {
        new NoOpCacheManager()
    }
}
