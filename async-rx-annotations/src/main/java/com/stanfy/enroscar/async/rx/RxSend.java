package com.stanfy.enroscar.async.rx;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Same as {@code @Send} but ensures that operator deals with
 * {@code rx.Observable} even if an annotated method return type is {@code Async}.
 * @author Roman Mazur - Stanfy (http://stanfy.com)
 */
@Retention(SOURCE)
@Target(METHOD)
public @interface RxSend {
}
