package org.tron.defi.contract_mirror.core.graph;

import lombok.Getter;
import org.tron.defi.contract_mirror.core.Contract;

import java.util.ArrayList;
import java.util.List;
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
        if (!edge.getTo().equals(this)) {
            return;
        }
        addEdge(inEdges, edge);
    }

    public void addOutEdge(Edge edge) {
        if (!edge.getFrom().equals(this)) {
            return;
        }
        addEdge(outEdges, edge);
    }

    public void deleteInEdge(Edge edge) {
        if (!edge.getTo().equals(this)) {
            return;
        }
        deleteEdge(inEdges, edge);
    }

    public void deleteOutEdge(Edge edge) {
        if (!edge.getFrom().equals(this)) {
            return;
        }
        deleteEdge(outEdges, edge);
    }

    public boolean isEqual(Node node) {
        return node.getToken().getClass().equals(getToken().getClass()) &&
               node.getToken().getAddress().equals(getToken().getAddress());
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

    private void addEdge(List<Edge> edgeList, Edge edge) {
        wlock.lock();
        try {
            for (Edge e : edgeList) {
                if (edge.isEqual(e)) {
                    return;
                }
            }
            edgeList.add(edge);
        } finally {
            wlock.unlock();
        }
    }

    private void deleteEdge(List<Edge> edgeList, Edge edge) {
        wlock.lock();
        try {
            for (Edge e : edgeList) {
                if (edge.isEqual(e)) {
                    edgeList.remove(e);
                    return;
                }
            }
        } finally {
            wlock.unlock();
        }
    }
}
