package org.tron.sunio.contract_mirror.event_decode.logdata;

import lombok.Data;

import java.util.List;

@Data
public class RawData {
    private String address;
    private String[] topics;
    private String data;
}
