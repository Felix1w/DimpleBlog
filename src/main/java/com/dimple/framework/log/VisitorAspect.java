package com.dimple.framework.log;

import com.dimple.bean.VisitorLog;
import com.dimple.framework.log.annotation.VLog;
import com.dimple.utils.IpUtil;
import com.dimple.utils.ServletUtil;
import com.dimple.utils.ShiroUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @ClassName: VisitorAspect
 * @Description: 访客统计
 * @Auther: Owenb
 * @Date: 11/29/18 10:34
 * @Version: 1.0
 */
@Aspect
@Component
@EnableAsync
@Slf4j
public class VisitorAspect {

    @Autowired
    AsyncLog asyncLog;
    private Integer result;
    private String url;

    /**
     * 日志记录的切入点
     */
    @Pointcut("@annotation(com.dimple.framework.log.annotation.VLog)")
    public void logPointcut() {

    }

    /**
     * 在方法执行完毕之后执行
     *
     * @param joinPoint
     */
    @AfterReturning(pointcut = "logPointcut()")
    public void doAfterReturning(JoinPoint joinPoint) {
        handleLog(joinPoint, null);
    }

    /**
     * 出异常后执行的方法
     *
     * @param joinPoint 切入点
     * @param e         异常
     */
    @AfterThrowing(value = "logPointcut()", throwing = "e")
    public void doAfter(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, e);
    }

    /**
     * 处理日志信息
     *
     * @param joinPoint 切入点对象
     * @param exception 异常信息
     */
    @Async
    public void handleLog(JoinPoint joinPoint, Exception exception) {
        VLog annotationLog = getAnnotationLog(joinPoint);
        if (annotationLog == null) {
            return;
        }
        String sessionId = ShiroUtil.getSessionId();
        VisitorLog visitorLog = new VisitorLog();
        visitorLog.setSessionId(sessionId);

        String ip = IpUtil.getLocalHostAddress();
        visitorLog.setIp(ip);

        String url = ServletUtil.getRequest().getRequestURI();

        visitorLog.setRequireUrl(url);
        visitorLog.setBlogId(getNumberFromString(url));

        if (exception != null) {
            visitorLog.setStatus(false);
        } else {
            visitorLog.setStatus(true);
        }

        setControllerMethodDescription(visitorLog, annotationLog);


        asyncLog.recordVisitorLog(visitorLog);

    }

    private Integer getNumberFromString(String s) {
        if (StringUtils.isBlank(s)) {
            return null;
        }
        String regEx = "[^0-9]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(s);
        Integer result = null;
        try {
            result = Integer.valueOf(m.replaceAll("").trim());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 将Controller注解中的参数拿到，设置到Log对象中去
     *
     * @param visitorLog    log对象
     * @param annotationLog controller上注解标注的对象
     */
    private void setControllerMethodDescription(VisitorLog visitorLog, VLog annotationLog) {
        visitorLog.setTitle(annotationLog.title());

        //todo 是否需要设置请求的博客的ID
    }

    /**
     * 查询这个controller上面是不是有一个注解
     *
     * @param joinPoint
     * @return
     */
    private VLog getAnnotationLog(JoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        //获取方法
        Method method = methodSignature.getMethod();
        //返回
        return method == null ? null : method.getAnnotation(VLog.class);
    }
}
