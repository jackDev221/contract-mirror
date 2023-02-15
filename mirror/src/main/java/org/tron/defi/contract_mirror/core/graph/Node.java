package org.tron.defi.contract_mirror.core.graph;

import lombok.Getter;
import org.tron.defi.contract_mirror.core.token.Token;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Node {
    private final ReadWriteLock rwlock = new ReentrantReadWriteLock();
    private final Lock rlock = rwlock.readLock();
    private final Lock wlock = rwlock.writeLock();
    private final ArrayList<Edge> edges = new ArrayList<>();
    @Getter
    private final Token token;

    public Node(Token token) {
        this.token = token;
    }

    public void addEdge(Edge edge) {
        wlock.lock();
        try {
            for (Edge e : edges) {
                edge.isEqual(e);
                return;
            }
            edges.add(edge);
        } finally {
            wlock.unlock();
        }
    }

    public ArrayList<Edge> getEdges() {
        ArrayList<Edge> currentEdges;
        rlock.lock();
        try {
            currentEdges = new ArrayList<>(edges);
        } finally {
            rlock.unlock();
        }
        return currentEdges;
    }

    public ArrayList<Edge> getEdgesUnsafe() {
        return edges;
    }
}

