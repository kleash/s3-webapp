package com.example.s3webapp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = com.example.s3webapp.S3WebappApplication.class)
@TestPropertySource(properties = {
        "s3.buckets[0].id=test",
        "s3.buckets[0].name=Test Bucket",
        "s3.buckets[0].bucketName=test-bucket",
        "s3.buckets[0].endpointUrl=http://localhost:9999",
        "s3.buckets[0].accessKey=test",
        "s3.buckets[0].secretKey=secret",
        "s3.buckets[0].region=us-east-1",
        "s3.buckets[0].pathStyleAccess=true"
})
class S3PropertiesTest {

    @Autowired
    private S3Properties properties;

    @Test
    void loadsBucketConfig() {
        assertThat(properties.buckets()).hasSize(1);
        S3Properties.BucketConfig config = properties.buckets().get(0);
        assertThat(config.id()).isEqualTo("test");
        assertThat(config.pathStyleAccess()).isTrue();
    }
}
