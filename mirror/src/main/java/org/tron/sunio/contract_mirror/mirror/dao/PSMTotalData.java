package org.tron.sunio.contract_mirror.mirror.dao;

import org.springframework.stereotype.Component;

import java.math.BigInteger;


@Component
public class PSMTotalData {
    private BigInteger totalUSDD;
    private BigInteger totalChargedUSDD;

    public synchronized void setTotalUSDD(BigInteger totalUSDD) {
        if (totalUSDD.compareTo(this.totalUSDD) > 0) {
            this.totalUSDD = new BigInteger(totalUSDD.toByteArray());
        }
    }

    public synchronized void setTotalChargedUSDD(BigInteger totalChargedUSDD) {
        if (totalChargedUSDD.compareTo(this.totalChargedUSDD) > 0) {
            this.totalChargedUSDD = new BigInteger(totalChargedUSDD.toByteArray());
        }
    }

    public BigInteger getTotalUSDD() {
        return new BigInteger(this.totalUSDD.toByteArray());
    }

    public BigInteger getTotalChargedUSDD() {
        return new BigInteger(this.totalUSDD.toByteArray());
    }
}
