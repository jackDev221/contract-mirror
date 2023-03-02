package org.tron.defi.contract_mirror.core.graph;

import lombok.Getter;
import org.tron.defi.contract_mirror.core.Contract;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Node {
    private final ReadWriteLock rwlock = new ReentrantReadWriteLock();
    private final Lock rlock = rwlock.readLock();
    private final Lock wlock = rwlock.writeLock();
    private final ArrayList<Edge> inEdges = new ArrayList<>();
    private final ArrayList<Edge> outEdges = new ArrayList<>();
    @Getter
    private final Contract token;

    public Node(Contract token) {
        this.token = token;
    }

    public void addInEdge(Edge edge) {
        wlock.lock();
        try {
            for (Edge e : inEdges) {
                edge.isEqual(e);
                return;
            }
            inEdges.add(edge);
        } finally {
            wlock.unlock();
        }
    }

    public void addOutEdge(Edge edge) {
        wlock.lock();
        try {
            for (Edge e : outEdges) {
                if (edge.isEqual(e)) {
                    return;
                }
            }
            outEdges.add(edge);
        } finally {
            wlock.unlock();
        }
    }

    public void deleteInEdge(Edge edge) {
        wlock.lock();
        try {
            for (Edge e : outEdges) {
                if (edge.isEqual(e)) {
                    outEdges.remove(e);
                    return;
                }
            }
        } finally {
            wlock.unlock();
        }
    }

    public void deleteOutEdge(Edge edge) {
        wlock.lock();
        try {
            for (Edge e : outEdges) {
                if (edge.isEqual(e)) {
                    outEdges.remove(e);
                    return;
                }
            }
        } finally {
            wlock.unlock();
        }
    }

    public ArrayList<Edge> getInEdges() {
        ArrayList<Edge> currentEdges;
        rlock.lock();
        try {
            currentEdges = new ArrayList<>(inEdges);
        } finally {
            rlock.unlock();
        }
        return currentEdges;
    }

    public ArrayList<Edge> getOutEdges() {
        ArrayList<Edge> currentEdges;
        rlock.lock();
        try {
            currentEdges = new ArrayList<>(outEdges);
        } finally {
            rlock.unlock();
        }
        return currentEdges;
    }

    public int inDegree() {
        rlock.lock();
        int degree = inEdges.size();
        rlock.unlock();
        return degree;
    }

    public int outDegree() {
        rlock.lock();
        int degree = outEdges.size();
        rlock.unlock();
        return degree;
    }
}
