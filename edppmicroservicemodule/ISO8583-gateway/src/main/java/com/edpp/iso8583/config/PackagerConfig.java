package com.edpp.iso8583.config;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

@Configuration
public class PackagerConfig {

    @Bean
    public GenericPackager iso8583Packager() {
        try {
            // Load from classpath using InputStream
            InputStream is = new ClassPathResource("iso8583.xml").getInputStream();
            GenericPackager packager = new GenericPackager();
            packager.unpack(is);
            return packager;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load ISO8583 packager", e);
        }
    }
}