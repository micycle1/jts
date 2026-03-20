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

import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Quadrant;
import org.locationtech.jts.util.Assert;
import org.locationtech.jts.math.DD;

class OverlayEdgeExact implements Comparable<OverlayEdgeExact> {

  public static OverlayEdgeExact createEdge(OverlayPoint[] pts, OverlayLabel lbl, boolean direction) {
    OverlayPoint origin;
    OverlayPoint dirPt;
    if (direction) {
      origin = pts[0];
      dirPt = firstDistinctPoint(pts, 0, 1, pts.length, 1);
    }
    else {
      int ilast = pts.length - 1;
      origin = pts[ilast];
      dirPt = firstDistinctPoint(pts, ilast, ilast - 1, -1, -1);
    }
    return new OverlayEdgeExact(origin, dirPt, direction, lbl, pts);
  }

  private static OverlayPoint firstDistinctPoint(OverlayPoint[] pts, int originIndex, int startIndex, int endExclusive, int step) {
    OverlayPoint origin = pts[originIndex];
    for (int i = startIndex; i != endExclusive; i += step) {
      if (! origin.equals(pts[i])) {
        return pts[i];
      }
    }
    throw new IllegalArgumentException("Cannot create edge with zero-length direction");
  }

  public static OverlayEdgeExact createEdgePair(OverlayPoint[] pts, OverlayLabel lbl) {
    OverlayEdgeExact e0 = OverlayEdgeExact.createEdge(pts, lbl, true);
    OverlayEdgeExact e1 = OverlayEdgeExact.createEdge(pts, lbl, false);
    e0.link(e1);
    return e0;
  }

  private OverlayPoint orig;
  private OverlayEdgeExact sym;
  private OverlayEdgeExact next;
  
  private OverlayPoint[] pts;
  private boolean direction;
  private OverlayPoint dirPt;
  private OverlayLabel label;

  private boolean isInResultArea = false;
  private boolean isInResultLine = false;
  private boolean isVisited = false;

  private OverlayEdgeExact nextResultEdge;
  private OverlayEdgeRingExact edgeRing;
  private MaximalEdgeRingExact maxEdgeRing;
  private OverlayEdgeExact nextResultMaxEdge;

  public OverlayEdgeExact(OverlayPoint orig, OverlayPoint dirPt, boolean direction, OverlayLabel label, OverlayPoint[] pts) {
    this.orig = orig;
    this.dirPt = dirPt;
    this.direction = direction;
    this.pts = pts;
    this.label = label;
  }

  public void link(OverlayEdgeExact sym) {
    this.sym = sym;
    sym.sym = this;
    this.next = sym;
    sym.next = this;
  }

  public OverlayPoint orig() { return orig; }
  public OverlayPoint dest() { return sym.orig; }
  public OverlayEdgeExact symOE() { return sym; }
  public OverlayEdgeExact sym() { return sym; }
  public OverlayEdgeExact oNextOE() { return sym.next; }

  // Next edge CCW around destination
  public OverlayEdgeExact next() { return next; }
  
  public OverlayEdgeExact prev() {
    OverlayEdgeExact curr = this;
    OverlayEdgeExact prevIE = this;
    do {
      prevIE = curr;
      curr = curr.oNextOE();
    } while (curr != this);
    return prevIE.sym;
  }

  public boolean isForward() { return direction; }
  public OverlayPoint directionPt() { return dirPt; }
  public OverlayLabel getLabel() { return label; }
  public OverlayPoint[] getCoordinates() { return pts; }
  
  public int getLocation(int geomIndex, int posIndex) {
    return label.getLocation(geomIndex, posIndex, isForward());
  }

  public int degree() {
    int degree = 0;
    OverlayEdgeExact e = this;
    do {
      degree++;
      e = e.oNextOE();
    } while (e != this);
    return degree;
  }

  public void addCoordinates(CoordinateList coords) {
    boolean isFirstEdge = coords.size() > 0;
    if (direction) {
      int startIndex = 1;
      if (isFirstEdge) startIndex = 0;
      for (int i = startIndex; i < pts.length; i++) {
        coords.add(pts[i].getCoordinate(), false);
      }
    }
    else {
      int startIndex = pts.length - 2;
      if (isFirstEdge) startIndex = pts.length - 1;
      for (int i = startIndex; i >= 0; i--) {
        coords.add(pts[i].getCoordinate(), false);
      }
    }
  }

