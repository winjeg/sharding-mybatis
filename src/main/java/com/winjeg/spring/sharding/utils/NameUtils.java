package com.winjeg.spring.sharding.utils;

import lombok.val;

/**
 * 统一一个创建类名的方法
 *
 * @author winjeg
 */
public class NameUtils {


    /**
     * 统一的命名方式，非常重要，涉及到映射，路由等
     */
    public static String buildClassName(String dsName, String orignalName) {
        return orignalName + process(dsName);
    }


    private static String process(String name) {
        StringBuilder builder = new StringBuilder();
        for (val c : name.toCharArray()) {
            if ((c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || c == '_'
                    || c == '$') {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
