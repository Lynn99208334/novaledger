package com.example.novaledger.common.masking;

import com.example.novaledger.common.util.SensitiveDataMasker;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

/**
 * 搭配 {@link Sensitive} annotation 使用的 Jackson Serializer。
 *
 * <p>在序列化輸出時根據 {@link MaskType} 呼叫對應的 {@link SensitiveDataMasker} 方法。
 * 明文不會被修改，只有進入序列化（API response / Audit Log）時才套用遮罩。
 *
 * <p>實作 {@link ContextualSerializer}，讓 Serializer 能在序列化前讀取欄位上的
 * {@link Sensitive} annotation，取得 {@link MaskType}。
 */
public class SensitiveMaskSerializer extends JsonSerializer<String>
        implements ContextualSerializer {

    private final MaskType maskType;

    /** Jackson 反射建立時使用的無參建構子，maskType 由 createContextual 設定。 */
    public SensitiveMaskSerializer() {
        this.maskType = null;
    }

    private SensitiveMaskSerializer(MaskType maskType) {
        this.maskType = maskType;
    }

    /**
     * 讀取欄位上的 {@link Sensitive} annotation，回傳帶有正確 maskType 的 Serializer 實例。
     * Jackson 在第一次序列化該欄位時呼叫此方法，之後會 cache 結果。
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        if (property != null) {
            Sensitive sensitive = property.getAnnotation(Sensitive.class);
            if (sensitive != null) {
                return new SensitiveMaskSerializer(sensitive.value());
            }
        }
        return this;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        String masked = applyMask(value);
        gen.writeString(masked);
    }

    private String applyMask(String value) {
        if (maskType == null) return value;
        return switch (maskType) {
            case ACCOUNT_NUMBER -> SensitiveDataMasker.maskAccountNumber(value);
            case PHONE -> SensitiveDataMasker.maskPhone(value);
            case EMAIL -> SensitiveDataMasker.maskEmail(value);
        };
    }
}
