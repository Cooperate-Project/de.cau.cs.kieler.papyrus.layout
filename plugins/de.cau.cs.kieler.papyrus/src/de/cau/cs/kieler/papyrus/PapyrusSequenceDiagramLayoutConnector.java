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
package de.cau.cs.kieler.papyrus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionLocator;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gmf.runtime.diagram.ui.editparts.AbstractBorderedShapeEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.CompartmentEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.ConnectionEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.IGraphicalEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.LabelEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.ListCompartmentEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.ResizableCompartmentEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.ShapeNodeEditPart;
import org.eclipse.gmf.runtime.diagram.ui.figures.ResizableCompartmentFigure;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramEditor;
import org.eclipse.gmf.runtime.draw2d.ui.figures.WrappingLabel;
import org.eclipse.gmf.runtime.notation.impl.EdgeImpl;
import org.eclipse.gmf.runtime.notation.impl.ShapeImpl;
import org.eclipse.papyrus.infra.ui.editor.IMultiDiagramEditor;
import org.eclipse.papyrus.infra.gmfdiag.common.editpart.IPapyrusEditPart;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Font;
import org.eclipse.ui.IWorkbenchPart;

import com.google.common.collect.BiMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import org.eclipse.elk.graph.ElkConnectableShape;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkEdgeSection;
import org.eclipse.elk.graph.ElkGraphElement;
import org.eclipse.elk.graph.ElkGraphFactory;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;
import org.eclipse.elk.core.LayoutConfigurator;
import org.eclipse.elk.core.math.ElkPadding;
import org.eclipse.elk.core.math.KVector;
import org.eclipse.elk.graph.properties.IProperty;
import org.eclipse.elk.graph.properties.IPropertyHolder;
import org.eclipse.elk.graph.properties.Property;
import org.eclipse.elk.graph.util.ElkGraphUtil;
import org.eclipse.elk.core.util.ElkUtil;
import org.eclipse.elk.core.util.Maybe;
import org.eclipse.elk.conn.gmf.GmfDiagramLayoutConnector;
import org.eclipse.elk.conn.gmf.GmfLayoutConfigurationStore;
import org.eclipse.elk.conn.gmf.IEditPartFilter;
import org.eclipse.elk.core.options.EdgeLabelPlacement;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.service.LayoutMapping;
import de.cau.cs.kieler.papyrus.sequence.properties.CoordinateSystem;
import de.cau.cs.kieler.papyrus.sequence.properties.MessageType;
import de.cau.cs.kieler.papyrus.sequence.properties.NodeType;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceArea;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceDiagramOptions;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceExecution;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceExecutionType;

/**
 * Layout manager wrapper for the Papyrus multi diagram editor.
 * 
 * @author msp original layout manager
 * @author grh adaptions for sequence diagram layout
 * @kieler.design proposed grh
 * @kieler.rating proposed yellow grh
 */
public class PapyrusSequenceDiagramLayoutConnector extends GmfDiagramLayoutConnector {

       /** editor part of the currently layouted diagram. */
    public static final IProperty<DiagramEditor> DIAGRAM_EDITOR = new Property<DiagramEditor>(
            "gmf.diagramEditor");
    
    /** Maps Papyrus node types that are basically Strings to proper node type enumeration values. */
    private static final Map<String, NodeType> PAPYRUS_NODE_TYPES = Maps.newHashMap();
    
    /** Maps Papyrus node types that are basically Strings to proper message type enumeration values. */
    private static final Map<String, MessageType> PAPYRUS_MESSAGE_TYPES = Maps.newHashMap();

    
    
    
    @Inject
    private IEditPartFilter editPartFilter;
        
    static {
        PAPYRUS_NODE_TYPES.put("Interaction_Shape", NodeType.SURROUNDING_INTERACTION);
        PAPYRUS_NODE_TYPES.put("Lifeline_Shape", NodeType.LIFELINE);
        PAPYRUS_NODE_TYPES.put("InteractionUse_Shape", NodeType.INTERACTION_USE);
        PAPYRUS_NODE_TYPES.put("CombinedFragment_Shape", NodeType.COMBINED_FRAGMENT);
        PAPYRUS_NODE_TYPES.put("InteractionOperand_Shape", NodeType.INTERACTION_OPERAND);
        PAPYRUS_NODE_TYPES.put("ActionExecutionSpecification_Shape", NodeType.ACTION_EXEC_SPECIFICATION);
        PAPYRUS_NODE_TYPES.put("BehaviorExecutionSpecification_Shape", NodeType.BEHAVIOUR_EXEC_SPECIFICATION);
        PAPYRUS_NODE_TYPES.put("Comment_Shape", NodeType.COMMENT);
        PAPYRUS_NODE_TYPES.put("Constraint_Shape", NodeType.CONSTRAINT);
        PAPYRUS_NODE_TYPES.put("DestructionOccurrenceSpecification_Shape", NodeType.DESTRUCTION_EVENT);
        PAPYRUS_NODE_TYPES.put("TimeConstraint_Shape", NodeType.TIME_CONSTRAINT);
        PAPYRUS_NODE_TYPES.put("TimeObservation_Shape", NodeType.TIME_OBSERVATION);
        PAPYRUS_NODE_TYPES.put("DurationConstraint_Shape", NodeType.DURATION_CONSTRAINT);
        PAPYRUS_NODE_TYPES.put("DurationObservation_Shape", NodeType.DURATION_OBSERVATION);
        
        PAPYRUS_MESSAGE_TYPES.put("Message_SynchEdge", MessageType.SYNCHRONOUS);
        PAPYRUS_MESSAGE_TYPES.put("Message_AsynchEdge", MessageType.ASYNCHRONOUS);
        PAPYRUS_MESSAGE_TYPES.put("Message_ReplyEdge", MessageType.REPLY);
        PAPYRUS_MESSAGE_TYPES.put("Message_CreateEdge", MessageType.CREATE);
        PAPYRUS_MESSAGE_TYPES.put("Message_DeleteEdge", MessageType.DELETE);
        PAPYRUS_MESSAGE_TYPES.put("Message_LostEdge", MessageType.LOST);
        PAPYRUS_MESSAGE_TYPES.put("Message_FoundEdge", MessageType.FOUND);
    }

    


