package com.example.novaledger.common.masking;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 標注在 DTO 欄位上，表示此欄位在序列化輸出時需自動遮罩。
 *
 * <p>遮罩時機：Jackson 序列化（API response、Audit Log 序列化）。
 * 明文不會被覆蓋，Service 層仍可拿到完整值做業務邏輯。
 *
 * <p>使用範例：
 * <pre>
 *   {@literal @}Sensitive(MaskType.ACCOUNT_NUMBER)
 *   private String accountNumber;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveMaskSerializer.class)
public @interface Sensitive {
    MaskType value();
}
