package org.tron.sunio.contract_mirror.mirror.router;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class CacheNode {
    private int index;
    private RoutNode node;

    public CacheNode(RoutNode node) {
        this.node = node;
        index = 0;
    }

    public RoutNode getSubNode() {
        RoutNode res = null;
        if (index < node.getSubNodes().size()) {
            res = node.getSubNodes().get(index);
            index++;
        }
        return res;
    }

}
