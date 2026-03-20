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


class ExactSegmentNode implements Comparable<ExactSegmentNode> {
  private final ExactNodedSegmentString segString;
  public final OverlayPoint exactCoord;
  public final int segmentIndex;
  private final int segmentOctant;
  private final boolean isInterior;

  public ExactSegmentNode(ExactNodedSegmentString segString, OverlayPoint exactCoord, int segmentIndex, int segmentOctant) {
    this.segString = segString;
    this.exactCoord = exactCoord;
    this.segmentIndex = segmentIndex;
    this.segmentOctant = segmentOctant;
    
    // Check interior status by comparing with the start point of the segment.
    OverlayPoint segStart = segString.getExactCoordinate(segmentIndex);
    this.isInterior = !exactCoord.equals(segStart);
  }

  public boolean isInterior() { return isInterior; }

  @Override
  public int compareTo(ExactSegmentNode other) {
    if (segmentIndex < other.segmentIndex) return -1;
    if (segmentIndex > other.segmentIndex) return 1;

    if (exactCoord.equals(other.exactCoord)) return 0;

    // an exterior node is the segment start point, so always sorts first
    if (!isInterior) return -1;
    if (!other.isInterior) return 1;

    return compare(segmentOctant, exactCoord, other.exactCoord);
  }

  private static int compare(int octant, OverlayPoint p0, OverlayPoint p1) {
    if (p0.equals(p1)) return 0;

    int xSign = p0.getX().compareTo(p1.getX());
    int ySign = p0.getY().compareTo(p1.getY());

    switch (octant) {
      case 0: return compareValue(xSign, ySign);
      case 1: return compareValue(ySign, xSign);
      case 2: return compareValue(ySign, -xSign);
      case 3: return compareValue(-xSign, ySign);
      case 4: return compareValue(-xSign, -ySign);
      case 5: return compareValue(-ySign, -xSign);
      case 6: return compareValue(-ySign, xSign);
      case 7: return compareValue(xSign, -ySign);
    }
    return 0;
  }

  private static int compareValue(int compareSign0, int compareSign1) {
    if (compareSign0 < 0) return -1;
    if (compareSign0 > 0) return 1;
    if (compareSign1 < 0) return -1;
    if (compareSign1 > 0) return 1;
    return 0;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ExactSegmentNode)) return false;
    ExactSegmentNode other = (ExactSegmentNode) obj;
    return segmentIndex == other.segmentIndex && exactCoord.equals(other.exactCoord);
  }
  
  @Override
  public int hashCode() {
    return 31 * segmentIndex + exactCoord.hashCode();
  }

  @Override
  public String toString() {
    return segmentIndex + ":" + exactCoord.toString();
  }
}
