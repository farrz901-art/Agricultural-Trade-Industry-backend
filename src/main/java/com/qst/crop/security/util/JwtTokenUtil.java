package com.qst.crop.security.util;
import io.jsonwebtoken.*;
import com.qst.crop.security.entity.JwtUser;
import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Data;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 生成令牌，验证等等一些操作
 * @author LENOVO
 */
@Data
//@ConfigurationProperties(prefix = "jwt")
@Component
public class JwtTokenUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);
    private String secret = "qst123456";
    // 过期时间 毫秒
    private static final Long expiration = 3600000L;
    private static final String header = "Authorization";

    /**
     * 从数据声明生成令牌
     *
     * @param claims 数据声明
     * @return 令牌
     */
    private String generateToken(Map<String, Object> claims) {
        Date expirationDate = new Date(System.currentTimeMillis() + expiration);
        return Jwts.builder().setClaims(claims).setExpiration(expirationDate).signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    public static String getHeader() {
        return header;
    }

    /**
     * 从令牌中获取数据声明
     *
     * @param token 令牌
     * @return 数据声明
     */
    private Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            logger.error("JWT令牌已过期: {}", e.getMessage());
            throw new RuntimeException("令牌已过期，请重新登录");
        } catch (UnsupportedJwtException e) {
            logger.error("JWT令牌格式不支持: {}", e.getMessage());
            throw new RuntimeException("令牌格式错误");
        } catch (MalformedJwtException e) {
            logger.error("JWT令牌格式无效（可能被篡改）: {}", e.getMessage());
            throw new RuntimeException("令牌无效");
        } catch (SignatureException e) {
            logger.error("JWT签名验证失败（密钥不匹配或令牌被篡改）: {}", e.getMessage());
            throw new RuntimeException("令牌验证失败");
        } catch (IllegalArgumentException e) {
            logger.error("JWT令牌为空或参数错误: {}", e.getMessage());
            throw new RuntimeException("令牌不能为空");
        } catch (Exception e) {
            logger.error("JWT令牌解析未知错误: {}", e.getMessage());
            throw new RuntimeException("令牌解析失败");
        }
    }
    //        Claims claims;
//        try {
//            claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
//        } catch (Exception e) {
//            claims = null;
//        }
//        return claims;

    /**
     * 生成令牌
     *
     * @param userDetails 用户
     * @return 令牌
     */
    public String generateToken(UserDetails userDetails) {
        JwtUser jwtUser= (JwtUser) userDetails;
        Map<String, Object> claims = new HashMap<>(2);
        claims.put(Claims.SUBJECT, userDetails.getUsername());
        claims.put(Claims.ISSUED_AT, new Date());
        claims.put("username", jwtUser.getUsername());
        claims.put("role",jwtUser.getAuthorities());
        return generateToken(claims);
    }

    /**
     * 从令牌中获取用户名
     *
     * @param token 令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        String username;
        try {
            Claims claims = getClaimsFromToken(token);
            username = claims.getSubject();
        } catch (Exception e) {
            username = null;
        }
        return username;
    }

    /**
     * 判断令牌是否过期
     *
     * @param token 令牌
     * @return 是否过期
     */
    public Boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 刷新令牌
     *
     * @param token 原令牌
     * @return 新令牌
     */
    public String refreshToken(String token) {
        String refreshedToken;
        try {
            Claims claims = getClaimsFromToken(token);
            claims.put(Claims.ISSUED_AT, new Date());
            refreshedToken = generateToken(claims);
        } catch (Exception e) {
            refreshedToken = null;
        }
        return refreshedToken;
    }

    /**
     * 验证令牌
     *
     * @param token       令牌
     * @param userDetails 用户
     * @return 是否有效
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        JwtUser user = (JwtUser) userDetails;
        String username = getUsernameFromToken(token);
        return (username.equals(user.getUsername()) && !isTokenExpired(token));
    }
}
