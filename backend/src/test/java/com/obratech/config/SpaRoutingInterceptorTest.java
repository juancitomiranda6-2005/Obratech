package com.obratech.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

class SpaRoutingInterceptorTest {

    private final SpaRoutingInterceptor interceptor = new SpaRoutingInterceptor();

    @Test
    void forwardsHtmlNavigationToTheReactEntryPoint() throws Exception {
        HttpServletRequest request = request("/clientes/mis-postulaciones", "text/html");
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/index.html")).thenReturn(dispatcher);

        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(dispatcher).forward(request, response);
    }

    @Test
    void leavesJsonApiRequestsOnTheirController() throws Exception {
        HttpServletRequest request = request("/api/dashboard/cliente", "application/json");
        HttpServletResponse response = mock(HttpServletResponse.class);

        assertTrue(interceptor.preHandle(request, response, new Object()));
        verify(request, never()).getRequestDispatcher("/index.html");
    }

    @Test
    void leavesBinaryDocumentRequestsOnTheirController() throws Exception {
        HttpServletRequest request = request("/proyectos/documento/abc/preview", "text/html");
        HttpServletResponse response = mock(HttpServletResponse.class);

        assertTrue(interceptor.preHandle(request, response, new Object()));
        verify(request, never()).getRequestDispatcher("/index.html");
    }

    private HttpServletRequest request(String path, String accept) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(path);
        when(request.getContextPath()).thenReturn("");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Accept")).thenReturn(accept);
        return request;
    }
}