    /**
     * {@inheritDoc}
     */
    @Override
    public LayoutMapping buildLayoutGraph(final IWorkbenchPart workbenchPart,
            final Object diagramPart) {
        
        if (workbenchPart instanceof IMultiDiagramEditor) {
            IWorkbenchPart part = ((IMultiDiagramEditor) workbenchPart).getActiveEditor();
            if (part.getClass().getSimpleName().equals("UmlSequenceDiagramForMultiEditor")) {
                // Build KGraph in a different way if it is a sequence diagram
                return buildSequenceLayoutGraph(part, diagramPart);
            }
            LayoutMapping mapping = super.buildLayoutGraph(part, diagramPart);
            return mapping;

        } else {
            return super.buildLayoutGraph(workbenchPart, diagramPart);
        }
    }

    /**
     * Special method to build a layoutGraph when the diagram is a sequence diagram.
     * 
     * @param workbenchPart
     *            the workbenchPart
     * @param diagramPart
     *            the diagramPart
     * @return a layoutGraph
     */
    protected LayoutMapping buildSequenceLayoutGraph(
            final IWorkbenchPart workbenchPart, final Object diagramPart) {

     // get the diagram editor part
        DiagramEditor diagramEditor = getDiagramEditor(workbenchPart);

        // choose the layout root edit part
        IGraphicalEditPart layoutRootPart = null;
        List<ShapeNodeEditPart> selectedParts = null;
        if (diagramPart instanceof ShapeNodeEditPart || diagramPart instanceof DiagramEditPart) {
            layoutRootPart = (IGraphicalEditPart) diagramPart;
        } else if (diagramPart instanceof IGraphicalEditPart) {
            EditPart tgEditPart = ((IGraphicalEditPart) diagramPart).getTopGraphicEditPart();
            if (tgEditPart instanceof ShapeNodeEditPart) {
                layoutRootPart = (IGraphicalEditPart) tgEditPart;
            }
        } else if (diagramPart instanceof Collection) {
            Collection<?> selection = (Collection<?>) diagramPart;
            // determine the layout root part from the selection
            for (Object object : selection) {
                if (object instanceof IGraphicalEditPart) {
                    if (layoutRootPart != null) {
                        EditPart parent = commonParent(layoutRootPart, (EditPart) object);
                        if (parent != null && !(parent instanceof RootEditPart)) {
                            layoutRootPart = (IGraphicalEditPart) parent;
                        }
                    } else if (!(object instanceof ConnectionEditPart)) {
                        layoutRootPart = (IGraphicalEditPart) object;
                    }
                }
            }
            // build a list of edit parts that shall be layouted completely
            if (layoutRootPart != null) {
                selectedParts = new ArrayList<ShapeNodeEditPart>(selection.size());
                for (Object object : selection) {
                    if (object instanceof IGraphicalEditPart) {
                        EditPart editPart = (EditPart) object;
                        while (editPart != null && editPart.getParent() != layoutRootPart) {
                            editPart = editPart.getParent();
                        }
                        if (editPart instanceof ShapeNodeEditPart
                                && editPartFilter.filter(editPart) && !selectedParts.contains(editPart)) {
                            selectedParts.add((ShapeNodeEditPart) editPart);
                        }
                    }
                }
            }
        }
        if (layoutRootPart == null && diagramEditor != null) {
            layoutRootPart = diagramEditor.getDiagramEditPart();
        }
        if (layoutRootPart == null) {
            throw new IllegalArgumentException(
                    "Not supported by this layout connector: Workbench part " + workbenchPart
                            + ", Edit part " + diagramPart);
        }

        
        
     // create the mapping
        LayoutMapping mapping = buildSequenceLayoutGraph(layoutRootPart, selectedParts, workbenchPart);

        return mapping;
    }

    /**
     * Creates the actual mapping given an edit part which functions as the root for the layout.
     * 
     * @param layoutRootPart
     *            the layout root edit part
     * @return a layout graph mapping
     */
    protected LayoutMapping buildSequenceLayoutGraph(
            final IGraphicalEditPart layoutRootPart,
            final List<ShapeNodeEditPart> selection, final IWorkbenchPart workbenchPart) {
        
        LayoutMapping mapping = new LayoutMapping((IWorkbenchPart) workbenchPart);
        mapping.setProperty(CONNECTIONS, new LinkedList<ConnectionEditPart>());

        // set the parent element
        mapping.setParentElement(layoutRootPart);

        // find the diagram edit part
        mapping.setProperty(DIAGRAM_EDIT_PART, getDiagramEditPart(layoutRootPart));
        
        ElkNode topNode = ElkGraphUtil.createNode(null);
        
        topNode.setProperty(SequenceDiagramOptions.COORDINATE_SYSTEM, CoordinateSystem.PAPYRUS);
        
        Rectangle rootBounds = layoutRootPart.getFigure().getBounds();
        if (layoutRootPart instanceof DiagramEditPart) {
            // start with the whole diagram as root for layout
            String labelText = ((DiagramEditPart) layoutRootPart).getDiagramView().getName();
            if (labelText.length() > 0) {
                ElkLabel label = ElkGraphUtil.createLabel(topNode);
                label.setText(labelText);
            }
        } else {
            // start with a specific node as root for layout
            topNode.setX(rootBounds.x);
            topNode.setY(rootBounds.y);
        }
        topNode.setWidth(rootBounds.width);
        topNode.setHeight(rootBounds.height);
        mapping.getGraphMap().put(topNode, layoutRootPart);
        mapping.setLayoutGraph(topNode);

        // traverse the children of the layout root part
        buildSequenceLayoutGraphRecursively(mapping, layoutRootPart, topNode, layoutRootPart);
        // transform all connections in the selected area
        processSequenceConnections(mapping);

        // copy annotations from KShapeLayout to VolatileLayoutConfig
        copyAnnotations(mapping, topNode);

        return mapping;
    }

