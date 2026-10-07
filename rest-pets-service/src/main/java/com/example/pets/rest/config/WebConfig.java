package com.example.pets.rest.config;

import com.example.pets.rest.domain.PetType;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToPetTypeConverter());
    }

    static final class StringToPetTypeConverter implements Converter<String, PetType> {
        @Override
        public PetType convert(String source) {
            return PetType.from(source);
        }
    }
}
