/*
 * KIELER - Kiel Integrated Environment for Layout Eclipse RichClient
 *
 * http://www.informatik.uni-kiel.de/rtsys/kieler/
 * 
 * Copyright 2015 by
 * + Christian-Albrechts-University of Kiel
 *   + Department of Computer Science
 *     + Real-Time and Embedded Systems Group
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */
package de.cau.cs.kieler.papyrus.sequence.properties;

/**
 * Enumeration of possible node types for sequence diagrams.
 * 
 * @author cds
 * @author dja
 */
public enum NodeType {
    
    /** The root node. */
    SURROUNDING_INTERACTION,
    /** A lifeline. */
    LIFELINE,
    /** The destruction of a lifeline. */
    DESTRUCTION_EVENT,
    /** A comment. Comments are good. We like comments. */
    COMMENT,
    /** A constraint. */
    CONSTRAINT,
    /** An interaction operand. */
    INTERACTION_OPERAND,
    /** An interaction use. */
    INTERACTION_USE,
    /** A combined fragment. */
    COMBINED_FRAGMENT,
    /** An action execution specification. */
    ACTION_EXEC_SPECIFICATION,
    /** A behaviour execution specification. */
    BEHAVIOUR_EXEC_SPECIFICATION,
    /** A time constraint. */
    TIME_CONSTRAINT,
    /** A time observation. */
    TIME_OBSERVATION,
    /** A duration constraint. */
    DURATION_CONSTRAINT,
    /** A duration observation. */
    DURATION_OBSERVATION,
    /** A dummy node that provides a non-null source for a found message. */
    FOUND_MESSAGE_SOURCE,
    /** A dummy node that provides a non-null target for a lost message. */
    LOST_MESSAGE_TARGET;
    
    
    /**
     * Checks if the node models one of the execution type nodes.
     * 
     * @return {@code true} if a node with this type should be handled as an execution.
     */
    public boolean isExecutionType() {
        return this == BEHAVIOUR_EXEC_SPECIFICATION
                || this == ACTION_EXEC_SPECIFICATION
                || this == TIME_CONSTRAINT
                || this == DURATION_CONSTRAINT;
    }
    
}
