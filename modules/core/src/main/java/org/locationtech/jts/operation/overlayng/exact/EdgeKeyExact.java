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

class EdgeKeyExact implements Comparable<EdgeKeyExact> {
  
  public static EdgeKeyExact create(EdgeExact edge) {
    return new EdgeKeyExact(edge);
  }
    
  private OverlayPoint p0;
  private OverlayPoint p1;

  public EdgeKeyExact(EdgeExact edge) {
    initPoints(edge);
  }

  private void initPoints(EdgeExact edge) {
    boolean direction = edge.direction();
    if (direction) {
      init(edge.getCoordinate(0), edge.getCoordinate(edge.firstDistinctIndex(true)));
    }
    else {
      int len = edge.size();
      init(edge.getCoordinate(len - 1), edge.getCoordinate(edge.firstDistinctIndex(false)));
    }
  }

  private void init(OverlayPoint p0, OverlayPoint p1) {
    this.p0 = p0;
    this.p1 = p1;
  }

  @Override
  public int compareTo(EdgeKeyExact ek) {
    int cmp = p0.compareTo(ek.p0);
    if (cmp != 0) return cmp;
    return p1.compareTo(ek.p1);
  }
  
  public boolean equals(Object o) {
    if (! (o instanceof EdgeKeyExact)) {
      return false;
    }
    EdgeKeyExact ek = (EdgeKeyExact) o;
    return p0.equals(ek.p0) && p1.equals(ek.p1);
  }
  
  public int hashCode() {
    int result = 17;
    result = 37 * result + p0.hashCode();
    result = 37 * result + p1.hashCode();
    return result;
  }
  
  public String toString() {
    return "EdgeKeyExact(" + p0 + ", " + p1 + ")";
  }
}
