package com.parcelflow.carrier.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/**
 * Configuration du serveur SOAP Spring-WS.
 *
 * <p>@EnableWs active le framework Spring Web Services.
 *
 * <p>DefaultWsdl11Definition génère automatiquement le WSDL à partir du XSD.
 * On accède au WSDL généré via : http://localhost:8082/ws/carrier.wsdl
 *
 * <p>Explication Contract-First :
 * <ol>
 *   <li>Le XSD (carrier.xsd) définit les types de données</li>
 *   <li>JAXB2 génère les classes Java depuis le XSD</li>
 *   <li>L'endpoint Java implémente la logique sur ces classes générées</li>
 * </ol>
 *
 * @author Malek
 */
@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {

    /**
     * Enregistre le servlet Spring-WS sur l'URL /ws/*.
     * Tous les appels SOAP passent par ce servlet.
     */
    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        // Transforme les requêtes WSDL en réponses appropriées
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    /**
     * Définit le service SOAP "carrier" à partir du XSD.
     * L'URL du WSDL sera : http://localhost:8082/ws/carrier.wsdl
     *
     * @param carrierSchema le schéma XSD chargé par Spring
     */
    @Bean(name = "carrier")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema carrierSchema) {
        DefaultWsdl11Definition wsdl = new DefaultWsdl11Definition();
        wsdl.setPortTypeName("CarrierServicePort");
        wsdl.setLocationUri("/ws");
        wsdl.setTargetNamespace("http://parcelflow.com/carrier");
        wsdl.setSchema(carrierSchema);
        return wsdl;
    }

    /**
     * Charge le fichier XSD depuis le classpath.
     */
    @Bean
    public XsdSchema carrierSchema() {
        return new SimpleXsdSchema(new ClassPathResource("wsdl/carrier.xsd"));
    }
}