    /**
     * Copy the annotations to the static config.
     * 
     * @param mapping
     *            the layout mapping
     * @param topNode
     *            the layout root part
     */
    private void copyAnnotations(final LayoutMapping mapping,
            final ElkNode topNode) {

        // Copy the executions
        List<SequenceExecution> executions = topNode.getProperty(
                SequenceDiagramOptions.EXECUTIONS);
        if (executions != null) {
            topNode.setProperty(SequenceDiagramOptions.EXECUTIONS, executions);
        } else {
            for (ElkNode node : topNode.getChildren()) {
                copyAnnotations(mapping, node);
            }
        }

        // Copy the information to which element a comment is attached to
        List<Object> attachedTo = topNode.getProperty(SequenceDiagramOptions.ATTACHED_OBJECTS);
        if (attachedTo != null) {
            List<Object> attTo = new LinkedList<Object>();
            BiMap<Object, ElkGraphElement> inverseGraphMap = mapping.getGraphMap().inverse();
            for (Object att : attachedTo) {
                attTo.add(inverseGraphMap.get(att));
            }
            topNode.setProperty(SequenceDiagramOptions.ATTACHED_OBJECTS, attTo);
        }

        String attachedElement = topNode.getProperty(SequenceDiagramOptions.ATTACHED_ELEMENT_TYPE);
        if (attachedElement != null) {
            topNode.setProperty(SequenceDiagramOptions.ATTACHED_ELEMENT_TYPE, attachedElement);
        }
    }

    /**
     * Recursively builds a layout graph by analyzing the children of the given edit part.
     * 
     * @param mapping
     *            the layout mapping
     * @param parentEditPart
     *            the parent edit part of the current elements
     * @param parentLayoutNode
     *            the corresponding ElkNode
     * @param currentEditPart
     *            the currently analyzed edit part
     */
    private void buildSequenceLayoutGraphRecursively(final LayoutMapping mapping,
            final IGraphicalEditPart parentEditPart, final ElkNode parentLayoutNode,
            final IGraphicalEditPart currentEditPart) {
        
        Maybe<ElkPadding> kinsets = new Maybe<>();

        parentLayoutNode.setProperty(SequenceDiagramOptions.AREAS,
                new LinkedList<SequenceArea>());

        // iterate through the children of the element
        for (Object obj : currentEditPart.getChildren()) {
            // check visibility of the child
            if (obj instanceof IGraphicalEditPart) {
                IFigure figure = ((IGraphicalEditPart) obj).getFigure();
                if (!figure.isVisible()) {
                    continue;
                }
            }

            // process a compartment, which may contain other elements
            if (obj instanceof ResizableCompartmentEditPart
                    && ((CompartmentEditPart) obj).getChildren().size() > 0) {
                CompartmentEditPart compartment = (CompartmentEditPart) obj;
                if (editPartFilter.filter(compartment)) {
                    boolean compExp = true;
                    IFigure compartmentFigure = compartment.getFigure();
                    if (compartmentFigure instanceof ResizableCompartmentFigure) {
                        ResizableCompartmentFigure resizCompFigure = 
                                (ResizableCompartmentFigure) compartmentFigure;
                        // check whether the compartment is collapsed
                        compExp = resizCompFigure.isExpanded();
                    }

                    if (compExp) {
                        buildSequenceLayoutGraphRecursively(mapping, parentEditPart,
                                parentLayoutNode, compartment);
                    }
                }

                // process a node, which may be a parent of ports, compartments, or other nodes
            } else if (obj instanceof ShapeNodeEditPart) {
                ShapeNodeEditPart childNodeEditPart = (ShapeNodeEditPart) obj;
                if (editPartFilter.filter(childNodeEditPart)) {
                    createSequenceNode(mapping, childNodeEditPart, parentEditPart, parentLayoutNode,
                            kinsets);
                }

                // process a label of the current node
            } else if (obj instanceof IGraphicalEditPart) {
                createSequenceNodeLabel(mapping, (IGraphicalEditPart) obj, parentEditPart,
                        parentLayoutNode);
            }
        }
    }

