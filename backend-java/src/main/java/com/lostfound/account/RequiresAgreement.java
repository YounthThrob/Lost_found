package com.lostfound.account;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 标注需要确认用户协议的受限操作接口。未确认协议的用户访问时返回 AGREEMENT_NOT_ACCEPTED。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresAgreement {
}
