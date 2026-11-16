package com.example.demo.config;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.i18n.*;
import java.util.Locale;
@Configuration
public class LanguageConfig implements WebMvcConfigurer {
    @Bean public LocaleResolver localeResolver(){var resolver=new CookieLocaleResolver("harmony-language");resolver.setDefaultLocale(Locale.forLanguageTag("es"));return resolver;}
    @Override public void addInterceptors(InterceptorRegistry registry){var interceptor=new LocaleChangeInterceptor();interceptor.setParamName("lang");interceptor.setIgnoreInvalidLocale(true);registry.addInterceptor(interceptor);}
}
