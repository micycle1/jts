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
import org.locationtech.jts.noding.SegmentString;

class ExactNodedSegmentString implements SegmentString {
  private final OverlayPoint[] exactPts;
  private Coordinate[] cachedPts;
  private final Object data;
  private ExactSegmentNodeList nodeList;

  public ExactNodedSegmentString(OverlayPoint[] exactPts, Object data) {
    this.exactPts = exactPts;
    this.data = data;
    this.nodeList = new ExactSegmentNodeList(this);
  }

  public ExactSegmentNodeList getNodeList() {
    return nodeList;
  }

  public void addIntersection(OverlayPoint exactPt, int segmentIndex) {
    nodeList.add(exactPt, segmentIndex);
  }

  public OverlayPoint getExactCoordinate(int i) {
    return exactPts[i];
  }

  public OverlayPoint[] getExactCoordinates() {
    return exactPts;
  }

  @Override
  public Object getData() {
    return data;
  }

  @Override
  public void setData(Object data) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Coordinate[] getCoordinates() {
    if (cachedPts == null) {
      cachedPts = new Coordinate[exactPts.length];
      for (int i = 0; i < exactPts.length; i++) {
        cachedPts[i] = exactPts[i].getCoordinate();
      }
    }
    return cachedPts;
  }

  @Override
  public int size() {
    return exactPts.length;
  }

  @Override
  public Coordinate getCoordinate(int i) {
    return getCoordinates()[i];
  }

  @Override
  public boolean isClosed() {
    return exactPts[0].equals(exactPts[exactPts.length - 1]);
  }
}
