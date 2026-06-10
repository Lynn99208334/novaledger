package com.example.novaledger.finance.account.converter;

import com.example.novaledger.common.util.AesEncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter：UserAccount.accountNumber 欄位自動加解密。
 *
 * <p>寫入 DB 前：明文 → AES-256-GCM 密文（Base64）
 * <p>從 DB 讀出：密文（Base64）→ 明文
 *
 * <p>Service / Controller 層完全透明，accountNumber 在 Java 側永遠是明文。
 */
@Converter
@Component
public class AccountNumberConverter implements AttributeConverter<String, String> {

    private static AesEncryptionService encryptionService;

    @Autowired
    public void setEncryptionService(AesEncryptionService encryptionService) {
        AccountNumberConverter.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String plainText) {
        if (encryptionService == null) {
            return plainText;
        }
        return encryptionService.encrypt(plainText);  // 明文 → 密文
    }

    @Override
    public String convertToEntityAttribute(String cipherText) {
        if (encryptionService == null) {
            return cipherText;
        }
        return encryptionService.decrypt(cipherText); // 密文 → 明文
    }
}
