package org.tron.defi.contract_mirror.core.graph;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class Graph {
    private final ConcurrentHashMap<String, Node> nodes = new ConcurrentHashMap<>();

    public Node getNode(String address) {
        return nodes.getOrDefault(address, null);
    }

    public Node addNode(Node node) {
        Node exist = nodes.putIfAbsent(node.getToken().getAddress(), node);
        return null != exist ? exist : node;
    }
}
