/*
 * KIELER - Kiel Integrated Environment for Layout Eclipse RichClient
 *
 * http://www.informatik.uni-kiel.de/rtsys/kieler/
 * 
 * Copyright 2015 by
 * + Kiel University
 *   + Department of Computer Science
 *     + Real-Time and Embedded Systems Group
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */
package de.cau.cs.kieler.papyrus.sequence.p6export;

import java.util.List;

import org.eclipse.elk.alg.layered.options.InternalProperties;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkBendPoint;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkEdgeSection;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.util.ElkGraphUtil;

import de.cau.cs.kieler.papyrus.sequence.ISequenceLayoutProcessor;
import de.cau.cs.kieler.papyrus.sequence.LayoutContext;
import de.cau.cs.kieler.papyrus.sequence.SequenceLayoutConstants;
import de.cau.cs.kieler.papyrus.sequence.graph.SComment;
import de.cau.cs.kieler.papyrus.sequence.graph.SGraph;
import de.cau.cs.kieler.papyrus.sequence.graph.SLifeline;
import de.cau.cs.kieler.papyrus.sequence.graph.SMessage;
import de.cau.cs.kieler.papyrus.sequence.properties.MessageType;
import de.cau.cs.kieler.papyrus.sequence.properties.NodeType;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceDiagramOptions;
import de.cau.cs.kieler.papyrus.sequence.properties.InternalSequenceProperties;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceExecution;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceExecutionType;

/**
 * Applies the layout results back to the original KGraph such that the Papyrus sequence diagram editor
 * can make sense of the coordinates.
 * 
 * @author grh
 * @author cds
 */