    /**
     * Create a node while building the layout graph.
     * 
     * @param mapping
     *            the layout mapping
     * @param nodeEditPart
     *            the node edit part
     * @param parentEditPart
     *            the parent node edit part that contains the current node
     * @param parentElkNode
     *            the corresponding parent layout node
     * @param elkinsets
     *            reference parameter for insets; the insets are calculated if this has not been
     *            done before
     */
    private void createSequenceNode(final LayoutMapping mapping,
            final ShapeNodeEditPart nodeEditPart, final IGraphicalEditPart parentEditPart,
            final ElkNode parentElkNode, final Maybe<ElkPadding> elkinsets) {

        IFigure nodeFigure = nodeEditPart.getFigure();
        ElkNode childLayoutNode = ElkGraphUtil.createNode(parentElkNode);
        

        // Add node type information to the ElkNode
        NodeType nodeType = null;
        if (nodeEditPart.getModel() instanceof ShapeImpl) {
            ShapeImpl impl = (ShapeImpl) nodeEditPart.getModel();
            nodeType = PAPYRUS_NODE_TYPES.get(impl.getType());
            childLayoutNode.setProperty(SequenceDiagramOptions.NODE_TYPE, nodeType);
        }

        // set location and size
        Rectangle childBounds = getAbsoluteBounds(nodeFigure);
        Rectangle containerBounds = getAbsoluteBounds(nodeFigure.getParent());

        childLayoutNode.setX(childBounds.x - containerBounds.x);
        childLayoutNode.setY(childBounds.y - containerBounds.y);
        childLayoutNode.setWidth(childBounds.width);
        childLayoutNode.setHeight(childBounds.height);

            // determine minimal size of the node
        try {
            Dimension minSize = nodeFigure.getMinimumSize();
            childLayoutNode.setProperty(CoreOptions.NODE_SIZE_MINIMUM, new KVector(minSize.width, minSize.height));
        } catch (SWTException exception) {
            // ignore exception and leave the default minimal size
        }

        // set insets if not yet defined
        if (elkinsets.get() == null) {
            Insets insets = calcSpecificInsets(parentEditPart.getFigure(), nodeFigure);
            ElkPadding ei = new ElkPadding(insets.top, insets.right, insets.bottom, insets.left);
            childLayoutNode.setProperty(CoreOptions.PADDING, ei);
            elkinsets.set(ei);
        }

        parentElkNode.getChildren().add(childLayoutNode);
        mapping.getGraphMap().put(childLayoutNode, nodeEditPart);

        // process the child as new current edit part
        if (nodeType == NodeType.SURROUNDING_INTERACTION) {
            buildSequenceLayoutGraphRecursively(mapping, nodeEditPart, childLayoutNode, nodeEditPart);
        } else if (nodeType == NodeType.LIFELINE) {
            handleLifeline(mapping, nodeEditPart, childLayoutNode);
        } else if (nodeType == NodeType.INTERACTION_USE || nodeType == NodeType.COMBINED_FRAGMENT) {
            // Handle areas such as interactionUse, combinedFragment and interactionOperand
            handleAreas(mapping, nodeEditPart, parentElkNode, childLayoutNode);
        } else if (nodeType == NodeType.COMMENT
                || nodeType == NodeType.CONSTRAINT
                || nodeType == NodeType.DURATION_OBSERVATION
                || nodeType == NodeType.TIME_OBSERVATION) {
            
            handleComments(mapping, nodeEditPart, childLayoutNode);
        }
        // store all the connections to process them later
        addConnections(mapping, nodeEditPart);
    }

    /**
     * Handle a node that represents a lifeline. Especially handle sub-nodes like execution
     * specifications.
     * 
     * @param mapping
     *            the layout mapping
     * @param nodeEditPart
     *            the current node edit part
     * @param layoutNode
     *            the created ElkNode
     */
    private void handleLifeline(final LayoutMapping mapping,
            final ShapeNodeEditPart nodeEditPart, final ElkNode layoutNode) {
                
        // handle label
        IGraphicalEditPart labelObj = nodeEditPart.getChildBySemanticHint("Lifeline_NameLabel");
        createSequenceNodeLabel(mapping, labelObj, nodeEditPart, layoutNode);
        
        // handle subnodes like execution specifications
        List<SequenceExecution> executions = new LinkedList<SequenceExecution>();
        for (Object child : nodeEditPart.getChildren()) {
            if (child instanceof ShapeNodeEditPart) {
                ShapeNodeEditPart childEditPart = (ShapeNodeEditPart) child;
                NodeType subNodeType = null;
                if (childEditPart.getModel() instanceof ShapeImpl) {
                    ShapeImpl shape = (ShapeImpl) childEditPart.getModel();
                    subNodeType = PAPYRUS_NODE_TYPES.get(shape.getType());
                }
                IFigure subNodeFigure = childEditPart.getFigure();
                ElkNode subNodeLayout = ElkGraphUtil.createNode(layoutNode);

                mapping.getGraphMap().put(subNodeLayout, childEditPart);

                // Copy layout information
                Rectangle subNodeBounds = getAbsoluteBounds(subNodeFigure);
                Rectangle subNodeContainerBounds = getAbsoluteBounds(subNodeFigure.getParent());
                
                subNodeLayout.setX(subNodeBounds.x - subNodeContainerBounds.x);
                subNodeLayout.setY(subNodeBounds.y - subNodeContainerBounds.y);
                subNodeLayout.setWidth(subNodeBounds.width);
                subNodeLayout.setHeight(subNodeBounds.height);

                if (subNodeType == NodeType.BEHAVIOUR_EXEC_SPECIFICATION
                        || subNodeType == NodeType.ACTION_EXEC_SPECIFICATION
                        || subNodeType == NodeType.TIME_CONSTRAINT
                        || subNodeType == NodeType.DURATION_CONSTRAINT) {
                    
                    // Create Execution Object (which handles all these types) and initialize it
                    createExecution(mapping, nodeEditPart, executions, childEditPart, subNodeType,
                            subNodeLayout);
                } else if (subNodeType == NodeType.DESTRUCTION_EVENT) {
                    // Subnode is destruction event
                    subNodeLayout.setProperty(
                            SequenceDiagramOptions.DESTRUCTION_NODE, subNodeLayout);
                }
            }
        }
        if (executions.size() > 0) {
            layoutNode.setProperty(SequenceDiagramOptions.EXECUTIONS,
                    executions);
        }
    }

