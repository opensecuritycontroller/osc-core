package org.osc.core.broker.job;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

/**

 * @param <T>
 *            - the class contained within a node
 *
 *            Graph implementation holding nodes of type T
 */
@SuppressWarnings("serial")
public class Graph<T> implements Iterable<T>, Serializable {

    public interface NodeVisitor<T> {

        /**
         * Visit the node at the "pre-order" point of the depth-first traversal
         * (i.e. before visiting its successors).
         *
         * @return false to abort the traversal
         */
        boolean preVisit(T node);

        /**
         * Visit the node at the "post-order" point of the depth-first traversal
         * (i.e. after visiting its successors).
         */
        void postVisit(T node);

    }

    /**
     * Iterator over the graph nodes in order of a depth-first <b>preorder</b>
     * traversal.
     */
    class DepthFirstPreorderIterator implements Iterator<T> {

        private Graph<T> graph;
        private Stack<T> nodesToVisit;
        private Set<T> nodesAlreadyVisited;

        DepthFirstPreorderIterator(T startNode, Graph<T> graph) {
            this.graph = graph;
            this.nodesToVisit = new Stack<T>();
            this.nodesAlreadyVisited = new HashSet<T>();

            this.nodesToVisit.push(startNode);
        }

        @Override
        public boolean hasNext() {
            return !this.nodesToVisit.isEmpty();
        }

