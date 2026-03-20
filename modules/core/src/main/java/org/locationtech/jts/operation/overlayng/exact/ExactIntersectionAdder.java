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

import org.locationtech.jts.noding.SegmentIntersector;
import org.locationtech.jts.noding.SegmentString;

class ExactIntersectionAdder implements SegmentIntersector {
  private ExactLineIntersector li;
  
  public ExactIntersectionAdder(ExactLineIntersector li) {
    this.li = li;
  }

  public static boolean isAdjacentSegments(int i1, int i2) {
    return Math.abs(i1 - i2) == 1;
  }

  private boolean isTrivialIntersection(SegmentString e0, int segIndex0, SegmentString e1, int segIndex1) {
    if (e0 == e1) {
      if (li.getIntersectionNum() == 1) {
        if (isAdjacentSegments(segIndex0, segIndex1))
          return true;
        if (e0.isClosed()) {
          int maxSegIndex = e0.size() - 1;
          if ((segIndex0 == 0 && segIndex1 == maxSegIndex) || (segIndex1 == 0 && segIndex0 == maxSegIndex)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  public void processIntersections(SegmentString e0, int segIndex0, SegmentString e1, int segIndex1) {
    if (e0 == e1 && segIndex0 == segIndex1) return;
    
    li.computeIntersection(e0.getCoordinate(segIndex0), e0.getCoordinate(segIndex0 + 1), 
                           e1.getCoordinate(segIndex1), e1.getCoordinate(segIndex1 + 1));
                           
    if (li.hasIntersection()) {
      if (!isTrivialIntersection(e0, segIndex0, e1, segIndex1)) {
        ExactNodedSegmentString en0 = (ExactNodedSegmentString) e0;
        ExactNodedSegmentString en1 = (ExactNodedSegmentString) e1;
        
        for (int i = 0; i < li.getIntersectionNum(); i++) {
          OverlayPoint exactPt = li.getExactIntersection(i);
          en0.addIntersection(exactPt, segIndex0);
          en1.addIntersection(exactPt, segIndex1);
        }
      }
    }
  }

  @Override
  public boolean isDone() {
    return false;
  }
}
