/*
 * Copyright (c) 2024 Martin Davis
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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.util.Assert;


class MaximalEdgeRingExact {

  private static final int STATE_FIND_INCOMING = 1;
  private static final int STATE_LINK_OUTGOING = 2;

  public static void linkResultAreaMaxRingAtNode(OverlayEdgeExact nodeEdge) {
    Assert.isTrue(nodeEdge.isInResultArea(), "Attempt to link non-result edge");

    OverlayEdgeExact endOut = nodeEdge.oNextOE();
    OverlayEdgeExact currOut = endOut;
    int state = STATE_FIND_INCOMING;
    OverlayEdgeExact currResultIn = null;
    do {
      if (currResultIn != null && currResultIn.isResultMaxLinked())
        return;
      
      switch (state) {
      case STATE_FIND_INCOMING:
        OverlayEdgeExact currIn = currOut.symOE();
        if (! currIn.isInResultArea()) break;
        currResultIn = currIn;
        state = STATE_LINK_OUTGOING;
        break;
      case STATE_LINK_OUTGOING:
        if (! currOut.isInResultArea()) break;
        currResultIn.setNextResultMax(currOut);
        state = STATE_FIND_INCOMING;
        break;
      }
      currOut = currOut.oNextOE();
    } while (currOut != endOut);
    
    if (state == STATE_LINK_OUTGOING) {
      throw new TopologyException("no outgoing edge found", nodeEdge.orig().getCoordinate());
    }    
  }

  private OverlayEdgeExact startEdge;

  public MaximalEdgeRingExact(OverlayEdgeExact e) {
    this.startEdge = e;
    attachEdges(e);
  }

  private void attachEdges(OverlayEdgeExact startEdge) {
    OverlayEdgeExact edge = startEdge;
    do {
      if (edge == null)
        throw new TopologyException("Ring edge is null");
      if (edge.getEdgeRingMax() == this)
        throw new TopologyException("Ring edge visited twice at " + edge.orig().getCoordinate(), edge.orig().getCoordinate());
      if (edge.nextResultMax() == null) {
        throw new TopologyException("Ring edge missing at", edge.dest().getCoordinate());
      }
      edge.setEdgeRingMax(this);
      edge = edge.nextResultMax();
    } while (edge != startEdge);  
  }
  
  public List<OverlayEdgeRingExact> buildMinimalRings(GeometryFactory geometryFactory) {
    linkMinimalRings();
    
    List<OverlayEdgeRingExact> minEdgeRings = new ArrayList<OverlayEdgeRingExact>();
    OverlayEdgeExact e = startEdge;
    do {
      if (e.getEdgeRing() == null) {
        OverlayEdgeRingExact minEr = new OverlayEdgeRingExact(e, geometryFactory);
        minEdgeRings.add(minEr);
      }
      e = e.nextResultMax();
    } while (e != startEdge);
    return minEdgeRings;
  }
  
  private void linkMinimalRings() {
    OverlayEdgeExact e = startEdge;
    do {
      linkMinRingEdgesAtNode(e, this);
      e = e.nextResultMax();
    } while (e != startEdge);
  }
  
  private static void linkMinRingEdgesAtNode(OverlayEdgeExact nodeEdge, MaximalEdgeRingExact maxRing) {
    OverlayEdgeExact endOut = nodeEdge;
    OverlayEdgeExact currMaxRingOut = endOut;
    OverlayEdgeExact currOut = endOut.oNextOE();

    do {
      if (isAlreadyLinked(currOut.symOE(), maxRing)) 
        return;

      if (currMaxRingOut == null) {
        currMaxRingOut = selectMaxOutEdge(currOut, maxRing);
      }
      else {
        currMaxRingOut = linkMaxInEdge(currOut, currMaxRingOut, maxRing);
      }
      currOut = currOut.oNextOE();
    } while (currOut != endOut);
    
    if (currMaxRingOut != null) {
      throw new TopologyException("Unmatched edge found during min-ring linking", nodeEdge.orig().getCoordinate());
    }    
  }

  private static boolean isAlreadyLinked(OverlayEdgeExact edge, MaximalEdgeRingExact maxRing) {
    boolean isLinked = edge.getEdgeRingMax() == maxRing
        && edge.isResultLinked();
    return isLinked;
  }

  private static OverlayEdgeExact selectMaxOutEdge(OverlayEdgeExact currOut, MaximalEdgeRingExact maxEdgeRing) {
    if (currOut.getEdgeRingMax() ==  maxEdgeRing)
      return currOut;
    return null;
  }

  private static OverlayEdgeExact linkMaxInEdge(OverlayEdgeExact currOut, 
      OverlayEdgeExact currMaxRingOut, 
      MaximalEdgeRingExact maxEdgeRing) 
  {
    OverlayEdgeExact currIn = currOut.symOE();
    if (currIn.getEdgeRingMax() !=  maxEdgeRing) 
      return currMaxRingOut;
    
    currIn.setNextResult(currMaxRingOut);
    return null;
  }
  
  public String toString() {
    Coordinate[] pts = getCoordinates();
    return WKTWriter.toLineString(pts);
  }

  private Coordinate[] getCoordinates() {
    CoordinateList coords = new CoordinateList();
    OverlayEdgeExact edge = startEdge;
    do {
      coords.add(edge.orig().getCoordinate());
      if (edge.nextResultMax() == null) {
        break;
      }
      edge = edge.nextResultMax();
    } while (edge != startEdge); 
    coords.add(edge.dest().getCoordinate());
    return coords.toCoordinateArray();
  }
}
