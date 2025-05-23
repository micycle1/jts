/*
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.locationtech.jts.simplify;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;

/**
 * Represents a {@link LineString} which can be modified to a simplified shape.  
 * This class provides an attribute which specifies the minimum allowable length
 * for the modified result.
 * 
 * @version 1.7
 */
class TaggedLineString
{

  private LineString parentLine;
  private TaggedLineSegment[] segs;
  private List<LineSegment> resultSegs = new ArrayList<LineSegment>();
  private int minimumSize;
  private boolean isRing = true;

  public TaggedLineString(LineString parentLine, int minimumSize, boolean isRing) {
    this.parentLine = parentLine;
    this.minimumSize = minimumSize;
    this.isRing = isRing;
    init();
  }

  public boolean isRing() {
    return isRing;
  }
  
  public int getMinimumSize()  {    return minimumSize;  }
  public LineString getParent() { return parentLine; }
  public Coordinate[] getParentCoordinates() { return parentLine.getCoordinates(); }
  public Coordinate[] getResultCoordinates() { return extractCoordinates(resultSegs); }

  public Coordinate getCoordinate(int i) {
    return parentLine.getCoordinateN(i);
  }

  public int size() {
    return parentLine.getNumPoints();
  }
  
  /**
   * Returns a vertex of the component,
   * in either simplified or original form.
   * Once the component is simplified a vertex of the simplified linework
   * must be returned. 
   * Otherwise the simplified linework could be jumped by a flattened line
   * which does not cross an original vertex, and so is reported as valid.
   * 
   * @return a component vertex
   */
  public Coordinate getComponentPoint() {
    //-- simplified vertex
    if (resultSegs.size() > 0) 
      return resultSegs.get(0).p0;
    //-- original vertex
    return getParentCoordinates()[1];
  }
  
  public int getResultSize()
  {
    int resultSegsSize = resultSegs.size();
    return resultSegsSize == 0 ? 0 : resultSegsSize + 1;
  }

  public TaggedLineSegment getSegment(int i) { return segs[i]; }

  /**
   * Gets a segment of the result list.
   * Negative indexes can be used to retrieve from the end of the list.
   * @param i the segment index to retrieve
   * @return the result segment
   */
  public LineSegment getResultSegment(int i) { 
    int index = i;
    if (i < 0) {
      index = resultSegs.size() + i;
    }
    return (LineSegment) resultSegs.get(index);
  }

  private void init()
  {
    Coordinate[] pts = parentLine.getCoordinates();
    segs = new TaggedLineSegment[pts.length - 1];
    for (int i = 0; i < pts.length - 1; i++) {
      TaggedLineSegment seg
               = new TaggedLineSegment(pts[i], pts[i + 1], parentLine, i);
      segs[i] = seg;
    }
  }

  public TaggedLineSegment[] getSegments() { return segs; }

  /**
   * Add a simplified segment to the result.
   * This assumes simplified segments are computed in the order
   * they occur in the line.
   * 
   * @param seg the result segment to add
   */
  public void addToResult(LineSegment seg)
  {
    resultSegs.add(seg);
  }

  public LineString asLineString()
  {
    return parentLine.getFactory().createLineString(extractCoordinates(resultSegs));
  }

  public LinearRing asLinearRing() {
    return parentLine.getFactory().createLinearRing(extractCoordinates(resultSegs));
  }

  private static Coordinate[] extractCoordinates(List<LineSegment> segs)
  {
    Coordinate[] pts = new Coordinate[segs.size() + 1];
    LineSegment seg = null;
    for (int i = 0; i < segs.size(); i++) {
      seg = (LineSegment) segs.get(i);
      pts[i] = seg.p0;
    }
    // add last point
    pts[pts.length - 1] = seg.p1;
    return pts;
  }

  LineSegment removeRingEndpoint()
  {
    LineSegment firstSeg = (LineSegment) resultSegs.get(0);
    LineSegment lastSeg = (LineSegment) resultSegs.get(resultSegs.size() - 1);

    firstSeg.p0 = lastSeg.p0;
    resultSegs.remove(resultSegs.size() - 1);
    return firstSeg;
  }


}
