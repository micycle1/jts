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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.util.Assert;

class PolygonBuilderExact {

  private GeometryFactory geometryFactory;
  private List<OverlayEdgeRingExact> shellList = new ArrayList<OverlayEdgeRingExact>();
  private List<OverlayEdgeRingExact> freeHoleList = new ArrayList<OverlayEdgeRingExact>();
  private boolean isEnforcePolygonal = true;

  public PolygonBuilderExact(List<OverlayEdgeExact> resultAreaEdges, GeometryFactory geomFact) {
    this(resultAreaEdges, geomFact, true);
  }
  
  public PolygonBuilderExact(List<OverlayEdgeExact> resultAreaEdges, GeometryFactory geomFact, boolean isEnforcePolygonal) {
    this.geometryFactory = geomFact;
    this.isEnforcePolygonal = isEnforcePolygonal;
    buildRings(resultAreaEdges);
  }

  public List<Polygon> getPolygons() {
    return computePolygons(shellList);  
  }

  public List<OverlayEdgeRingExact> getShellRings() {
    return shellList;  
  }

  private List<Polygon> computePolygons(List<OverlayEdgeRingExact> shellList) {
    List<Polygon> resultPolyList = new ArrayList<Polygon>();
    for (OverlayEdgeRingExact er : shellList ) {
      Polygon poly = er.toPolygon(geometryFactory);
      resultPolyList.add(poly);
    }
    return resultPolyList;
  }
  
  private void buildRings(List<OverlayEdgeExact> resultAreaEdges) {
    linkResultAreaEdgesMax(resultAreaEdges);
    List<MaximalEdgeRingExact> maxRings = buildMaximalRings(resultAreaEdges);
    buildMinimalRings(maxRings);
    placeFreeHoles(shellList, freeHoleList);
  }
  
  private void linkResultAreaEdgesMax(List<OverlayEdgeExact> resultEdges) {
    for (OverlayEdgeExact edge : resultEdges ) {
      MaximalEdgeRingExact.linkResultAreaMaxRingAtNode(edge);
    }    
  }
  
  private static List<MaximalEdgeRingExact> buildMaximalRings(Collection<OverlayEdgeExact> edges) {
    List<MaximalEdgeRingExact> edgeRings = new ArrayList<MaximalEdgeRingExact>();
    for (OverlayEdgeExact e : edges) {
      if (e.isInResultArea() && e.getLabel().isBoundaryEither() ) {
        if (e.getEdgeRingMax() == null) {
          MaximalEdgeRingExact er = new MaximalEdgeRingExact(e);
          edgeRings.add(er);
        }
      }
    }
    return edgeRings;
  }

  private void buildMinimalRings(List<MaximalEdgeRingExact> maxRings) {
    for (MaximalEdgeRingExact erMax : maxRings) {
      List<OverlayEdgeRingExact> minRings = erMax.buildMinimalRings(geometryFactory);
      minRings = filterDegenerateRings(minRings);
      assignShellsAndHoles(minRings);
    }
  }

  private static List<OverlayEdgeRingExact> filterDegenerateRings(List<OverlayEdgeRingExact> edgeRings) {
    List<OverlayEdgeRingExact> validRings = new ArrayList<OverlayEdgeRingExact>();
    for (OverlayEdgeRingExact edgeRing : edgeRings) {
      if (! edgeRing.isDegenerate()) {
        validRings.add(edgeRing);
      }
    }
    return validRings;
  }

  private void assignShellsAndHoles(List<OverlayEdgeRingExact> minRings) {
    OverlayEdgeRingExact shell = findSingleShell(minRings);
    if (shell != null) {
      assignHoles(shell, minRings);
      shellList.add(shell);
    }
    else {
      freeHoleList.addAll(minRings);
    }
  }
  
  private OverlayEdgeRingExact findSingleShell(List<OverlayEdgeRingExact> edgeRings) {
    int shellCount = 0;
    OverlayEdgeRingExact shell = null;
    for ( OverlayEdgeRingExact er : edgeRings ) {
      if (! er.isHole()) {
        shell = er;
        shellCount++;
      }
    }
    Assert.isTrue(shellCount <= 1, "found two shells in EdgeRing list");
    return shell;
  }
  
  private static void assignHoles(OverlayEdgeRingExact shell, List<OverlayEdgeRingExact> edgeRings) {
    for (OverlayEdgeRingExact er : edgeRings) {
      if (er.isHole()) {
        er.setShell(shell);
      }
    }
  }

  private void placeFreeHoles(List<OverlayEdgeRingExact> shellList, List<OverlayEdgeRingExact> freeHoleList) {
    for (OverlayEdgeRingExact hole : freeHoleList ) {
      if (hole.getShell() == null) {
        OverlayEdgeRingExact shell = hole.findEdgeRingContaining(shellList);
        if (isEnforcePolygonal  && shell == null) {
          throw new TopologyException("unable to assign free hole to a shell", hole.getCoordinate());
        }
        hole.setShell(shell);
      }
    }
  }

}
