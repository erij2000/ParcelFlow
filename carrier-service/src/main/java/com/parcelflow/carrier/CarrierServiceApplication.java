package com.parcelflow.carrier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée du Carrier Service.
 *
 * <p>Ce service expose un endpoint SOAP selon le principe "Contract-First" :
 * le contrat (WSDL/XSD) est défini en premier, les classes Java sont
 * générées à partir de lui lors du build Maven (plugin JAXB2).
 *
 * <p>Endpoint SOAP accessible : http://localhost:8082/ws
 * WSDL auto-publié : http://localhost:8082/ws/carrier.wsdl
 *
 * @author Malek
 */
@SpringBootApplication
public class CarrierServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CarrierServiceApplication.class, args);
    }
}