    /**
     * Handle nodes that represent area-like objects (interaction use, combined fragments).
     * 
     * @param mapping
     *            the layout mapping
     * @param nodeEditPart
     *            the current node edit part
     * @param parentElkNode
     *            the parent ElkNode
     * @param layoutNode
     *            the created ElkNode
     */
    private void handleAreas(final LayoutMapping mapping,
            final ShapeNodeEditPart nodeEditPart, final ElkNode parentElkNode, final ElkNode layoutNode) {
        IFigure nodeFigure = nodeEditPart.getFigure();
        Rectangle bounds = getAbsoluteBounds(nodeFigure);
        Rectangle parentBounds = getAbsoluteBounds(nodeFigure.getParent());

        SequenceArea area = new SequenceArea(layoutNode);

        // Copy layout information
        area.getPosition().x = bounds.x - parentBounds.x;
        area.getPosition().y = bounds.y - parentBounds.y;
        area.getSize().x = bounds.width;
        area.getSize().y = bounds.height;

        List<SequenceArea> areas = parentElkNode.getProperty(SequenceDiagramOptions.AREAS);
        areas.add(area);
        parentElkNode.setProperty(SequenceDiagramOptions.AREAS, areas);
        
        // Get coordinates of the interaction operands if existing
        for (Object child : nodeEditPart.getChildren()) {
            if (child instanceof ListCompartmentEditPart) {
                ListCompartmentEditPart lcEditPart = (ListCompartmentEditPart) child;
                for (Object childObj : lcEditPart.getChildren()) {
                    if (childObj instanceof AbstractBorderedShapeEditPart) {
                        AbstractBorderedShapeEditPart ioEditPart = 
                                (AbstractBorderedShapeEditPart) childObj;
                        Rectangle ioBounds = getAbsoluteBounds(ioEditPart.getFigure());
                        ElkNode areaNode = ElkGraphUtil.createNode(parentElkNode);
                        mapping.getGraphMap().put(areaNode, ioEditPart);
                        SequenceArea subArea = new SequenceArea(areaNode);
                        // Copy layout information
                        subArea.getPosition().x = ioBounds.x - parentBounds.x;
                        subArea.getPosition().y = ioBounds.y - parentBounds.y;
                        subArea.getSize().x = ioBounds.width;
                        subArea.getSize().y = ioBounds.height;
                        area.getSubAreas().add(subArea);
                    }
                }
            }
        }
    }

    /**
     * Handle nodes that represent comments, constraints or observations.
     * 
     * @param mapping
     *            the layout mapping
     * @param nodeEditPart
     *            the current node edit part
     * @param nodeLayout
     *            the node layout
     */
    private void handleComments(final LayoutMapping mapping,
            final ShapeNodeEditPart nodeEditPart, final ElkNode nodeLayout) {
        // FIXME time observations are not detected properly

        // Handle comments, constraints and observations
        List<Object> attachedTo = new LinkedList<Object>();
        // Process connections of the object
        for (Object connObj : nodeEditPart.getSourceConnections()) {
            if (connObj instanceof ConnectionEditPart) {
                ConnectionEditPart connedit = (ConnectionEditPart) connObj;
                mapping.getProperty(CONNECTIONS).add(connedit);
                nodeLayout.setProperty(SequenceDiagramOptions.ATTACHED_ELEMENT_TYPE,
                        connedit.getTarget().getClass().getSimpleName());
                
                // If target is lifeline, attach to the nearest message
                if (connedit.getTarget() instanceof ShapeNodeEditPart) {
                    float yPos = connedit.getConnectionFigure().getPoints().getLastPoint().y();
                    ConnectionEditPart nearestMessage = findMessageBelowPoint(
                            (ShapeNodeEditPart) connedit.getTarget(), connedit, yPos);
                    if (nearestMessage != null) {
                        attachedTo.add(nearestMessage);
                    }
                } else {
                    // If target already is a message, attach to that message
                    attachedTo.add(connedit.getTarget());
                }
            }
        }

        // If the object is connected to any other object, attach property with connected
        // objects
        if (attachedTo.size() > 0) {
            nodeLayout.setProperty(SequenceDiagramOptions.ATTACHED_OBJECTS, attachedTo);
        }
    }

