/*
 * Copyright (c) 2024 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.overlayng.exact;

import org.locationtech.jts.operation.overlayng.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.Position;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.util.Assert;

class OverlayLabellerExact {
  private OverlayGraphExact graph;
  private InputGeometry inputGeometry;
  private Collection<OverlayEdgeExact> edges;
  
  public OverlayLabellerExact(OverlayGraphExact graph, InputGeometry inputGeometry) {
    this.graph = graph;
    this.inputGeometry = inputGeometry;
    edges = graph.getEdges();
  }

  public void computeLabelling() {
    Collection<OverlayEdgeExact> nodes = graph.getNodeEdges();
    
    labelAreaNodeEdges(nodes);
    labelConnectedLinearEdges();
    
    labelCollapsedEdges();
    labelConnectedLinearEdges();
    
    labelDisconnectedEdges();
  }

  private void labelAreaNodeEdges(Collection<OverlayEdgeExact> nodes) {
    for (OverlayEdgeExact nodeEdge : nodes) {
      propagateAreaLocations(nodeEdge, 0);
      if (inputGeometry.hasEdges(1)) {
        propagateAreaLocations(nodeEdge, 1);
      }
    }
  }

  public void propagateAreaLocations(OverlayEdgeExact nodeEdge, int geomIndex) {
    if (! inputGeometry.isArea(geomIndex)) return;
    if (nodeEdge.degree() == 1) return;
    
    OverlayEdgeExact eStart = findPropagationStartEdge(nodeEdge, geomIndex);
    if (eStart == null) return;
    
    int currLoc = eStart.getLocation(geomIndex, Position.LEFT);
    OverlayEdgeExact e = eStart.oNextOE();
    
    do {
      OverlayLabel label = e.getLabel();
      if (! label.isBoundary(geomIndex)) {
        label.setLocationLine(geomIndex, currLoc);
      }
      else {
        Assert.isTrue(label.hasSides(geomIndex));
        int locRight = e.getLocation(geomIndex, Position.RIGHT);
        if (locRight != currLoc) {
          throw new TopologyException("side location conflict: arg " + geomIndex, e.orig().getCoordinate());
        }
        int locLeft = e.getLocation(geomIndex, Position.LEFT);
        if (locLeft == Location.NONE) {
          Assert.shouldNeverReachHere("found single null side at " + e);
        }
        currLoc = locLeft;
      }
      e = e.oNextOE();
    } while (e != eStart);
  }

  private static OverlayEdgeExact findPropagationStartEdge(OverlayEdgeExact nodeEdge, int geomIndex) {
    OverlayEdgeExact eStart = nodeEdge;
    do {
      OverlayLabel label = eStart.getLabel();
      if (label.isBoundary(geomIndex)) {
        Assert.isTrue(label.hasSides(geomIndex));
        return eStart;
      }
      eStart = eStart.oNextOE();
    } while (eStart != nodeEdge);
    return null;
  }
  
  private void labelCollapsedEdges() {
    for (OverlayEdgeExact edge : edges) {
      if (edge.getLabel().isLineLocationUnknown(0)) {
        labelCollapsedEdge(edge, 0);
      }
      if (edge.getLabel().isLineLocationUnknown(1)) {
        labelCollapsedEdge(edge, 1);
      }
    }
  }

  private void labelCollapsedEdge(OverlayEdgeExact edge, int geomIndex) {
    OverlayLabel label = edge.getLabel();
    if (! label.isCollapse(geomIndex)) return;
    label.setLocationCollapse(geomIndex);
  }

  private void labelConnectedLinearEdges() {
    propagateLinearLocations(0);
    if (inputGeometry.hasEdges(1)) {
      propagateLinearLocations(1);
    }
  }

  private void propagateLinearLocations(int geomIndex) {
    List<OverlayEdgeExact> linearEdges = findLinearEdgesWithLocation(edges, geomIndex);
    if (linearEdges.size() <= 0) return;
    
    Deque<OverlayEdgeExact> edgeStack = new ArrayDeque<OverlayEdgeExact>(linearEdges);
    boolean isInputLine = inputGeometry.isLine(geomIndex);
    while (! edgeStack.isEmpty()) {
      OverlayEdgeExact lineEdge = edgeStack.removeFirst();
      propagateLinearLocationAtNode(lineEdge, geomIndex, isInputLine, edgeStack);
    }
  }
  
  private static void propagateLinearLocationAtNode(OverlayEdgeExact eNode, 
      int geomIndex, boolean isInputLine, 
      Deque<OverlayEdgeExact> edgeStack) {
    int lineLoc = eNode.getLabel().getLineLocation(geomIndex);
    if (isInputLine && lineLoc != Location.EXTERIOR) return;
    
    OverlayEdgeExact e = eNode.oNextOE();
    do {
      OverlayLabel label = e.getLabel();
      if ( label.isLineLocationUnknown(geomIndex) ) {
        label.setLocationLine(geomIndex, lineLoc);
        edgeStack.addFirst( e.symOE() );
      }
      e = e.oNextOE();
    } while (e != eNode);
  }
  
  private static List<OverlayEdgeExact> findLinearEdgesWithLocation(
      Collection<OverlayEdgeExact>edges, int geomIndex) {
    List<OverlayEdgeExact> linearEdges = new ArrayList<OverlayEdgeExact>();
    for (OverlayEdgeExact edge : edges) {
      OverlayLabel lbl = edge.getLabel();
      if (lbl.isLinear(geomIndex) && !lbl.isLineLocationUnknown(geomIndex)) {
        linearEdges.add(edge);
      }
    }
    return linearEdges;
  }

  private void labelDisconnectedEdges() {
    for (OverlayEdgeExact edge : edges) {
      if (edge.getLabel().isLineLocationUnknown(0)) {
        labelDisconnectedEdge(edge, 0);
      }
      if (edge.getLabel().isLineLocationUnknown(1)) {
        labelDisconnectedEdge(edge, 1);
      }
    }
  }

  private void labelDisconnectedEdge(OverlayEdgeExact edge, int geomIndex) { 
     OverlayLabel label = edge.getLabel();
    
    if (! inputGeometry.isArea(geomIndex)) {
      label.setLocationAll(geomIndex, Location.EXTERIOR);
      return;
    };
    
    int edgeLoc = locateEdgeBothEnds(geomIndex, edge);
    label.setLocationAll(geomIndex, edgeLoc);
  }

  private int locateEdgeBothEnds(int geomIndex, OverlayEdgeExact edge) {
    int locOrig = inputGeometry.locatePointInArea(geomIndex, edge.orig().getCoordinate());
    int locDest = inputGeometry.locatePointInArea(geomIndex, edge.dest().getCoordinate());
    boolean isInt = locOrig != Location.EXTERIOR && locDest != Location.EXTERIOR;
    int edgeLoc = isInt ? Location.INTERIOR : Location.EXTERIOR;
    return edgeLoc;
  } 

  public void markResultAreaEdges(int overlayOpCode) {
    for (OverlayEdgeExact edge : edges) {
      markInResultArea(edge, overlayOpCode);
    }
  }

  public void markInResultArea(OverlayEdgeExact e, int overlayOpCode) {
    OverlayLabel label = e.getLabel();
    if ( label.isBoundaryEither()
        && OverlayNGExact.isResultOfOp(
              overlayOpCode,
              label.getLocationBoundaryOrLine(0, Position.RIGHT, e.isForward()),
              label.getLocationBoundaryOrLine(1, Position.RIGHT, e.isForward()))) {
      e.markInResultArea();  
    }
  }
  
  public void unmarkDuplicateEdgesFromResultArea() {
    for (OverlayEdgeExact edge : edges) {
      if (edge.isInResultAreaBoth()) {
        edge.unmarkFromResultAreaBoth();     
      }
    }
  }

  public static String toString(OverlayEdgeExact nodeEdge) {
    Coordinate orig = nodeEdge.orig().getCoordinate();
    StringBuilder sb = new StringBuilder();
    sb.append("Node( "+ WKTWriter.format(orig) + " )" + "\n");
    OverlayEdgeExact e = nodeEdge;
    do {
      sb.append("  -> " + e);
      if (e.isResultLinked()) {
        sb.append(" Link: ");
        sb.append(e.nextResult());
      }
      sb.append("\n");
      e = e.oNextOE();
    } while (e != nodeEdge);
    return sb.toString(); 
  }
}
