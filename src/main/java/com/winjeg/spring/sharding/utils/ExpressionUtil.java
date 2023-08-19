package com.winjeg.spring.sharding.utils;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.Expression;

import java.util.HashMap;
import java.util.Map;

/**
 * 使用轻量级表达式引擎来计算， 经测试微秒基别性能损耗
 * 基于 aviator 表达式引擎，
 *
 * @author winjeg
 */
public class ExpressionUtil {
    private static final AviatorEvaluatorInstance EVALUATOR = AviatorEvaluator.getInstance();

    /**
     * 评估一个表达式，计算出最终结果
     */
    public static String eval(String expStr, String varName, long varValue) {
        Map<String, Object> map = new HashMap<>();
        map.put(varName, varValue);
        return eval(expStr, map);
    }

    public static String eval(String expStr, Map<String, Object> expMap) {
        Expression e = EVALUATOR.compile(expStr, true);
        return (String) e.execute(expMap);
    }
}
