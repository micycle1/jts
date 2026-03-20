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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.math.DD;

/**
 * An exact coordinate representing a high-precision double-double topology point.
 * Used internally by the exact overlay graph to preserve precision and identity.
 */
class OverlayPoint implements Comparable<OverlayPoint> {
  private final DD dx;
  private final DD dy;
  
  public OverlayPoint(DD dx, DD dy) {
    this.dx = dx;
    this.dy = dy;
  }
  
  public OverlayPoint(Coordinate c) {
    this.dx = DD.valueOf(c.x);
    this.dy = DD.valueOf(c.y);
  }

  public DD getX() {
    return dx;
  }

  public DD getY() {
    return dy;
  }

  public Coordinate getCoordinate() {
    return new Coordinate(dx.doubleValue(), dy.doubleValue());
  }

  @Override
  public int compareTo(OverlayPoint o) {
    int compX = dx.compareTo(o.dx);
    if (compX != 0) return compX;
    return dy.compareTo(o.dy);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof OverlayPoint)) return false;
    OverlayPoint other = (OverlayPoint) obj;
    return dx.compareTo(other.dx) == 0 && dy.compareTo(other.dy) == 0;
  }

  @Override
  public int hashCode() {
    long xBits = Double.doubleToLongBits(dx.doubleValue());
    long yBits = Double.doubleToLongBits(dy.doubleValue());
    int result = 17;
    result = 37 * result + (int) (xBits ^ (xBits >>> 32));
    result = 37 * result + (int) (yBits ^ (yBits >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "OverlayPoint(" + dx + ", " + dy + ")";
  }
}