public final class PapyrusExporter implements ISequenceLayoutProcessor {

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final LayoutContext context, final IElkProgressMonitor progressMonitor) {
        progressMonitor.begin("Applying Layout Results", 1);
        
        // The height of the diagram (the surrounding interaction)
        double diagramHeight = context.sgraph.getSize().y + context.messageSpacing
                + context.lifelineHeader + context.lifelineYPos + 60;
        
        // Set position for lifelines/nodes
        for (SLifeline lifeline : context.lifelineOrder) {
            // Dummy lifelines don't need any layout
            if (lifeline.isDummy()) {
                continue;
            }

            ElkNode node = (ElkNode) lifeline.getProperty(InternalProperties.ORIGIN);
            

            if (node.getProperty(SequenceDiagramOptions.NODE_TYPE)
                    == NodeType.SURROUNDING_INTERACTION) {
                
                // This is the surrounding node
                break;
            }

            // Handle messages of the lifeline and their labels
            List<SequenceExecution> executions = lifeline.getProperty(
                    SequenceDiagramOptions.EXECUTIONS);
            applyMessageCoordinates(context, diagramHeight, lifeline, executions);

            // Apply execution coordinates and adjust positions of messages attached to these
            // executions.
            applyExecutionCoordinates(context, lifeline);

            // Set position and height for the lifeline.
            node.setY(lifeline.getPosition().y);
            node.setX(lifeline.getPosition().x);
            node.setHeight(lifeline.getSize().y);

            // Place destruction if existing
            ElkNode destruction = lifeline.getProperty(SequenceDiagramOptions.DESTRUCTION_NODE);
            if (destruction != null) {
                
                double destructionXPos = node.getWidth() / 2 - destruction.getWidth() / 2;
                double destructionYPos = node.getHeight() - destruction.getHeight();
                destruction.setX(destructionXPos);
                destruction.setY(destructionYPos);
            }
        }

        // Place all comments
        placeComments(context.sgraph);

        // Set position and size of surrounding interaction
        ElkNode parentLayout = context.kgraph;
        parentLayout.setWidth(context.sgraph.getSize().x);
        parentLayout.setHeight(diagramHeight);
        parentLayout.setX(context.borderSpacing);
        parentLayout.setY(context.borderSpacing);
        
        progressMonitor.done();
    }
    

    /**
     * Apply the calculated coordinates of the messages that are connected to the given lifeline.
     * 
     * @param context
     *            the layout context that contains all relevant information for the current layout run.
     * @param diagramHeight
     *            the height of the whole diagram
     * @param lifeline
     *            the lifeline whose messages are handled
     * @param executions
     *            the list of executions
     */
    private void applyMessageCoordinates(final LayoutContext context, final double diagramHeight,
            final SLifeline lifeline, final List<SequenceExecution> executions) {
        
        /*
         * TODO Set this to one if Papyrus team fixes its bug. Workaround for Papyrus bug:
         * Y-coordinates are stored in a strange way by Papyrus. When the message starts or ends at
         * a lifeline, y-coordinates must be given relative to the lifeline. However, these relative
         * coordinates must be scaled as if the lifeline was having the height of its surrounding
         * interaction.
         */
        double factor = (diagramHeight + SequenceLayoutConstants.TWENTY) / lifeline.getSize().y;

        // Resize node if there are any create or delete messages involved
        for (SMessage message : lifeline.getIncomingMessages()) {
            if (message.getProperty(SequenceDiagramOptions.MESSAGE_TYPE) == MessageType.CREATE) {
                // Set lifeline's yPos to the yPos of the create-message
                lifeline.getPosition().y = message.getTargetYPos() + context.lifelineHeader / 2;
                
                // Modify height of lifeline in order to compensate yPos changes
                lifeline.getSize().y += context.lifelineYPos - message.getTargetYPos()
                        - context.lifelineHeader / 2;
            } else if (message.getProperty(SequenceDiagramOptions.MESSAGE_TYPE) 
                    == MessageType.DELETE) {
                
                // Modify height of lifeline in order to end at the yPos of the delete-message
                lifeline.getSize().y -= context.sgraph.getSize().y + context.messageSpacing
                        - message.getTargetYPos();
            }
        }

        // The horizontal center of the current lifeline
        double llCenter = lifeline.getPosition().x + lifeline.getSize().x / 2;

        // Handle outgoing messages
        for (SMessage message : lifeline.getOutgoingMessages()) {
            ElkEdge edge = (ElkEdge) message.getProperty(InternalProperties.ORIGIN);
            ElkEdgeSection edgeSection = ElkGraphUtil.firstEdgeSection(edge, false, false);
            
            
            edgeSection.setStartY(message.getSourceYPos() * factor);
            edgeSection.setStartX(lifeline.getPosition().x + lifeline.getSize().x / 2);

            // Set execution coordinates according to connected messages coordinates
            if (executions != null) {
                for (SequenceExecution execution : executions) {
                    if (execution.getMessages().contains(message)) {
                        double sourceYPos = message.getSourceYPos();
                        if (execution.getPosition().y == 0) {
                            execution.getPosition().y = sourceYPos;
                            execution.getSize().y = 0;
                        } else {
                            if (sourceYPos < execution.getPosition().y) {
                                if (message.getSource() != message.getTarget()) {
                                    double diff = execution.getPosition().y - sourceYPos;
                                    execution.getPosition().y = sourceYPos;
                                    if (execution.getSize().y >= 0) {
                                        execution.getSize().y += diff;
                                    }
                                }
                            }
                            if (sourceYPos > execution.getPosition().y + execution.getSize().y) {
                                execution.getSize().y = sourceYPos - execution.getPosition().y;
                            }
                        }
                    }
                }
            }

            // Handle messages that lead to something else than a lifeline
            if (message.getTarget().isDummy()) {
                double reverseFactor = lifeline.getSize().y
                        / (diagramHeight + SequenceLayoutConstants.FOURTY);
                edgeSection.setEndY(
                        SequenceLayoutConstants.TWENTY + message.getTargetYPos() * reverseFactor);

                // Lost-messages end between its source and the next lifeline
                if (message.getProperty(SequenceDiagramOptions.MESSAGE_TYPE) == MessageType.LOST) {
                    edgeSection.setEndX(lifeline.getPosition().x + lifeline.getSize().x 
                            + context.lifelineSpacing / 2);
                }
            }

            if (message.getSource() == message.getTarget()) {
                // Specify bendpoints for selfloops
                List<ElkBendPoint> bendPoints = edgeSection.getBendPoints();
                bendPoints.get(0).setX(llCenter + context.messageSpacing / 2);
                bendPoints.get(0).setY(edgeSection.getStartY());
            }

            // Walk through the labels and adjust their position
            double lableFactor = (diagramHeight + SequenceLayoutConstants.TWENTY) / lifeline.getSize().y;
            placeLabels(context, lifeline, lableFactor, llCenter, message, edge);
        }

        // Handle incoming messages
        for (SMessage message : lifeline.getIncomingMessages()) {
            ElkEdge edge = (ElkEdge) message.getProperty(InternalProperties.ORIGIN);
            ElkEdgeSection edgeSection = ElkGraphUtil.firstEdgeSection(edge, false, false);
            
            edgeSection.setEndX(lifeline.getPosition().x + lifeline.getSize().x / 2);
            edgeSection.setEndY(message.getTargetYPos() * factor);

            if (message.getProperty(SequenceDiagramOptions.MESSAGE_TYPE) == MessageType.CREATE) {
                // Reset x-position of create message because it leads to the header and not the line
                edgeSection.setEndX(lifeline.getPosition().x);
            } else if (message.getProperty(SequenceDiagramOptions.MESSAGE_TYPE) 
                    == MessageType.DELETE) {
                // Reset y-position of delete message to end at the end of the lifeline
                edgeSection.setEndY((lifeline.getPosition().y + lifeline.getSize().y 
                        - context.lifelineHeader) * factor);
            }

            // Reset execution coordinates if the message is contained in an execution
            if (executions != null) {
                for (SequenceExecution execution : executions) {
                    if (execution.getMessages().contains(message)) {
                        double targetYPos = message.getTargetYPos();
                        if (execution.getPosition().y == 0) {
                            execution.getPosition().y = targetYPos;
                            execution.getSize().y = 0;
                        } else {
                            if (targetYPos < execution.getPosition().y) {
                                double diff = execution.getPosition().y - targetYPos;
                                execution.getPosition().y = targetYPos;
                                if (execution.getSize().y >= 0) {
                                    execution.getSize().y += diff;
                                }
                            }
                            if (targetYPos > execution.getPosition().y + execution.getSize().y) {
                                execution.getSize().y = targetYPos - execution.getPosition().y;
                            }
                        }
                    }
                }
            }

            // Handle messages that come from something else than a lifeline
            if (message.getSource().isDummy()) {
                
                double reverseFactor = lifeline.getSize().y
                        / (diagramHeight + SequenceLayoutConstants.FOURTY);
                edgeSection.setStartY(
                        SequenceLayoutConstants.TWENTY + message.getSourceYPos() * reverseFactor);

                // Found-messages start between its source and the previous lifeline
                if (message.getProperty(SequenceDiagramOptions.MESSAGE_TYPE) == MessageType.FOUND) {
                    edgeSection.setStartX(lifeline.getPosition().x - context.lifelineSpacing / 2);
                }
            }

            if (message.getSource() == message.getTarget()) {
                // Specify bendpoints for selfloops
                List<ElkBendPoint> bendPoints = edgeSection.getBendPoints();
                bendPoints.get(1).setX((float) (llCenter + context.messageSpacing / 2));
                bendPoints.get(1).setY(edgeSection.getEndY());
            }
        }
    }

    /**
     * Place the label(s) of the given message.
     * 
     * @param context
     *            the layout context that contains all relevant information for the current layout run.
     * @param lifeline
     *            the current lifeline
     * @param factor
     *            the edge factor
     * @param llCenter
     *            the horizontal center of the current lifeline
     * @param message
     *            the current message
     * @param edge
     *            the edge representation of the message
     */
    private void placeLabels(final LayoutContext context, final SLifeline lifeline,
            final double factor, final double llCenter, final SMessage message, final ElkEdge edge) {
        
        ElkEdgeSection edgeSection = ElkGraphUtil.firstEdgeSection(edge, false, false);
                
        for (ElkLabel label : edge.getLabels()) {

            // The index of the current lifeline in the ordered list of lifelines
            int lifelineIndex = context.lifelineOrder.indexOf(lifeline);

            if (message.getTarget().getHorizontalSlot() > lifeline.getHorizontalSlot()) {
                // Message leads rightwards
                switch (context.labelAlignment) {
                case SOURCE_CENTER:
                    // If the lifeline is the last lifeline (lost message), fall through to SOURCE
                    // placement to avoid ArrayIndexOutOfBoundsException
                    if (lifelineIndex + 1 < context.lifelineOrder.size()) {
                        // Place labels centered between the source lifeline and its neighbored
                        // lifeline
                        SLifeline nextLL = context.lifelineOrder.get(lifelineIndex + 1);
                        double center = (llCenter + nextLL.getPosition().x + nextLL.getSize().x / 2) / 2;
                        label.setX(center - label.getWidth() / 2 );
                        break;
                    }
                case SOURCE:
                    // Place labels near the source lifeline
                    label.setX(llCenter + SequenceLayoutConstants.LABELSPACING );
                    break;
                case CENTER:
                    // Place labels in the center of the message
                    double targetCenter = message.getTarget().getPosition().x
                            + message.getTarget().getSize().x / 2;
                    label.setX((llCenter + targetCenter) / 2 - label
                            .getWidth() / 2);
                }
                // Create messages should not overlap the target's header
                if (message.getProperty(SequenceDiagramOptions.MESSAGE_TYPE) == MessageType.CREATE) {
                    label.setX(llCenter + SequenceLayoutConstants.LABELSPACING);
                }
                label.setY(-label.getHeight() - 2);
            } else if (message.getTarget().getHorizontalSlot() < lifeline.getHorizontalSlot()) {
                // Message leads leftwards
                switch (context.labelAlignment) {
                case SOURCE_CENTER:
                    // If the lifeline is the first lifeline (found message), fall through to SOURCE
                    // placement to avoid ArrayIndexOutOfBoundsException
                    if (lifelineIndex > 0) {
                        // Place labels centered between the source lifeline and its neighbored
                        // lifeline
                        SLifeline lastLL = context.lifelineOrder.get(lifelineIndex - 1);
                        double center = (llCenter + lastLL.getPosition().x + lastLL.getSize().x / 2) / 2;
                        label.setX(center - label.getWidth() / 2);
                        break;
                    }
                case SOURCE:
                    // Place labels near the source lifeline
                    label.setX(
                            llCenter - label.getWidth() - SequenceLayoutConstants.LABELSPACING);
                    break;
                case CENTER:
                    // Place labels in the center of the message
                    double targetCenter = message.getTarget().getPosition().x
                            + message.getTarget().getSize().x / 2;
                    label.setX((llCenter + targetCenter) / 2 - label
                            .getWidth() / 2);
                }
                label.setY((message.getSourceYPos() + 2) * factor);
            } else {
                // Message is selfloop
                
                // Place labels right of the selfloop
//                ElkEdgeSection edgeSection = ElkGraphUtil.firstEdgeSection(edge, false, false);
                double xPos;
                if (edgeSection.getBendPoints().size() > 0) {
                    ElkBendPoint firstBend = edgeSection.getBendPoints().get(0);
                    xPos = firstBend.getX();
                } else {
                    xPos = edgeSection.getStartX();
                }
                label.setY(
                        (message.getSourceYPos() + SequenceLayoutConstants.LABELSPACING) * factor);
                label.setX(
                        xPos + SequenceLayoutConstants.LABELMARGIN / 2);
            }
            
        }
    }

    /**
     * Apply execution coordinates and adjust positions of messages attached to these executions.
     * 
     * @param context
     *            the layout context that contains all relevant information for the current layout run.
     * @param lifeline
     *            the lifeline, whose executions are placed
     */
    private void applyExecutionCoordinates(final LayoutContext context, final SLifeline lifeline) {
        List<SequenceExecution> executions = lifeline.getProperty(SequenceDiagramOptions.EXECUTIONS);
        if (executions == null) {
            return;
        }

        // Set xPos, maxXPos and height / maxYPos
        arrangeExecutions(executions, lifeline.getSize().x);

        // Get the layout data of the execution
        ElkNode node = (ElkNode) lifeline.getProperty(InternalProperties.ORIGIN);

        // Walk through the lifeline's executions
        node.setProperty(SequenceDiagramOptions.EXECUTIONS, executions);
        for (SequenceExecution execution : executions) {
            Object executionObj = execution.getOrigin();

            if (executionObj instanceof ElkNode) {
                if (execution.getType() == SequenceExecutionType.DURATION
                        || execution.getType() == SequenceExecutionType.TIME_CONSTRAINT) {
                    
                    execution.getPosition().y += SequenceLayoutConstants.TWENTY;
                }

                // Apply calculated coordinates to the execution
                ElkNode executionNode = (ElkNode) executionObj;
                executionNode.setX( execution.getPosition().x);
                executionNode.setY(execution.getPosition().y - context.lifelineYPos);
                executionNode.setWidth(execution.getSize().x);
                executionNode.setHeight(execution.getSize().y);

                // Determine max and min y-pos of messages
                double minYPos = lifeline.getSize().y;
                double maxYPos = 0;
                for (Object messObj : execution.getMessages()) {
                    if (messObj instanceof SMessage) {
                        SMessage message = (SMessage) messObj;
                        double messageYPos;
                        if (message.getSource() == lifeline) {
                            messageYPos = message.getSourceYPos();
                        } else {
                            messageYPos = message.getTargetYPos();
                        }
                        if (messageYPos < minYPos) {
                            minYPos = messageYPos;
                        }
                        if (messageYPos > maxYPos) {
                            maxYPos = messageYPos;
                        }
                    }
                }

                /*
                 * TODO set executionFactor to one if the Papyrus team fixes the bug. Calculate
                 * conversion factor. Conversion is necessary because Papyrus stores the
                 * y-coordinates in a very strange way. When the message starts or ends at an
                 * execution, y-coordinates must be given relative to the execution. However, these
                 * relative coordinates must be scaled as if the execution was having the height of
                 * its lifeline.
                 */
                double effectiveHeight = lifeline.getSize().y - SequenceLayoutConstants.TWENTY;
                double executionHeight = maxYPos - minYPos;
                double executionFactor = effectiveHeight / executionHeight;

                // Walk through execution's messages and adjust their position
                for (Object messObj : execution.getMessages()) {
                    if (messObj instanceof SMessage) {
                        SMessage mess = (SMessage) messObj;
                        boolean toLeft = false;
                        if (mess.getSource().getHorizontalSlot() > mess.getTarget()
                                .getHorizontalSlot()) {
                            // Message leads leftwards
                            toLeft = true;
                        }

                        ElkEdge edge = (ElkEdge) mess.getProperty(InternalProperties.ORIGIN);
                        ElkEdgeSection edgeSection = ElkGraphUtil.firstEdgeSection(edge, false, false);
                        double newXPos = lifeline.getPosition().x + execution.getPosition().x;
                        if (mess.getSource() == mess.getTarget()) {
                            // Selfloop: insert bend points
                            edgeSection.getBendPoints().get(0).setY(edgeSection.getStartY());
                            edgeSection.getBendPoints().get(1).setY(edgeSection.getEndY());
                            edgeSection.setEndX(newXPos + execution.getSize().x);
                            edgeSection.setEndY(0);
                        } else if (mess.getSource() == lifeline) {
                            if (!toLeft) {
                                newXPos += execution.getSize().x;
                            }
                            edgeSection.setStartX(newXPos);

                            // Calculate the message's height relative to the execution
                            double relHeight = mess.getSourceYPos() - minYPos;
                            if (relHeight == 0) {
                                edgeSection.setStartY(0);
                            } else {
                                edgeSection.setStartY(
                                        context.lifelineHeader + relHeight * executionFactor);
                            }
                        } else {
                            if (toLeft) {
                                newXPos += execution.getSize().x;
                            }
                            edgeSection.setEndX(newXPos);

                            // Calculate the message's height relative to the execution
                            double relHeight = mess.getTargetYPos() - minYPos;
                            if (relHeight == 0) {
                                edgeSection.setEndY(0);
                            } else {
                                edgeSection.setEndY(
                                        context.lifelineHeader + relHeight * executionFactor);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Set x position and width of an execution and check for minimum height.
     * 
     * @param executions
     *            List of {@link SequenceExecution} at the given {@link SLifeline}
     * @param parentWidth
     *            Width of the {@link SLifeline}
     */
    private void arrangeExecutions(final List<SequenceExecution> executions, final double parentWidth) {
        final double minHeight = 20;
        final double executionWidth = 16;

        // Initially set horizontal position and height of empty executions
        for (SequenceExecution execution : executions) {
            execution.getPosition().x = (parentWidth - executionWidth) / 2;
            // Give executions without messages their original height and yPos
            if (execution.getMessages().size() == 0) {
                ElkNode shapelayout = execution.getOrigin();
                execution.getPosition().y = shapelayout.getY();
                execution.getSize().y = shapelayout.getHeight();
            }
        }

        if (executions.size() > 1) {
            // reset xPos if execution is attached to another execution
            for (SequenceExecution execution : executions) {
                if (execution.getType() == SequenceExecutionType.DURATION
                        || execution.getType() == SequenceExecutionType.TIME_CONSTRAINT) {
                    continue;
                }
                
                int pos = 0;
                for (SequenceExecution otherExecution : executions) {
                    if (execution != otherExecution) {
                        if (execution.getPosition().y > otherExecution.getPosition().y
                                && execution.getPosition().y + execution.getSize().y < otherExecution
                                        .getPosition().y + otherExecution.getSize().y) {
                            pos++;
                        }
                    }
                }
                if (pos > 0) {
                    execution.getPosition().x = execution.getPosition().x + pos * executionWidth
                            / 2;
                }
            }
        }

        // Check minimum height of executions and set width
        for (SequenceExecution execution : executions) {
            if (execution.getSize().y < minHeight) {
                execution.getSize().y = minHeight;
            }
            
            if (execution.getType() == SequenceExecutionType.DURATION
                    || execution.getType() == SequenceExecutionType.TIME_CONSTRAINT) {
                
                continue;
            }
            execution.getSize().x = executionWidth;
        }
    }

    /**
     * Place the comment objects (comments, constraints) according to their calculated coordinates.
     * 
     * @param graph
     *            the Sequence Graph
     */
    private void placeComments(final SGraph graph) {
        for (SComment comment : graph.getComments()) {
            Object origin = comment.getProperty(InternalProperties.ORIGIN);
            ElkNode commentLayout = ((ElkNode) origin);
            commentLayout.setX(comment.getPosition().x);
            commentLayout.setY(comment.getPosition().y);
            if (comment.getMessage() != null) {
                // Connected comments

                // Set coordinates for the connection of the comment
                double edgeSourceXPos, edgeSourceYPos, edgeTargetXPos, edgeTargetYPos;
                String attachedElement = comment.getProperty(
                        SequenceDiagramOptions.ATTACHED_ELEMENT_TYPE);
                if (attachedElement.toLowerCase().startsWith("lifeline")
                        || attachedElement.toLowerCase().contains("execution")) {
                    
                    // Connections to lifelines or executions are drawn horizontally
                    SLifeline lifeline = comment.getLifeline();
                    edgeSourceXPos = comment.getPosition().x;
                    edgeSourceYPos = comment.getPosition().y + comment.getSize().y / 2;
                    edgeTargetXPos = lifeline.getPosition().x + lifeline.getSize().x / 2;
                    edgeTargetYPos = edgeSourceYPos;
                } else {
                    // Connections to messages are drawn vertically
                    edgeSourceXPos = comment.getPosition().x + comment.getSize().x / 2;
                    edgeTargetXPos = edgeSourceXPos;
                    ElkEdge edge = (ElkEdge) comment.getMessage().getProperty(InternalProperties.ORIGIN);
                    ElkEdgeSection edgeSection = ElkGraphUtil.firstEdgeSection(edge, false, false);
                    
                    edgeSourceYPos = comment.getPosition().y + comment.getSize().y;
                    edgeTargetYPos = (edgeSection.getEndY() + edgeSection.getStartY()) / 2;
                }

                // Apply connection coordinates to layout
                ElkEdge commentEdge = comment.getProperty(
                        InternalSequenceProperties.COMMENT_CONNECTION);
                ElkEdgeSection commentEdgeSection = ElkGraphUtil.firstEdgeSection(commentEdge, false, false);
                commentEdgeSection.setStartLocation(edgeSourceXPos, edgeSourceYPos);
                commentEdgeSection.setEndLocation(edgeTargetXPos, edgeTargetYPos);
            }
        }
    }

}
