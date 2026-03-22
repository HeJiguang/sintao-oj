package com.sintao.common.file.config;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OSSConfig {

    @Autowired
    private OSSProperties prop;

    public OSS ossClient;

    @Bean
    public OSS ossClient() throws ClientException {
        DefaultCredentialProvider credentialsProvider = CredentialsProviderFactory.newDefaultCredentialProvider(
                prop.getAccessKeyId(), prop.getAccessKeySecret());

        // 创建 ClientBuilderConfiguration
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

        // 娴ｈ法鏁ら崘鍛秹endpoint鏉╂稖顢戞稉濠佺炊
        ossClient = OSSClientBuilder.create()
                .endpoint(prop.getEndpoint())
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(prop.getRegion())
                .build();
        return ossClient;
    }

    @PreDestroy
    public void closeOSSClient() {
        ossClient.shutdown();
    }
}