  // --- Topology Flags ---
  public boolean isInResultArea() { return isInResultArea; }
  public boolean isInResultAreaBoth() { return isInResultArea && symOE().isInResultArea; }
  public void unmarkFromResultAreaBoth() { isInResultArea = false; symOE().isInResultArea = false; }
  public void markInResultArea() { isInResultArea  = true; }
  public void markInResultAreaBoth() { isInResultArea = true; symOE().isInResultArea = true; }
  public boolean isInResultLine() { return isInResultLine; }
  public void markInResultLine() { isInResultLine = true; symOE().isInResultLine = true; }
  public boolean isInResult() { return isInResultArea || isInResultLine; }
  public boolean isInResultEither() { return isInResult() || symOE().isInResult(); }

  void setNextResult(OverlayEdgeExact e) { nextResultEdge = e; }
  public OverlayEdgeExact nextResult() { return nextResultEdge; }
  public boolean isResultLinked() { return nextResultEdge != null; }
  
  void setNextResultMax(OverlayEdgeExact e) { nextResultMaxEdge = e; }
  public OverlayEdgeExact nextResultMax() { return nextResultMaxEdge; }
  public boolean isResultMaxLinked() { return nextResultMaxEdge != null; }
  
  public boolean isVisited() { return isVisited; }
  private void markVisited() { isVisited = true; }
  public void markVisitedBoth() { markVisited(); symOE().markVisited(); }
  
  public void setEdgeRing(OverlayEdgeRingExact edgeRing) { this.edgeRing = edgeRing; } 
  public OverlayEdgeRingExact getEdgeRing() { return edgeRing; } 
  public MaximalEdgeRingExact getEdgeRingMax() { return maxEdgeRing; }
  public void setEdgeRingMax(MaximalEdgeRingExact maximalEdgeRing) { maxEdgeRing = maximalEdgeRing; }

  // --- Noding Graph Insertion & Sorting ---
  public void insert(OverlayEdgeExact eAdd) {
    if (oNextOE() == this) {
      insertAfter(eAdd);
      return;
    }
    OverlayEdgeExact ePrev = insertionEdge(eAdd);
    ePrev.insertAfter(eAdd);
  }

  private void insertAfter(OverlayEdgeExact e) {
    Assert.equals(orig, e.orig());
    OverlayEdgeExact save = oNextOE();
    sym.next = e;
    e.symOE().next = save;
  }

  private OverlayEdgeExact insertionEdge(OverlayEdgeExact eAdd) {
    OverlayEdgeExact ePrev = this;
    do {
      OverlayEdgeExact eNext = ePrev.oNextOE();
      if (eNext.compareTo(ePrev) > 0 && eAdd.compareTo(ePrev) >= 0 && eAdd.compareTo(eNext) <= 0) { 
        return ePrev;         
      }
      if (eNext.compareTo(ePrev) <= 0 && (eAdd.compareTo(eNext) <= 0 || eAdd.compareTo(ePrev) >= 0)) {
        return ePrev; 
      }
      ePrev = eNext;
    } while (ePrev != this);
    Assert.shouldNeverReachHere();
    return null;
  }

  @Override
  public int compareTo(OverlayEdgeExact e) {
    DD v1x = dirPt.getX().subtract(orig.getX());
    DD v1y = dirPt.getY().subtract(orig.getY());
    DD v2x = e.dirPt.getX().subtract(e.orig.getX());
    DD v2y = e.dirPt.getY().subtract(e.orig.getY());

    if (v1x.compareTo(v2x) == 0 && v1y.compareTo(v2y) == 0) return 0;

    int quadrant = quadrant(v1x, v1y);
    int quadrant2 = quadrant(v2x, v2y);

    if (quadrant > quadrant2) return 1;
    if (quadrant < quadrant2) return -1;

    // Match HalfEdge.compareAngularDirection:
    // Orientation.index(orig, dir2, dir1) = sign(det(v2, v1)) = -sign(det(v1, v2))
    DD det = v1x.multiply(v2y).subtract(v1y.multiply(v2x));

    return -det.signum();
  }

  private static int quadrant(DD dx, DD dy) {
    int xSign = dx.signum();
    int ySign = dy.signum();
    if (xSign == 0 && ySign == 0) {
      throw new IllegalArgumentException("Cannot compute the quadrant for point ( 0.0, 0.0 )");
    }
    if (xSign >= 0) {
      return ySign >= 0 ? Quadrant.NE : Quadrant.SE;
    }
    return ySign >= 0 ? Quadrant.NW : Quadrant.SW;
  }
}
