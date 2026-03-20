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

import org.locationtech.jts.algorithm.Distance;
import org.locationtech.jts.algorithm.LineIntersector;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.math.DD;

/**
 * A robust line intersector that computes and preserves the exact 
 * Double-Double (DD) intersection points as {@link OverlayPoint}s.
 */
class ExactLineIntersector extends LineIntersector {

  private OverlayPoint[] exactIntPt = new OverlayPoint[2];

  public ExactLineIntersector() {
    super();
  }

  public OverlayPoint getExactIntersection(int intIndex) {
    return exactIntPt[intIndex];
  }

  @Override
  public void computeIntersection(Coordinate p, Coordinate p1, Coordinate p2) {
    isProper = false;
    if (Envelope.intersects(p1, p2, p)) {
      if ((Orientation.index(p1, p2, p) == 0) && (Orientation.index(p2, p1, p) == 0)) {
        isProper = true;
        if (p.equals(p1) || p.equals(p2)) {
          isProper = false;
        }
        result = POINT_INTERSECTION;
        exactIntPt[0] = new OverlayPoint(p);
        return;
      }
    }
    result = NO_INTERSECTION;
  }

  @Override
  protected int computeIntersect(Coordinate p1, Coordinate p2, Coordinate q1, Coordinate q2) {
    isProper = false;

    if (!Envelope.intersects(p1, p2, q1, q2)) return NO_INTERSECTION;

    int Pq1 = Orientation.index(p1, p2, q1);
    int Pq2 = Orientation.index(p1, p2, q2);

    if ((Pq1 > 0 && Pq2 > 0) || (Pq1 < 0 && Pq2 < 0)) return NO_INTERSECTION;

    int Qp1 = Orientation.index(q1, q2, p1);
    int Qp2 = Orientation.index(q1, q2, p2);

    if ((Qp1 > 0 && Qp2 > 0) || (Qp1 < 0 && Qp2 < 0)) return NO_INTERSECTION;

    boolean collinear = Pq1 == 0 && Pq2 == 0 && Qp1 == 0 && Qp2 == 0;
    if (collinear) {
      return computeCollinearIntersection(p1, p2, q1, q2);
    }

    OverlayPoint exactP = null;
    Coordinate p = null;

    if (Pq1 == 0 || Pq2 == 0 || Qp1 == 0 || Qp2 == 0) {
      isProper = false;
      if (p1.equals2D(q1)) { p = p1; exactP = new OverlayPoint(p1); }
      else if (p1.equals2D(q2)) { p = p1; exactP = new OverlayPoint(p1); }
      else if (p2.equals2D(q1)) { p = p2; exactP = new OverlayPoint(p2); }
      else if (p2.equals2D(q2)) { p = p2; exactP = new OverlayPoint(p2); }
      else if (Pq1 == 0) { p = q1; exactP = new OverlayPoint(q1); }
      else if (Pq2 == 0) { p = q2; exactP = new OverlayPoint(q2); }
      else if (Qp1 == 0) { p = p1; exactP = new OverlayPoint(p1); }
      else if (Qp2 == 0) { p = p2; exactP = new OverlayPoint(p2); }
    } else {
      isProper = true;
      exactP = computeExactIntersection(p1, p2, q1, q2);
      if (exactP != null) {
        p = exactP.getCoordinate();
        // FIX: Removed the inaccurate double-precision isInSegmentEnvelopes fallback.
        // We trust the exact math.
      } else {
        p = nearestEndpoint(p1, p2, q1, q2);
        exactP = new OverlayPoint(p);
      }
    }

    intPt[0] = p; 
    exactIntPt[0] = exactP;
    return POINT_INTERSECTION;
  }

