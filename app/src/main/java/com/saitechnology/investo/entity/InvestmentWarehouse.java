package com.saitechnology.investo.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvestmentWarehouse implements Serializable {
    String userId, accountId, bankName, branchName, transactionTime, depositeDate, maturityDate, specialNotes;
}