        @Override
        public T next() {
            T node = this.nodesToVisit.pop();
            this.nodesAlreadyVisited.add(node);

            for (T successor : this.graph.getSuccessors(node)) {
                if (!this.nodesAlreadyVisited.contains(successor)) {
                    this.nodesToVisit.push(successor);
                }
            }

            return node;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    private Set<T> nodes;

    /**
     * The graphs edges represented as {@link Multimap} entries of the form
     *
     * <pre>
     * { src => { dst1, dst2, dst3} }
     * </pre>
     */
    private Multimap<T, T> edges;

    /*
     * Only used for the external representation of an edge
     */
    public static final class Edge<T> implements Serializable {
        private T src;
        private T dest;

        private Edge(T src, T dest) {
            this.src = src;
            this.dest = dest;
        }

        public T getSource() {
            return this.src;
        }

        public T getDest() {
            return this.dest;
        }

        @Override
        public String toString() {
            return String.format("%s ==> %s", this.src, this.dest);
        }
    }

    public Graph() {
        this.nodes = new HashSet<T>();
        this.edges = HashMultimap.create();
    }

    public Graph(Graph<T> other) {
        this.nodes = new HashSet<T>(other.nodes);
        this.edges = HashMultimap.create(other.edges);
    }

    private Graph(Set<T> nodes, Multimap<T, T> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public Set<Edge<T>> getEdges() {
        Set<Edge<T>> edgeSet = new HashSet<Edge<T>>();
        for (Map.Entry<T, T> edge : this.edges.entries()) {
            T src = edge.getKey();
            T dest = edge.getValue();

            edgeSet.add(new Edge<T>(src, dest));
        }
        return edgeSet;
    }

    public Set<T> getSuccessors(T node) {
        return ImmutableSet.copyOf(this.edges.get(node));
    }

    public Set<T> getPredecessors(T node) {
        Set<T> predecessors = new HashSet<T>();

        for (Map.Entry<T, T> edge : this.edges.entries()) {
            T src = edge.getKey();
            T dest = edge.getValue();

            if (node.equals(dest)) {
                predecessors.add(src);
            }
        }

        return Collections.unmodifiableSet(predecessors);
    }

    public Set<T> getDescendants(T node) {
        Set<T> descendants = Sets.newHashSet();
        for (T successor : this.getSuccessors(node)) {
            descendants.addAll(Sets.newHashSet(this.depthFirstPreorderIterator(successor)));
        }

        return descendants;
    }

    public Set<T> getAncestors(T node) {
        return this.reverse().getDescendants(node);
    }

    @SuppressWarnings("unchecked")
    public void addEdge(T src, T dest) {
        this.addEdges(src, dest);
    }

    @SuppressWarnings("unchecked")
    public void addEdges(T src, T... dests) {
        this.addNode(src);
        for (T dest : dests) {
            this.addNode(dest);
            this.edges.put(src, dest);
        }
    }

    public void addEdges(T src, Set<T> dests) {
        this.addNode(src);
        for (T dest : dests) {
            this.addNode(dest);
            this.edges.put(src, dest);
        }
    }

    public void addEdges(Set<T> srcs, T dest) {
        this.addNode(dest);
        for (T src : srcs) {
            this.addNode(src);
            this.edges.put(src, dest);
        }
    }

    public void addNode(T node) {
        this.nodes.add(node);
    }

    @Override
    public Iterator<T> iterator() {
        return this.getNodes().iterator();
    }

    public Set<T> getNodes() {
        return Collections.unmodifiableSet(this.nodes);
    }

    public Set<T> getSources() {
        return this.reverse().getSinks();
    }

    public Set<T> getSinks() {
        return Sets.difference(this.nodes, this.edges.keySet());
    }

    /**
     * Remove the node from the graph. All edges in which that node is a source
     * or destination will also be removed.
     */
    public void removeNode(T node) {
        this.nodes.remove(node);
        this.edges.removeAll(node);

        for (Iterator<Map.Entry<T, T>> i = this.edges.entries().iterator(); i.hasNext();) {
            Map.Entry<T, T> edge = i.next();

            T dest = edge.getValue();

            if (node.equals(dest)) {
                i.remove();
            }
        }

    }

    public void removeEdge(T src, T dest) {
        for (Iterator<T> i = this.edges.get(src).iterator(); i.hasNext();) {
            if (i.next().equals(dest)) {
                i.remove();
            }
        }
    }

    public void replace(T orig, T replacement) {

        for (T successor : this.getSuccessors(orig)) {
            this.addEdge(replacement, successor);
        }

        for (T predecessor : this.getPredecessors(orig)) {
            this.addEdge(predecessor, replacement);
        }

        this.removeNode(orig);
    }

    /**
     * Return a <b>new</b> <tt>Graph</tt> with the same nodes as this graph but
     * with the edges reversed.
     *
     * @return the reversed graph
     */
    public Graph<T> reverse() {
        Multimap<T, T> reverseEdges = HashMultimap.create();
        Multimaps.invertFrom(this.edges, reverseEdges);

        return new Graph<T>(new HashSet<T>(this.nodes), reverseEdges);
    }

    public boolean pathExists(T source, T dest) {
        if (source.equals(dest)) {
            return true;
        }

        for (Iterator<T> i = this.depthFirstPreorderIterator(source); i.hasNext();) {
            if (dest.equals(i.next())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Iterator over the graph nodes in order of a depth-first <b>preorder</b>
     * traversal.
     */
    public Iterator<T> depthFirstPreorderIterator(T startNode) {
        return new DepthFirstPreorderIterator(startNode, this);
    }

    public void depthFirstVisit(T startNode, NodeVisitor<T> visitor) {
        this.depthFirstVisitHelper(startNode, visitor, new HashSet<T>());
    }

    private void depthFirstVisitHelper(T node, NodeVisitor<T> visitor, Set<T> nodesVisited) {

        if (nodesVisited.contains(node)) {
            return;
        }

        nodesVisited.add(node);

        if (visitor.preVisit(node)) {

            for (T child : this.getSuccessors(node)) {
                this.depthFirstVisitHelper(child, visitor, nodesVisited);
            }

            visitor.postVisit(node);

        }

    }

    /**
     * Return a list of the nodes in "topological sort" order. An ordering in
     * which each node is guaranteed to come before nodes to which it has edges.
     *
     * <p>
     * The results of this method are unspecified if the graph has cycles.
     */
    public List<T> topologicalSort() {
        final Set<T> visited = new HashSet<T>(this.nodes.size());
        final Deque<T> result = new ArrayDeque<T>(this.nodes.size());

        /*
         * The *reverse* of the post-order traversal list represents a
         * topological sort of a graph.
         *
         * Since the graph could contain multiple sources, initiate a
         * depth-first search from each source short-circuiting each DFS if
         * we've already hit a node that has been visited by a DFS originated
         * from a previous source.
         */
        for (T source : this.getSources()) {
            this.depthFirstVisit(source, new NodeVisitor<T>() {
                @Override
                public boolean preVisit(T node) {
                    boolean alreadyVisited = visited.contains(node);
                    visited.add(node);

                    return !alreadyVisited;
                }

                @Override
                public void postVisit(T node) {
                    /*
                     * Since we're building up a list in reverse order,
                     * *prepend* the node to the head of the Deque
                     */
                    result.addFirst(node);
                }
            });
        }

        return ImmutableList.copyOf(result);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.nodes).append(this.edges).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Graph)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Graph<T> that = (Graph<T>) obj;
        return new EqualsBuilder().append(this.nodes, that.nodes).append(this.edges, that.edges).isEquals();
    }

    @Override
    public String toString() {
        return "Nodes:\n\t" + Joiner.on("\n\t").join(this.topologicalSort()) + "\n\nEndges:\n\t"
                + Joiner.on("\n\t").join(this.getEdges()) + "\n";
    }

}
