package com.example.blog.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViewModeServiceTest {
    private final ViewModeService viewModeService = new ViewModeService(new DefaultResourceLoader());

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void view_usesMobileTemplateWhenRequestComesFromMobileBrowser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("mobile/index", viewModeService.view("index"));
    }

    @Test
    void view_keepsDesktopTemplateForDesktopRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("index", viewModeService.view("index"));
    }

    @Test
    void view_fallsBackWhenMobileTemplateDoesNotExist() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14)");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("missing/view", viewModeService.view("missing/view"));
    }
}