  private int computeCollinearIntersection(Coordinate p1, Coordinate p2, Coordinate q1, Coordinate q2) {
    boolean q1inP = Envelope.intersects(p1, p2, q1);
    boolean q2inP = Envelope.intersects(p1, p2, q2);
    boolean p1inQ = Envelope.intersects(q1, q2, p1);
    boolean p2inQ = Envelope.intersects(q1, q2, p2);

    if (q1inP && q2inP) {
      setCollinearInts(q1, q2);
      return COLLINEAR_INTERSECTION;
    }
    if (p1inQ && p2inQ) {
      setCollinearInts(p1, p2);
      return COLLINEAR_INTERSECTION;
    }
    if (q1inP && p1inQ) {
      setCollinearInts(q1, p1);
      return q1.equals(p1) && !q2inP && !p2inQ ? POINT_INTERSECTION : COLLINEAR_INTERSECTION;
    }
    if (q1inP && p2inQ) {
      setCollinearInts(q1, p2);
      return q1.equals(p2) && !q2inP && !p1inQ ? POINT_INTERSECTION : COLLINEAR_INTERSECTION;
    }
    if (q2inP && p1inQ) {
      setCollinearInts(q2, p1);
      return q2.equals(p1) && !q1inP && !p2inQ ? POINT_INTERSECTION : COLLINEAR_INTERSECTION;
    }
    if (q2inP && p2inQ) {
      setCollinearInts(q2, p2);
      return q2.equals(p2) && !q1inP && !p1inQ ? POINT_INTERSECTION : COLLINEAR_INTERSECTION;
    }
    return NO_INTERSECTION;
  }

  private void setCollinearInts(Coordinate p1, Coordinate p2) {
    intPt[0] = p1; exactIntPt[0] = new OverlayPoint(p1);
    intPt[1] = p2; exactIntPt[1] = new OverlayPoint(p2);
  }

  private boolean isInSegmentEnvelopes(Coordinate pt) {
    Envelope env0 = new Envelope(inputLines[0][0], inputLines[0][1]);
    Envelope env1 = new Envelope(inputLines[1][0], inputLines[1][1]);
    return env0.contains(pt) && env1.contains(pt);
  }

  private static Coordinate nearestEndpoint(Coordinate p1, Coordinate p2, Coordinate q1, Coordinate q2) {
    Coordinate nearestPt = p1;
    double minDist = Distance.pointToSegment(p1, q1, q2);
    double dist = Distance.pointToSegment(p2, q1, q2);
    if (dist < minDist) { minDist = dist; nearestPt = p2; }
    dist = Distance.pointToSegment(q1, p1, p2);
    if (dist < minDist) { minDist = dist; nearestPt = q1; }
    dist = Distance.pointToSegment(q2, p1, p2);
    if (dist < minDist) { nearestPt = q2; }
    return nearestPt;
  }

  /**
   * Computes exact DD intersection point.
   */
  private static OverlayPoint computeExactIntersection(Coordinate p1, Coordinate p2, Coordinate q1, Coordinate q2) {
    DD px = new DD(p1.y).selfSubtract(p2.y);
    DD py = new DD(p2.x).selfSubtract(p1.x);
    DD pw = new DD(p1.x).selfMultiply(p2.y).selfSubtract(new DD(p2.x).selfMultiply(p1.y));

    DD qx = new DD(q1.y).selfSubtract(q2.y);
    DD qy = new DD(q2.x).selfSubtract(q1.x);
    DD qw = new DD(q1.x).selfMultiply(q2.y).selfSubtract(new DD(q2.x).selfMultiply(q1.y));

    DD x = py.multiply(qw).selfSubtract(qy.multiply(pw));
    DD y = qx.multiply(pw).selfSubtract(px.multiply(qw));
    DD w = px.multiply(qy).selfSubtract(qx.multiply(py));

    if (w.isZero()) {
      return null;
    }

    DD xInt = x.divide(w);
    DD yInt = y.divide(w);

    if (xInt.isNaN() || yInt.isNaN()) {
      return null;
    }

    return new OverlayPoint(xInt, yInt);
  }
}
