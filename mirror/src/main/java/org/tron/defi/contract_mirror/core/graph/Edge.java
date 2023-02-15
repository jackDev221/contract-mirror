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
        return edge.getFrom().getToken().getAddress().equals(getFrom().getToken().getAddress()) &&
               edge.getTo().getToken().getAddress().equals(getTo().getToken().getAddress()) &&
               edge.getPool().getAddress().equals(getPool().getAddress());
    }
}
