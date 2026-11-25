package com.parcelflow.order.client.carrier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webservices.client.HttpWebServiceMessageSenderBuilder;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

import java.time.Duration;

/**
 * Configuration du client SOAP vers carrier-service.
 *
 * <p>Les classes du package "generated" sont produites depuis carrier.xsd
 * (le meme fichier que celui du carrier-service : un seul contrat).
 * L'URL et les timeouts viennent de application.yml / variables d'environnement.
 */
@Configuration
public class CarrierClientConfig {

    @Bean
    public Jaxb2Marshaller carrierMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("com.parcelflow.order.client.carrier.generated");
        return marshaller;
    }

    @Bean
    public WebServiceTemplate carrierWebServiceTemplate(
            WebServiceTemplateBuilder builder,
            Jaxb2Marshaller carrierMarshaller,
            @Value("${carrier.soap.url}") String url,
            @Value("${carrier.soap.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${carrier.soap.read-timeout-ms:3000}") long readTimeoutMs) {
        return builder
                .setMarshaller(carrierMarshaller)
                .setUnmarshaller(carrierMarshaller)
                .setDefaultUri(url)
                // Timeout explicite sur tout appel reseau (exigence du sujet)
                // Spring Boot 3.3+ : les timeouts se configurent via HttpWebServiceMessageSenderBuilder
                .messageSenders(new HttpWebServiceMessageSenderBuilder()
                        .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                        .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                        .build())
                .build();
    }
}

