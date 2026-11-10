package com.lostfound.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** 拦截 @RequiresAgreement 标注的接口，未确认协议的用户访问时抛出 AGREEMENT_NOT_ACCEPTED。 */
@Component
public class AgreementInterceptor implements HandlerInterceptor {

    private final AgreementGuard agreementGuard;

    public AgreementInterceptor(AgreementGuard agreementGuard) {
        this.agreementGuard = agreementGuard;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod method) {
            RequiresAgreement annotation = method.getMethodAnnotation(RequiresAgreement.class);
            if (annotation != null) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getName() != null) {
                    agreementGuard.requireAgreed(authentication.getName());
                }
            }
        }
        return true;
    }
}
