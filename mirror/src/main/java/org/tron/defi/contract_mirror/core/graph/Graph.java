package org.tron.defi.contract_mirror.core.graph;

import org.springframework.stereotype.Component;
import org.tron.defi.contract_mirror.core.pool.Pool;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class Graph {
    private final ConcurrentHashMap<String, Node> nodes = new ConcurrentHashMap<>();

    public Node addNode(Node node) {
        Node exist = nodes.putIfAbsent(node.getToken().getAddress(), node);
        return null != exist ? exist : node;
    }

    public void deleteEdge(Edge edge) {
        Node from = edge.getFrom();
        from.deleteOutEdge(edge);
        if (from.inDegree() + from.outDegree() == 0) {
            nodes.remove(from.getToken().getAddress());
        }
        Node to = edge.getTo();
        to.deleteInEdge(edge);
        if (to.inDegree() + to.outDegree() == 0) {
            nodes.remove(to.getToken().getAddress());
        }
    }

    public Node getNode(String address) {
        return nodes.getOrDefault(address, null);
    }

    public Node replaceNode(Node newNode) {
        Node oldNode = getNode(newNode.getToken().getAddress());
        if (null == oldNode) {
            return addNode(newNode);
        }

        for (Edge inEdge : oldNode.getInEdges()) {
            Node from = inEdge.getFrom();
            Pool pool = inEdge.getPool();
            Edge edge = new Edge(from, newNode, pool);
            newNode.addInEdge(edge);
            from.addOutEdge(edge);
            deleteEdge(inEdge);
            pool.replaceToken(newNode.getToken());
        }

        for (Edge outEdge : oldNode.getOutEdges()) {
            Node to = outEdge.getTo();
            Pool pool = outEdge.getPool();
            Edge edge = new Edge(newNode, to, pool);
            newNode.addOutEdge(edge);
            to.addInEdge(edge);
            deleteEdge(outEdge);
            pool.replaceToken(newNode.getToken());
        }
        assert oldNode.inDegree() + oldNode.outDegree() == 0;
        return addNode(newNode);
    }
}
