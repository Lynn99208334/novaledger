package com.example.novaledger.finance.bank.entity;

import com.example.novaledger.common.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(
        name = "bank_branches",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bank_branch", columnNames = {"bank_code", "branch_code"})
        }
)
public class BankBranch extends BaseEntity {

    @Column(name = "bank_code", nullable = false, length = 10)
    private String bankCode;

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String address;

    @Column(length = 30)
    private String phone;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
