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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Location;

class LineBuilderExact {
  
  private GeometryFactory geometryFactory;
  private OverlayGraphExact graph;
  private int opCode;
  private int inputAreaIndex;
  private boolean hasResultArea;
  
  private boolean isAllowMixedResult = ! OverlayNGExact.STRICT_MODE_DEFAULT;
  private boolean isAllowCollapseLines = ! OverlayNGExact.STRICT_MODE_DEFAULT;

  private List<LineString> lines = new ArrayList<LineString>();
  
  public LineBuilderExact(InputGeometry inputGeom, OverlayGraphExact graph, boolean hasResultArea, int opCode, GeometryFactory geomFact) {
    this.graph = graph;
    this.opCode = opCode;
    this.geometryFactory = geomFact;
    this.hasResultArea = hasResultArea;
    inputAreaIndex = inputGeom.getAreaIndex();
  }

  public void setStrictMode(boolean isStrictResultMode) {
    isAllowCollapseLines = ! isStrictResultMode;
    isAllowMixedResult = ! isStrictResultMode;
  }
  
  public List<LineString> getLines() {
	  markResultLines();
	  addResultLinesForNodes();
	  addResultLinesRings();
	  return lines;
	}

  private void markResultLines() {
    Collection<OverlayEdgeExact> edges = graph.getEdges();
    for (OverlayEdgeExact edge : edges) {
      if (edge.isInResultEither()) 
        continue;
      if (isResultLine(edge.getLabel())) {
        edge.markInResultLine();
      }
    }
  }
  
  private boolean isResultLine(OverlayLabel lbl) {
    if (lbl.isBoundarySingleton()) return false;
    
    if (! isAllowCollapseLines && lbl.isBoundaryCollapse()) return false;

    if (lbl.isInteriorCollapse()) return false;
    
    if (opCode != OverlayNGExact.INTERSECTION) {
      if (lbl.isCollapseAndNotPartInterior()) return false;

      if (hasResultArea && lbl.isLineInArea(inputAreaIndex)) 
        return false;
    }
    
    if (isAllowMixedResult && opCode == OverlayNGExact.INTERSECTION && lbl.isBoundaryTouch()) {
      return true;
    }
    
    int aLoc = effectiveLocation(lbl, 0);
    int bLoc = effectiveLocation(lbl, 1);
    boolean isInResult = OverlayNGExact.isResultOfOp(opCode, aLoc, bLoc);
    return isInResult;
  }
  
  private static int effectiveLocation(OverlayLabel lbl, int geomIndex) {
    if (lbl.isCollapse(geomIndex)) return Location.INTERIOR;
    if (lbl.isLine(geomIndex)) return Location.INTERIOR;
    return lbl.getLineLocation(geomIndex);
  }
  
  private void addResultLinesForNodes() {
    Collection<OverlayEdgeExact> edges = graph.getEdges();
    for (OverlayEdgeExact edge : edges) {
      if (! edge.isInResultLine()) continue;
      if (edge.isVisited()) continue;
      
      if (degreeOfLines(edge) != 2) {
        lines.add( buildLine( edge ));
      }
    }
  }
 
  private void addResultLinesRings() {
    Collection<OverlayEdgeExact> edges = graph.getEdges();
    for (OverlayEdgeExact edge : edges) {
      if (! edge.isInResultLine()) continue;
      if (edge.isVisited()) continue;
      
      lines.add( buildLine( edge ));
    }
  }
 
  private LineString buildLine(OverlayEdgeExact node) {
    CoordinateList pts = new CoordinateList();
    pts.add(node.orig().getCoordinate(), false);
    
    boolean isForward = node.isForward();
    
    OverlayEdgeExact e = node;
    do {
      e.markVisitedBoth();
      e.addCoordinates(pts);
      
      if (degreeOfLines(e.symOE()) != 2) {
        break;
      }
      e = nextLineEdgeUnvisited(e.symOE());
    }
    while (e != null);
    
    Coordinate[] ptsOut = pts.toCoordinateArray(isForward);
    
    LineString line = geometryFactory.createLineString(ptsOut);
    return line;
  }

  private static OverlayEdgeExact nextLineEdgeUnvisited(OverlayEdgeExact node) {
    OverlayEdgeExact e = node;
    do {
      e = e.oNextOE();
      if (e.isVisited()) continue;
      if (e.isInResultLine()) {
        return e;
      }
    } while (e != node);
    return null;
  }

  private static int degreeOfLines(OverlayEdgeExact node) {
    int degree = 0;
    OverlayEdgeExact e = node;
    do {
      if (e.isInResultLine()) {
        degree++;
      }
      e = e.oNextOE();
    } while (e != node);
    return degree;
  }
}
