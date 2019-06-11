package edu.columbia.cs.psl.ioclones.analysis;

import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Value;


public class ReverseCFGNode<V extends Value> extends Frame {
    // in-order parents of this node
    private Set<ReverseCFGNode<V>> parents = new HashSet<ReverseCFGNode<V>>();

    // in-order successors of this node
    private Set<ReverseCFGNode<V>> children = new HashSet<ReverseCFGNode<V>>();

    // is this an exit node?
    boolean isEXIT; // aka is start node in reverse CFG


    public ReverseCFGNode(int nLocals, int nStack) {
        super(nLocals, nStack);
        this.isEXIT = false;
    }

    public ReverseCFGNode(Frame src) {
        super(src);
        this.isEXIT = false;
    }

    public Set<ReverseCFGNode<V>> getParents() {
        return this.parents;
    }

    public Set<ReverseCFGNode<V>> getChildren() {
        return this.children;
    }

    public boolean getEXIT() {
        return this.isEXIT;
    }

    public void setEXIT(boolean exit) {
        this.isEXIT = exit;
    }

    // recursively get all nodes in path to dest; ignore loops as definition of
    // control dependency is "weakened" by unnecessary loops, and are unneeded
    private Set<List<ReverseCFGNode<V>>> getPathsTo(ReverseCFGNode<V> curr,
                                                    ReverseCFGNode<V> dest,
                                                    LinkedList<ReverseCFGNode<V>> path,
                                                    Set<List<ReverseCFGNode<V>>> paths,
                                                    Set<ReverseCFGNode<V>> visited) {
        visited.add(curr);
        path.add(curr);

        if (curr.equals(dest)) {
            paths.add(path);
            return paths;
        }

        for (ReverseCFGNode<V> child : curr.getChildren()) {
            if (!visited.contains(child)) {
                paths.addAll(getPathsTo(child,
                                        dest,
                                        (LinkedList<ReverseCFGNode<V>>) path.clone(),
                                        paths,
                                        visited));
            }
        }

        return paths;
    }

    public Set<List<ReverseCFGNode<V>>> getAllPathsTo(ReverseCFGNode<V> dest) {
        return getPathsTo(this,
                          dest,
                          new LinkedList<ReverseCFGNode<V>>(),
                          new HashSet<List<ReverseCFGNode<V>>>(),
                          new HashSet<ReverseCFGNode<V>>());
    }

    

    /*
     * Recursively get all reachable nodes in graph, should be called starting from exit node
     */
    public HashSet<ReverseCFGNode<V>> getAllNodesRecursively(ReverseCFGNode<V> node,
                                              Set<ReverseCFGNode<V>> visited) {
        HashSet<ReverseCFGNode<V>> nodes = new HashSet<ReverseCFGNode<V>>();
        nodes.add(node);
        visited.add(node);

        for (ReverseCFGNode<V> parent : node.getParents()) {
            if (!visited.contains(parent)) {
                nodes.addAll(getAllNodesRecursively(parent, visited));
            }
        }

        for (ReverseCFGNode<V> child : node.getChildren()) {
            if (!visited.contains(child)) {
                nodes.addAll(getAllNodesRecursively(child, visited));
            }
        }

        return nodes;
    }

    public HashSet<ReverseCFGNode<V>> getAllNodes(ReverseCFGNode<V> node) {
        return getAllNodesRecursively(node, new HashSet<ReverseCFGNode<V>>());
    }

    public Map<ReverseCFGNode<V>, Set<ReverseCFGNode<V>>> getPostDominators() {
        // algorithm implemented from https://en.wikipedia.org/wiki/Dominator_(graph_theory)#Algorithms
        // getAllNodes(this) is expected to return a list of every single node in graph
        
        // do not call this until the EXIT node has been created in the graph!!! it is required to get
        // all nodes easily
        if (!this.isEXIT) {
            throw new IllegalArgumentException("Must be called from an EXIT node!");
        }

        // all nodes minus the exit node
        HashSet<ReverseCFGNode<V>> allNodes = getAllNodes(this);
        allNodes.remove(this);

        Map<ReverseCFGNode<V>, Set<ReverseCFGNode<V>>> dominators = new HashMap<ReverseCFGNode<V>,
                                                                                Set<ReverseCFGNode<V>>>();

        for (ReverseCFGNode<V> node: allNodes) {
            dominators.put(node, getAllNodes(this));
        }

        Set<ReverseCFGNode<V>> exitSet = new HashSet<ReverseCFGNode<V>>();
        exitSet.add(this);
        dominators.put(this, exitSet);

        boolean changed = true;
        while (changed) {
            changed = false;

            for (ReverseCFGNode<V> currentNode: allNodes) {
                Set<ReverseCFGNode<V>> currentDominators = dominators.get(currentNode);

                // child(p) is equiv to pred(p^) for p in cfg, p^ in reverse cfg
                Set<ReverseCFGNode<V>> newDominators = new HashSet<ReverseCFGNode<V>>();

                // if no children (entry), set to {node}
                if (currentNode.getChildren().size() == 0) {
                    newDominators.add(currentNode);
                }
                // else set to {node} union the intersection of all child dominators
                else {
                    newDominators.add(currentNode);

                    Set<ReverseCFGNode<V>> childDominators = (HashSet<ReverseCFGNode<V>>) allNodes.clone();
                    for (ReverseCFGNode<V> predecessorNode: currentNode.getChildren()) {
                        childDominators.retainAll(dominators.get(predecessorNode));
                    }

                    newDominators.addAll(childDominators);
                }

                if (!newDominators.equals(currentDominators)) {
                    changed = true;
                    dominators.put(currentNode, newDominators);
                }
            }
        }

        return dominators;
    }
}
