package cheng7.mediadesk.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Writer;
import java.util.UUID;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    public ApiAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException exception
    ) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setStatus(403);
        ApiError apiError;
        if (exception instanceof CsrfException) {
            apiError = new ApiError(
                "CSRF_REJECTED",
                "请重试",
                UUID.randomUUID().toString()
            );
        } else {
            apiError = new ApiError(
                "FORBIDDEN",
                "请重试",
                UUID.randomUUID().toString()
            );
        }

        Writer writer = response.getWriter();

        objectMapper.writeValue(writer, apiError);
    }
}