    /**
     * Creates a SequenceExecution Object and cares about the initialization.
     * 
     * @param mapping
     *            the layout mapping
     * @param lifelineEditPart
     *            the edit part of the current lifeline
     * @param executions
     *            the list of executions at the current lifeline
     * @param childEditPart
     *            the executions edit part
     * @param nodeType
     *            the type of the node
     * @param executionNode
     *            the ElkNode representation of the execution
     */
    private void createExecution(final LayoutMapping mapping,
            final ShapeNodeEditPart lifelineEditPart, final List<SequenceExecution> executions,
            final ShapeNodeEditPart childEditPart, final NodeType nodeType, final ElkNode executionNode) {

        
        ElkNode executionLayout = ElkGraphUtil.createNode(executionNode);
        IFigure executionFigure = childEditPart.getFigure();
        Rectangle executionBounds = getAbsoluteBounds(executionFigure);

        SequenceExecution execution = new SequenceExecution(executionNode);

        if (nodeType == NodeType.BEHAVIOUR_EXEC_SPECIFICATION
                || nodeType == NodeType.ACTION_EXEC_SPECIFICATION) {

            executionNode.setProperty(SequenceDiagramOptions.NODE_TYPE,
                    nodeType);
            execution.setType(SequenceExecutionType.EXECUTION);
        } else if (nodeType == NodeType.DURATION_CONSTRAINT) {
            executionNode.setProperty(SequenceDiagramOptions.NODE_TYPE,
                    nodeType);
            execution.setType(SequenceExecutionType.DURATION);
        } else if (nodeType == NodeType.TIME_CONSTRAINT) {
            executionNode.setProperty(SequenceDiagramOptions.NODE_TYPE,
                    nodeType);
            execution.setType(SequenceExecutionType.TIME_CONSTRAINT);
        }

        // Walk through the connected messages
        for (Object targetConn : childEditPart.getTargetConnections()) {
            if (targetConn instanceof ConnectionEditPart) {
                ConnectionEditPart connectionEditPart = (ConnectionEditPart) targetConn;
                mapping.getProperty(CONNECTIONS).add(connectionEditPart);

                execution.addMessage(connectionEditPart);
            }
        }
        for (Object sourceConn : childEditPart.getSourceConnections()) {
            if (sourceConn instanceof ConnectionEditPart) {
                ConnectionEditPart connectionEditPart = (ConnectionEditPart) sourceConn;

                execution.addMessage(connectionEditPart);
            }
        }
        executions.add(execution);
        executionLayout.setProperty(PapyrusProperties.EXECUTION, execution);

        // Add messages to duration if their send/receive event is in the area of the
        // duration
        if (nodeType == NodeType.DURATION_CONSTRAINT || nodeType == NodeType.TIME_CONSTRAINT) {
            // get position of messages and compare to duration
            int from = executionBounds.y();
            int to = executionBounds.y() + executionBounds.height();
            for (Object connObj : lifelineEditPart.getSourceConnections()) {
                if (connObj instanceof ConnectionEditPart) {
                    ConnectionEditPart conn = (ConnectionEditPart) connObj;
                    int point = conn.getConnectionFigure().getPoints().getFirstPoint().y;
                    if (point <= to + 2 && point >= from - 2) {
                        execution.addMessage(conn);
                    }
                }
            }
            for (Object connObj : lifelineEditPart.getTargetConnections()) {
                if (connObj instanceof ConnectionEditPart) {
                    ConnectionEditPart conn = (ConnectionEditPart) connObj;
                    int point = conn.getConnectionFigure().getPoints().getLastPoint().y;
                    if (point <= to + 2 && point >= from - 2) {
                        execution.addMessage(conn);
                    }
                }
            }
        }
    }

    /**
     * Finds the nearest message that is located below a given point or the lowermost message if
     * there is no message below the given point.
     * 
     * @param lifeline
     *            the lifeline, whose messages are searched
     * @param skipConnection
     *            the connection that is not considered in the search
     * @param yPos
     *            the point
     * @return the ConnectionEditPart of the searched message
     */
    private ConnectionEditPart findMessageBelowPoint(final ShapeNodeEditPart lifeline,
            final ConnectionEditPart skipConnection, final float yPos) {
        float minDiff = Float.MAX_VALUE;
        float low = 0;
        ConnectionEditPart next = null;
        ConnectionEditPart lowest = null;
        for (Object connection : lifeline.getSourceConnections()) {
            if (connection instanceof ConnectionEditPart) {
                ConnectionEditPart connect = (ConnectionEditPart) connection;
                if (connect == skipConnection) {
                    continue;
                }
                float connectionYPos = connect.getConnectionFigure().getPoints().getFirstPoint()
                        .y();

                if (connectionYPos >= yPos) {
                    // Message is below comment
                    float currentDiff = Math.abs(connectionYPos - yPos);
                    if (currentDiff < minDiff) {
                        minDiff = currentDiff;
                        next = connect;
                    }
                } else {
                    // Message is above comment
                    if (connectionYPos > low) {
                        low = connectionYPos;
                        lowest = connect;
                    }
                }
            }
        }
        for (Object connection : lifeline.getTargetConnections()) {
            if (connection instanceof ConnectionEditPart) {
                ConnectionEditPart connect = (ConnectionEditPart) connection;
                if (connect == skipConnection) {
                    continue;
                }
                float connectionYPos = connect.getConnectionFigure().getPoints().getLastPoint().y();

                if (connectionYPos > yPos) {
                    // Message is below comment
                    float currentDiff = Math.abs(connectionYPos - yPos);
                    if (currentDiff < minDiff) {
                        minDiff = currentDiff;
                        next = connect;
                    }
                } else {
                    // Message is above comment
                    if (connectionYPos > low) {
                        low = connectionYPos;
                        lowest = connect;
                    }
                }
            }
        }
        if (next != null) {
            return next;
        } else {
            return lowest;
        }
    }

    /**
     * Create a node label while building the layout graph.
     * 
     * @param mapping
     *            the layout mapping
     * @param labelEditPart
     *            the label edit part
     * @param nodeEditPart
     *            the parent node edit part
     * @param knode
     *            the layout node for which the label is set
     */
    private void createSequenceNodeLabel(final LayoutMapping mapping,
            final IGraphicalEditPart labelEditPart, final IGraphicalEditPart nodeEditPart,
            final ElkNode knode) {
        IFigure labelFigure = labelEditPart.getFigure();
        String text = null;
        Font font = null;
        if (labelFigure instanceof WrappingLabel) {
            WrappingLabel wrappingLabel = (WrappingLabel) labelFigure;
            text = wrappingLabel.getText();
            font = wrappingLabel.getFont();
        } else if (labelFigure instanceof Label) {
            Label label = (Label) labelFigure;
            text = label.getText();
            font = label.getFont();
        }
        if (text != null) {
            ElkLabel label = ElkGraphUtil.createLabel(knode);
            label.setText(text);
            mapping.getGraphMap().put(label, labelEditPart);
            
            Rectangle labelBounds = getAbsoluteBounds(labelFigure);
            Rectangle nodeBounds = getAbsoluteBounds(nodeEditPart.getFigure());
            label.setX(labelBounds.x - nodeBounds.x);
            label.setY(labelBounds.y - nodeBounds.y);
            try {
                Dimension size = labelFigure.getPreferredSize();
                label.setWidth(size.width);
                label.setHeight(size.height);
                if (font != null && !font.isDisposed()) {
                    label.setProperty(CoreOptions.FONT_NAME, font.getFontData()[0].getName());
                    label.setProperty(CoreOptions.FONT_SIZE, font.getFontData()[0].getHeight());
                }
            } catch (SWTException exception) {
                // ignore exception and leave the label size to (0, 0)
            }
            
        }
    }


