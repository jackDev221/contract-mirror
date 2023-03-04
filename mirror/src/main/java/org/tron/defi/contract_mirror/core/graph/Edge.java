package org.tron.defi.contract_mirror.core.graph;

import lombok.Data;
import org.tron.defi.contract_mirror.core.pool.Pool;

@Data
public class Edge {
    Node from;
    Node to;
    Pool pool;

    public Edge(Node from, Node to, Pool pool) {
        this.from = from;
        this.to = to;
        this.pool = pool;
    }

    public boolean isEqual(Edge edge) {
        // assuming pool type never change
        return edge.getFrom().isEqual(getFrom()) &&
               edge.getTo().isEqual(getTo()) &&
               edge.getPool().getAddress().equals(getPool().getAddress());
    }
}
