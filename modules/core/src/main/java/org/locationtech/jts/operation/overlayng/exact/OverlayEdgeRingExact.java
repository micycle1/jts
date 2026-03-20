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
import java.util.List;

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator;
import org.locationtech.jts.algorithm.locate.PointOnGeometryLocator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateArrays;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;

class OverlayEdgeRingExact {
  
  private OverlayEdgeExact startEdge;
  private LinearRing ring;
  private boolean isHole;
  private Coordinate[] ringPts;
  private IndexedPointInAreaLocator locator;
  private OverlayEdgeRingExact shell;
  private List<OverlayEdgeRingExact> holes = new ArrayList<OverlayEdgeRingExact>();

  public OverlayEdgeRingExact(OverlayEdgeExact start, GeometryFactory geometryFactory) {
    startEdge = start;
    ringPts = computeRingPts(start);
    computeRing(ringPts, geometryFactory);
  }

  public LinearRing getRing() { return ring; }
  private Envelope getEnvelope() { return ring.getEnvelopeInternal(); }
  public boolean isHole() { return isHole; }
  public boolean isDegenerate() {
    Coordinate[] ptsNoRepeat = CoordinateArrays.removeRepeatedPoints(ringPts);
    return ptsNoRepeat.length < 4;
  }
  
  public void setShell(OverlayEdgeRingExact shell) {
    this.shell = shell;
    if (shell != null) shell.addHole(this);
  }
  public boolean hasShell() { return shell != null; }
  public OverlayEdgeRingExact getShell() {
    if (isHole()) return shell;
    return this;
  }

  public void addHole(OverlayEdgeRingExact ring) { holes.add(ring); }

  private Coordinate[] computeRingPts(OverlayEdgeExact start) {
    OverlayEdgeExact edge = start;
    CoordinateList pts = new CoordinateList();
    do {
      if (edge.getEdgeRing() == this)
        throw new TopologyException("Edge visited twice during ring-building at " + edge.orig().getCoordinate(), edge.orig().getCoordinate());

      edge.addCoordinates(pts);
      edge.setEdgeRing(this);
      if (edge.nextResult() == null)
        throw new TopologyException("Found null edge in ring", edge.dest().getCoordinate());

      edge = edge.nextResult();
    } while (edge != start);
    pts.closeRing();
    return pts.toCoordinateArray();
  }
  
  private void computeRing(Coordinate[] ringPts, GeometryFactory geometryFactory) {
    if (ring != null) return;
    ring = geometryFactory.createLinearRing(ringPts);
    isHole = Orientation.isCCW(ring.getCoordinates());
  }

  private Coordinate[] getCoordinates() { return ringPts; }
  
  public OverlayEdgeRingExact findEdgeRingContaining(List<OverlayEdgeRingExact> erList) {
    OverlayEdgeRingExact minContainingRing = null;
    for (OverlayEdgeRingExact edgeRing: erList) {
      if (edgeRing.contains(this)) {
        if (minContainingRing == null || minContainingRing.getEnvelope().contains(edgeRing.getEnvelope())) {
          minContainingRing = edgeRing;
        }
      }
    }
    return minContainingRing;
  }

  private PointOnGeometryLocator getLocator() {
    if (locator == null) {
      locator = new IndexedPointInAreaLocator(getRing());
    }
    return locator;
  }
  
  public int locate(Coordinate pt) {
    return getLocator().locate(pt);
  }
  
  private boolean contains(OverlayEdgeRingExact ring) {
    Envelope env = getEnvelope();
    Envelope testEnv = ring.getEnvelope();
    if (! env.containsProperly(testEnv)) {
      return false;
    }
    return isPointInOrOut(ring);
  }
  
  private boolean isPointInOrOut(OverlayEdgeRingExact ring) {
    for (Coordinate pt : ring.getCoordinates()) {
      int loc = locate(pt);
      if (loc == Location.INTERIOR) {
        return true;
      }
      if (loc == Location.EXTERIOR) {
        return false;
      }
    }
    return false;
  }

  public Coordinate getCoordinate() { return ringPts[0]; }

  public Polygon toPolygon(GeometryFactory factory) {
    LinearRing[] holeLR = null;
    if (holes != null) {
      holeLR = new LinearRing[holes.size()];
      for (int i = 0; i < holes.size(); i++) {
        holeLR[i] = (LinearRing) holes.get(i).getRing();
      }
    }
    Polygon poly = factory.createPolygon(ring, holeLR);
    return poly;
  }

  public OverlayEdgeExact getEdge() { return startEdge; }
}
