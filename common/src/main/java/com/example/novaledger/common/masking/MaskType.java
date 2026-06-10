package com.example.novaledger.common.masking;

/**
 * 遮罩類型，對應 SensitiveDataMasker 的各個方法。
 * 新增遮罩規則時，在此加入新的 enum 值，並在 SensitiveMaskSerializer 加入對應分支。
 */
public enum MaskType {
    ACCOUNT_NUMBER,
    PHONE,
    EMAIL
}
