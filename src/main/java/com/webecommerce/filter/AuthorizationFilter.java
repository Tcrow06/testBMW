package com.webecommerce.filter;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.webecommerce.constant.EnumRole;
import com.webecommerce.dto.response.people.CustomerResponse;
import com.webecommerce.dto.response.people.UserResponse;
import com.webecommerce.service.IOwnerService;
import com.webecommerce.service.impl.CustomerService;
import com.webecommerce.utils.JWTUtil;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/loginFilter")
public class AuthorizationFilter implements Filter {

    @Inject
    private CustomerService customerService;

    @Inject
    private IOwnerService ownerService;
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String url = request.getRequestURI();
        Cookie tokenCookie = JWTUtil.getCookieToken(request);
        if (tokenCookie != null && !tokenCookie.getValue().isEmpty()) {

            try {
                String token = tokenCookie.getValue();
                handleValidToken(token, url, request, response, filterChain);

            } catch (TokenExpiredException e) {
                handleExpiredToken(url, request, response, filterChain);

            } catch (Exception e) {
                System.out.println("Invalid token: " + e.getMessage());
                handleInvalidToken(request, response);
            }
        } else {
            handleNoToken(url, request, response, filterChain);
        }

    }

    private void handleValidToken(String token, String url, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        DecodedJWT decodedJWT = JWTUtil.verifyToken(token);
        String role = decodedJWT.getClaim("role").asString();
        Long id = decodedJWT.getClaim("id").asLong();

        UserResponse userResponse = null;
        if(role.equals(EnumRole.CUSTOMER.toString())){
            userResponse = customerService.findById(id);
        }
        if(role.equals(EnumRole.OWNER.toString())){
            userResponse = ownerService.findById(id);
        }
        if(userResponse == null){
            request.setAttribute("status", 404);
            JWTUtil.destroyToken(request,response);
            response.sendRedirect(request.getContextPath() + "/dang-nhap?action=login&message=token_invalid&alert=danger");
            return;
        }
        userResponse.setId(-1L);
        request.setAttribute("user",userResponse);
        request.setAttribute("status", 200);
        if (url.startsWith("/dang-nhap")||url.startsWith("/dang-ki")) {
            response.sendRedirect(request.getContextPath() + "/");
        } else if (url.startsWith("/chu-cua-hang")) {
            if (role.equals(EnumRole.OWNER.toString())) {
                filterChain.doFilter(request, response);
            } else {
                response.sendRedirect(request.getContextPath() + "/trang-chu?message=not_permission_access&alert=danger");
            }
        }else if (checkAdminNotAccess(url)){
            if (role.equals(EnumRole.CUSTOMER.toString())) {
                filterChain.doFilter(request, response);
            } else {
                response.sendRedirect(request.getContextPath() + "/chu-cua-hang?message=not_permission_access&alert=danger");
            }
        }
        else {
            filterChain.doFilter(request, response);
        }

    }

    private void handleExpiredToken(String url, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        JWTUtil.destroyToken(request, response);
        if (checkURLForNoToken(url)) {
            filterChain.doFilter(request, response);
        } else {
            response.sendRedirect(request.getContextPath() + "/dang-nhap?action=login&message=token_expired&alert=danger");
        }
    }

    private void handleInvalidToken(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        JWTUtil.destroyToken(request, response);
        response.sendRedirect(request.getContextPath() + "/dang-nhap?action=login&message=token_invalid&alert=danger");
    }

    private void handleNoToken(String url, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (checkURLForNoToken(url)) {
            filterChain.doFilter(request, response);
        } else {
            response.sendRedirect(request.getContextPath() + "/dang-nhap?action=login&message=not_permission_access&alert=danger");
        }
    }

    private boolean checkURLForNoToken(String url) {

        return !(url.startsWith("/chu-cua-hang")||url.startsWith("/thanh-toan"));
    }
    private boolean checkAdminNotAccess(String url){
        return (url.startsWith("/kiem-tra-san-pham")||url.startsWith("/thanh-toan")
                ||url.startsWith("/gio-hang"));
    }

    @Override
    public void destroy() {

    }
}