    /**
     * Creates new edges and takes care of the labels for each connection identified in the
     * {@code buildLayoutGraphRecursively} method.
     * 
     * @param mapping
     *            the layout mapping
     */
    protected void processSequenceConnections(final LayoutMapping mapping) {
        Map<EReference, ElkEdge> reference2EdgeMap = new HashMap<EReference, ElkEdge>();
        
        for (ConnectionEditPart connection : mapping.getProperty(CONNECTIONS)) {
            boolean isOppositeEdge = false;
            EdgeLabelPlacement edgeLabelPlacement = EdgeLabelPlacement.UNDEFINED;
            ElkEdge edge;

            // check whether the edge belongs to an Ecore reference, which may have opposites
            EObject modelObject = connection.getNotationView().getElement();
            if (modelObject instanceof EReference) {
                EReference reference = (EReference) modelObject;
                edge = reference2EdgeMap.get(reference.getEOpposite());
                if (edge != null) {
                    edgeLabelPlacement = EdgeLabelPlacement.TAIL;
                    isOppositeEdge = true;
                } else {
                    edge = ElkGraphUtil.createEdge(null);
                    reference2EdgeMap.put(reference, edge);
                }
            } else {
                edge = ElkGraphUtil.createEdge(null);
            }

            if (connection.getModel() instanceof EdgeImpl) {
                EdgeImpl impl = (EdgeImpl) connection.getModel();
                edge.setProperty(SequenceDiagramOptions.MESSAGE_TYPE,
                        PAPYRUS_MESSAGE_TYPES.get(impl.getType()));
            }

            BiMap<Object, ElkGraphElement> inverseGraphMap = mapping.getGraphMap().inverse();

            // find a proper source node and source port
            ElkGraphElement sourceElem;
            EditPart sourceObj = connection.getSource();
            
            if (sourceObj instanceof ConnectionEditPart) {
                sourceElem = inverseGraphMap.get(((ConnectionEditPart) sourceObj).getSource());
                if (sourceElem == null) {
                    sourceElem = inverseGraphMap.get(((ConnectionEditPart) sourceObj).getTarget());
                }
            } else {
                sourceElem = inverseGraphMap.get(sourceObj);
            }
            
            
            ElkConnectableShape sourceShape = null;
            ElkPort sourcePort = null;
            ElkNode sourceNode = null;
            if (sourceElem instanceof ElkNode) {
                sourceNode = (ElkNode) sourceElem;
                NodeType nodeType = sourceNode.getProperty(
                        SequenceDiagramOptions.NODE_TYPE);
                
                if (nodeType == NodeType.BEHAVIOUR_EXEC_SPECIFICATION
                        || nodeType == NodeType.ACTION_EXEC_SPECIFICATION
                        || nodeType == NodeType.DURATION_CONSTRAINT) {
                    
                    sourceNode = (ElkNode) inverseGraphMap.get(sourceObj.getParent());
                }
                sourceShape = sourceNode;
            } else if (sourceElem instanceof ElkPort) {
                sourcePort = (ElkPort) sourceElem;
                sourceNode = sourcePort.getParent();
                sourceShape = sourcePort;
            } else {
                continue;
            }

            // find a proper target node and target port
            ElkGraphElement targetElem;
            EditPart targetObj = connection.getTarget();
            targetElem = inverseGraphMap.get(targetObj);
            ElkNode targetNode = null;
            ElkPort targetPort = null;
            ElkConnectableShape targetShape = null;
            
            if (targetElem instanceof ElkNode) {
                targetNode = (ElkNode) targetElem;
                NodeType nodeType = targetNode.getProperty(
                        SequenceDiagramOptions.NODE_TYPE);
                
                if (nodeType == NodeType.BEHAVIOUR_EXEC_SPECIFICATION
                        || nodeType == NodeType.ACTION_EXEC_SPECIFICATION
                        || nodeType == NodeType.DURATION_CONSTRAINT) {
                    
                    targetNode = (ElkNode) inverseGraphMap.get(targetObj.getParent());
                }
                targetShape = targetNode;
            } else if (targetElem instanceof ElkPort) {
                targetPort = (ElkPort) targetElem;
                targetNode = targetPort.getParent();
                targetShape = targetPort;
            } else if (targetElem instanceof ElkEdge) {
//                // Handle edges that have edges as target
//                ElkEdge targetEdge = (ElkEdge) targetElem;
//                edge.getS
//                edge.setSource(sourceNode);
//                // Since ElkEdges cannot have an edge as target, use its target node (this doesn't
//                // matter for the results)
//
//                edge.setTarget(targetEdge.getTarget());
//                graphMap.put(edge, connection);
                continue;
            } else {
                continue;
            }

            // calculate offset for edge and label coordinates
            ElkNode edgeContainment = ElkGraphUtil.findLowestCommonAncestor(sourceNode, targetNode);
            
            KVector offset = new KVector();
            ElkUtil.toAbsolute(offset, edgeContainment);


            if (!isOppositeEdge) {
                // set source and target
                edge.getSources().add(sourceShape);
                edge.getTargets().add(targetShape);
                
                // now that source and target are set, put the edge into the graph
                edgeContainment.getContainedEdges().add(edge);

                mapping.getGraphMap().put(edge, connection);

                // store the current coordinates of the edge
                setEdgeLayout(edge, connection, offset);
            

                List<SequenceExecution> targetprops = targetNode
                        .getProperty(SequenceDiagramOptions.EXECUTIONS);
                if (targetprops != null) {
                    for (SequenceExecution targetprop : targetprops) {
                        // replace ConnectionEditPart by its ElkEdge in execution
                        if (targetprop.getMessages().remove(connection)) {
                            targetprop.addMessage(edge);
                        }
                    }
                }
                
                List<SequenceExecution> sourceProps = sourceNode
                        .getProperty(SequenceDiagramOptions.EXECUTIONS);
                if (sourceProps != null) {
                    for (SequenceExecution sourceprop : sourceProps) {
                        // replace ConnectionEditPart by its ElkEdge in execution
                        if (sourceprop.getMessages().remove(connection)) {
                            sourceprop.addMessage(edge);
                        }
                    }
                }

                // store the current coordinates of the edge
                setEdgeLayout(edge, connection, offset);
            }

            // process edge labels
            processSequenceEdgeLabels(mapping, connection, edge, edgeLabelPlacement, offset);
        }
    }



