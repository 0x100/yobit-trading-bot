package ru._x100.yobitbot.config

import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate

@Configuration
class AppConfig {

    @Autowired
    YobitApiConfig clientProperties

    @Bean
    @ConfigurationProperties(prefix = "http-client")
    HttpComponentsClientHttpRequestFactory httpRequestFactory()
    {
        CloseableHttpClient httpClient = HttpClients.custom().setSSLHostnameVerifier(new NoopHostnameVerifier()).build()
        new HttpComponentsClientHttpRequestFactory(httpClient)
    }

    @Bean
    RestTemplate httpClient() {
        RestTemplate restTemplate = new RestTemplate(httpRequestFactory())
        restTemplate.getMessageConverters().add(messageConverter())

        restTemplate
    }

    @Bean
    HttpMessageConverter messageConverter() {
        HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter()
        messageConverter.setSupportedMediaTypes(Arrays.asList(
                MediaType.APPLICATION_JSON,
                MediaType.TEXT_HTML,
                MediaType.APPLICATION_OCTET_STREAM))
        messageConverter
    }

    @Bean
    HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED)
        headers.add('Key', clientProperties.key)
        headers
    }
}
