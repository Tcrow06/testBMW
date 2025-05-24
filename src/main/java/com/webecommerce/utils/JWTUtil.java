package com.webecommerce.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.webecommerce.constant.EnumRole;
import com.webecommerce.dto.response.people.CustomerResponse;
import com.webecommerce.dto.response.people.UserResponse;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JWTUtil {
    public static final String SECRET = "1234";
    public static final long EXPIRATION_TIME = 3600000;
    public static String generateToken(UserResponse user) {
        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        JWTCreator.Builder buildToken = JWT.create()
                .withClaim("id", user.getId())
                .withClaim("role",user.getRole().toString())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .withIssuedAt(new Date())
                .withJWTId(UUID.randomUUID().toString());
        return buildToken.sign(algorithm);
    }
    public static DecodedJWT verifyToken(String token){
        if(token ==null) return null;
        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        JWTVerifier verifier = JWT.require(algorithm).build();
        return verifier.verify(token);
    }
    public static Cookie getCookieToken(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        if(cookies!=null){
            for(Cookie cookie : cookies){
                if(cookie.getName().equals("token"))
                    return cookie;
            }
        }
        return null;
    }
    public static String getToken(HttpServletRequest request){
        Cookie cookie = getCookieToken(request);
        if(cookie!=null && !cookie.getValue().isEmpty()){
            return cookie.getValue();
        }
        return null;
    }
    public static void destroyToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("token")) {
                    cookie.setValue("");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                    break;
                }
            }
        }
    }
    public static Map<String, Object> getClaimsFromToken(String token) {
        DecodedJWT jwt = verifyToken(token);
        if (jwt == null)
            return null;
        Map<String, Object> claimsMap = new HashMap<>();
        Map<String, Claim> claims = jwt.getClaims();
        for (Map.Entry<String, Claim> entry : claims.entrySet()) {
            Claim claim = entry.getValue();
            if (claim.asString() != null) {
                claimsMap.put(entry.getKey(), claim.asString());
            } else if (claim.asLong() != null) {
                claimsMap.put(entry.getKey(), claim.asLong());
            } else if (claim.asInt() != null) {
                claimsMap.put(entry.getKey(), claim.asInt());
            } else if (claim.asBoolean() != null) {
                claimsMap.put(entry.getKey(), claim.asBoolean());
            } else if (claim.asDate() != null) {
                claimsMap.put(entry.getKey(), claim.asDate());
            }
        }
        return claimsMap;
    }
    public static Long getIdUser(HttpServletRequest request) {
        Map<String, Object> hashMap = JWTUtil.getClaimsFromToken(JWTUtil.getToken(request));
        if (hashMap != null && !hashMap.isEmpty()) {
            return (Long) hashMap.get("id");
        }
        return null;
    }

    public static String getRole(HttpServletRequest request) {
        Map<String, Object> hashMap = JWTUtil.getClaimsFromToken(JWTUtil.getToken(request));
        if (hashMap != null && !hashMap.isEmpty()) {
            return hashMap.get("role").toString();
        }
        return null;
    }

    public static Long getIdUserFromToken(String token) {
        if (token != null && !token.isEmpty()) {
            Map<String, Object> claims = getClaimsFromToken(token);
            if (claims != null && !claims.isEmpty()) {
                return (Long) claims.get("id");
            }
        }
        return null;
    }

    public static String getRoleFromToken(String token) {
        if (token != null && !token.isEmpty()) {
            Map<String, Object> claims = getClaimsFromToken(token);
            if (claims != null && !claims.isEmpty()) {
                return (String) claims.get("role"); // Trích xuất role từ claims
            }
        }
        return null; // Trả về null nếu không tìm thấy role
    }


}