    /**
     * Process the labels of an edge.
     * 
     * @param mapping
     *            the layout mapping
     * @param connection
     *            the connection edit part
     * @param edge
     *            the layout edge
     * @param placement
     *            predefined placement for all labels, or {@code UNDEFINED} if the placement shall
     *            be derived from the edit part
     * @param offset
     *            the offset for coordinates
     */
    private void processSequenceEdgeLabels(final LayoutMapping mapping,
            final ConnectionEditPart connection, final ElkEdge edge,
            final EdgeLabelPlacement placement, final KVector offset) {
        /*
         * ars: source and target is exchanged when defining it in the gmfgen file. So if Emma sets
         * a label to be placed as target on a connection, then the label will show up next to the
         * source node in the diagram editor. So correct it here, very ugly.
         */
        for (Object obj : connection.getChildren()) {
            if (obj instanceof LabelEditPart) {
                LabelEditPart labelEditPart = (LabelEditPart) obj;
                IFigure labelFigure = labelEditPart.getFigure();

                // Check if the label is visible in the first place
                if (labelFigure != null && !labelFigure.isVisible()) {
                    continue;
                }

                Rectangle labelBounds = getAbsoluteBounds(labelFigure);
                String labelText = null;
                Dimension iconBounds = null;

                if (labelFigure instanceof WrappingLabel) {
                    WrappingLabel wrappingLabel = (WrappingLabel) labelFigure;
                    labelText = wrappingLabel.getText();
                    if (wrappingLabel.getIcon() != null) {
                        iconBounds = new Dimension();
                        iconBounds.width = wrappingLabel.getIcon().getBounds().width
                                + wrappingLabel.getIconTextGap();
                        iconBounds.height = wrappingLabel.getIcon().getBounds().height;
                        labelText = "O " + labelText;
                    }
                } else if (labelFigure instanceof Label) {
                    Label label = (Label) labelFigure;
                    labelText = label.getText();
                    if (label.getIcon() != null) {
                        iconBounds = label.getIconBounds().getSize();
                        iconBounds.width += label.getIconTextGap();
                        labelText = "O " + labelText;
                    }
                }

                if (labelText != null && labelText.length() > 0) {
                    ElkLabel labelLayout = ElkGraphUtil.createLabel(edge);
                    
                    if (placement == EdgeLabelPlacement.UNDEFINED) {
                        switch (labelEditPart.getKeyPoint()) {
                        case ConnectionLocator.SOURCE:
                            labelLayout.setProperty(CoreOptions.EDGE_LABELS_PLACEMENT,
                                    EdgeLabelPlacement.HEAD);
                            break;
                        case ConnectionLocator.MIDDLE:
                            labelLayout.setProperty(CoreOptions.EDGE_LABELS_PLACEMENT,
                                    EdgeLabelPlacement.CENTER);
                            break;
                        case ConnectionLocator.TARGET:
                            labelLayout.setProperty(CoreOptions.EDGE_LABELS_PLACEMENT,
                                    EdgeLabelPlacement.TAIL);
                            break;
                        }
                    } else {
                        labelLayout.setProperty(CoreOptions.EDGE_LABELS_PLACEMENT, placement);
                    }
                    Font font = labelFigure.getFont();
                    if (font != null && !font.isDisposed()) {
                        labelLayout.setProperty(CoreOptions.FONT_NAME,
                                font.getFontData()[0].getName());
                        labelLayout.setProperty(CoreOptions.FONT_SIZE,
                                font.getFontData()[0].getHeight());
                    }
                    labelLayout.setX(labelBounds.x - (double) offset.x);
                    labelLayout.setY(labelBounds.y - (double) offset.y);
                    if (iconBounds != null) {
                        labelLayout.setWidth(labelBounds.width + iconBounds.width);
                    } else {
                        labelLayout.setWidth(labelBounds.width);
                    }
                    labelLayout.setHeight(labelBounds.height);
//                    labelLayout.resetModificationFlag();
                    labelLayout.setText(labelText);
                    mapping.getGraphMap().put(labelLayout, labelEditPart);
                } else {
                    // add the label to the mapping anyway so it is reset to its reference location
                    ElkLabel label = ElkGraphFactory.eINSTANCE.createElkLabel();
                    mapping.getGraphMap().put(label, labelEditPart);
                }
            }
        }
    }
}
