package com.example.blog.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;
import java.util.Set;

@Service
public class ViewModeService {
    private static final Set<String> MOBILE_USER_AGENT_MARKERS = Set.of(
            "android", "iphone", "ipad", "ipod", "mobile", "windows phone",
            "blackberry", "opera mini", "opera mobi", "harmonyos", "miui"
    );

    private final ResourceLoader resourceLoader;

    public ViewModeService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String view(String viewName) {
        if (!StringUtils.hasText(viewName)
                || viewName.startsWith("redirect:")
                || viewName.startsWith("forward:")) {
            return viewName;
        }

        if (!isMobileClient()) {
            return viewName;
        }

        String mobileViewName = "mobile/" + viewName;
        Resource resource = resourceLoader.getResource("classpath:/templates/" + mobileViewName + ".html");
        return resource.exists() ? mobileViewName : viewName;
    }

    public boolean isMobileClient() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return false;
        }

        String requestedMode = request.getParameter("device");
        if ("mobile".equalsIgnoreCase(requestedMode)) {
            return true;
        }
        if ("desktop".equalsIgnoreCase(requestedMode)) {
            return false;
        }

        String clientHint = request.getHeader("Sec-CH-UA-Mobile");
        if ("?1".equals(clientHint)) {
            return true;
        }
        if ("?0".equals(clientHint)) {
            return false;
        }

        String userAgent = request.getHeader("User-Agent");
        if (!StringUtils.hasText(userAgent)) {
            return false;
        }

        String normalizedUserAgent = userAgent.toLowerCase(Locale.ROOT);
        for (String marker : MOBILE_USER_AGENT_MARKERS) {
            if (normalizedUserAgent.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}
