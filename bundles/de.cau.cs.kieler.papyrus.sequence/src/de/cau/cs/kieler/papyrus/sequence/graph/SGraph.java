/*
 * KIELER - Kiel Integrated Environment for Layout Eclipse RichClient
 *
 * http://www.informatik.uni-kiel.de/rtsys/kieler/
 * 
 * Copyright 2012 by
 * + Christian-Albrechts-University of Kiel
 *   + Department of Computer Science
 *     + Real-Time and Embedded Systems Group
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */
package de.cau.cs.kieler.papyrus.sequence.graph;

import java.util.List;

import org.eclipse.elk.core.math.KVector;

import com.google.common.collect.Lists;

/**
 * The graph representation for sequence diagrams. The layout algorithm converts Papyrus sequence
 * diagrams into this internal representation which is more practicable than the original one.
 * 
 * @author grh
 * @kieler.design 2012-11-20 cds, msp
 * @kieler.rating yellow 2012-12-11 cds, ima
 */
public final class SGraph extends SGraphElement {
    private static final long serialVersionUID = -7952451128297135991L;
    
    /** The list of lifelines in the sequence diagram. It is not intended to have a special order. */
    private List<SLifeline> lifelines = Lists.newArrayList();
    /** The list of comments in the sequence diagram. It is not intended to have a special order. */
    private List<SComment> comments = Lists.newArrayList();
    /** The size of the diagram. This is modified during the layout process. */
    private KVector size = new KVector();

    /**
     * Get the size of the graph.
     * 
     * @return the KVector with the size
     */
    public KVector getSize() {
        return size;
    }

    /**
     * Get the list of lifelines in the SGraph. The list is NOT sorted in any way.
     * 
     * @return the list of lifelines
     */
    public List<SLifeline> getLifelines() {
        return lifelines;
    }

    /**
     * Get the list of comments in the SGraph.
     * 
     * @return the list of comments
     */
    public List<SComment> getComments() {
        return comments;
    }

    /**
     * Add a lifeline to the SGraph.
     * 
     * @param lifeline
     *            the new lifeline
     */
    public void addLifeline(final SLifeline lifeline) {
        this.lifelines.add(lifeline);
        lifeline.setGraph(this);
    }

    /**
     * Remove a lifeline from the SGraph.
     * 
     * @param lifeline
     *            the lifeline to be removed
     */
    public void removeLifeline(final SLifeline lifeline) {
        lifelines.remove(lifeline);
        lifeline.setGraph(null);
    }

    /**
     * This is not needed by the layout algorithm. This debug functionality simply prints the names
     * of the lifelines. 
     * {@inheritDoc}
     */
    public String toString() {
        String ret = "SGraph: ( ";
        for (SLifeline lifeline : this.lifelines) {
            ret += lifeline.getName() + " ";
        }
        ret += ")";
        return ret;
    }
}
