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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateArrays;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.noding.MCIndexNoder;

class ExactEdgeNodingBuilder {

  private static final int MIN_LIMIT_PTS = 20;

  private List<ExactNodedSegmentString> inputEdges = new ArrayList<ExactNodedSegmentString>();
  private Envelope clipEnv = null;
  private RingClipper clipper;
  private LineLimiter limiter;

  private boolean[] hasEdges = new boolean[2];

  public ExactEdgeNodingBuilder() {
  }

  public void setClipEnvelope(Envelope clipEnv) {
    this.clipEnv = clipEnv;
    clipper = new RingClipper(clipEnv);
    limiter = new LineLimiter(clipEnv);
  }

  public boolean hasEdgesFor(int geomIndex) {
    return hasEdges[geomIndex];
  }

  public List<EdgeExact> build(Geometry geom0, Geometry geom1) {
    add(geom0, 0);
    add(geom1, 1);
    List<EdgeExact> nodedEdges = node(inputEdges);
    
    // Merge edges
    List<EdgeExact> mergedEdges = EdgeMergerExact.merge(nodedEdges);
    return mergedEdges;
  }
  
  private List<EdgeExact> node(List<ExactNodedSegmentString> segStrings) {
    MCIndexNoder noder = new MCIndexNoder();
    ExactLineIntersector li = new ExactLineIntersector();
    noder.setSegmentIntersector(new ExactIntersectionAdder(li));
    
    noder.computeNodes(segStrings);
    
    List<ExactNodedSegmentString> nodedSS = new ArrayList<ExactNodedSegmentString>();
    for (ExactNodedSegmentString ss : segStrings) {
      ss.getNodeList().addSplitEdges(nodedSS);
    }
    
    List<EdgeExact> edges = createEdges(nodedSS);
    return edges;
  }

  private List<EdgeExact> createEdges(Collection<ExactNodedSegmentString> segStrings) {
    List<EdgeExact> edges = new ArrayList<EdgeExact>();
    for (ExactNodedSegmentString ss : segStrings) {
      OverlayPoint[] pts = ss.getExactCoordinates();
      
      if (EdgeExact.isCollapsed(pts)) 
        continue;
      
      EdgeSourceInfo info = (EdgeSourceInfo) ss.getData();
      hasEdges[info.getIndex()] = true;
      edges.add(new EdgeExact(pts, info));
    }
    return edges;
  }
  
  private void add(Geometry g, int geomIndex) {
    if (g == null || g.isEmpty()) return;
    
    if (isClippedCompletely(g.getEnvelopeInternal())) 
      return;

    if (g instanceof Polygon)                 addPolygon((Polygon) g, geomIndex);
    else if (g instanceof LineString)         addLine((LineString) g, geomIndex);
    else if (g instanceof MultiLineString)    addCollection((MultiLineString) g, geomIndex);
    else if (g instanceof MultiPolygon)       addCollection((MultiPolygon) g, geomIndex);
    else if (g instanceof GeometryCollection) addGeometryCollection((GeometryCollection) g, geomIndex, g.getDimension());
  }
  
  private void addCollection(GeometryCollection gc, int geomIndex) {
    for (int i = 0; i < gc.getNumGeometries(); i++) {
      Geometry g = gc.getGeometryN(i);
      add(g, geomIndex);
    }
  }

  private void addGeometryCollection(GeometryCollection gc, int geomIndex, int expectedDim) {
    for (int i = 0; i < gc.getNumGeometries(); i++) {
      Geometry g = gc.getGeometryN(i);
      if (g.getDimension() != expectedDim) {
        throw new IllegalArgumentException("Overlay input is mixed-dimension");
      }
      add(g, geomIndex);
    }
  }

  private void addPolygon(Polygon poly, int geomIndex) {
    LinearRing shell = poly.getExteriorRing();
    addPolygonRing(shell, false, geomIndex);

    for (int i = 0; i < poly.getNumInteriorRing(); i++) {
      LinearRing hole = poly.getInteriorRingN(i);
      addPolygonRing(hole, true, geomIndex);
    }
  }

  private void addPolygonRing(LinearRing ring, boolean isHole, int index) {
    if (ring.isEmpty()) return;
    if (isClippedCompletely(ring.getEnvelopeInternal())) return;
    
    Coordinate[] pts = clip(ring);

    if (pts.length < 2) return;
    
    int depthDelta = computeDepthDelta(ring, isHole);
    EdgeSourceInfo info = new EdgeSourceInfo(index, depthDelta, isHole);
    addEdge(pts, info);
  }

  private boolean isClippedCompletely(Envelope env) {
    if (clipEnv == null) return false;
    return clipEnv.disjoint(env);
  }
  
  private Coordinate[] clip(LinearRing ring) {
    Coordinate[] pts = ring.getCoordinates();
    Envelope env = ring.getEnvelopeInternal();
    if (clipper == null || clipEnv.covers(env)) {
      return removeRepeatedPoints(ring);
    }
    return clipper.clip(pts);
  }
  
  private static Coordinate[] removeRepeatedPoints(LineString line) {
    Coordinate[] pts = line.getCoordinates();
    return CoordinateArrays.removeRepeatedPoints(pts);
  }
  
  private static int computeDepthDelta(LinearRing ring, boolean isHole) {
    boolean isCCW = Orientation.isCCW(ring.getCoordinateSequence());
    boolean isOriented = !isHole ? !isCCW : isCCW;
    return isOriented ? 1 : -1;
  }

  private void addLine(LineString line, int geomIndex) {
    if (line.isEmpty()) return;
    if (isClippedCompletely(line.getEnvelopeInternal())) return;
    
    if (isToBeLimited(line)) {
      List<Coordinate[]> sections = limit(line);
      for (Coordinate[] pts : sections) {
        addLine(pts, geomIndex);
      }
    } else {
      Coordinate[] ptsNoRepeat = removeRepeatedPoints(line);
      addLine(ptsNoRepeat, geomIndex);
    }
  }

  private void addLine(Coordinate[] pts, int geomIndex) {
    if (pts.length < 2) return;
    EdgeSourceInfo info = new EdgeSourceInfo(geomIndex);
    addEdge(pts, info);
  }
  
  private void addEdge(Coordinate[] pts, EdgeSourceInfo info) {
    OverlayPoint[] exactPts = new OverlayPoint[pts.length];
    for(int i = 0; i < pts.length; i++) {
      exactPts[i] = new OverlayPoint(pts[i]);
    }
    ExactNodedSegmentString ss = new ExactNodedSegmentString(exactPts, info);
    inputEdges.add(ss);
  }

  private boolean isToBeLimited(LineString line) {
    Coordinate[] pts = line.getCoordinates();
    if (limiter == null || pts.length <= MIN_LIMIT_PTS) return false;
    Envelope env = line.getEnvelopeInternal();
    if (clipEnv.covers(env)) return false;
    return true;
  }

  private List<Coordinate[]> limit(LineString line) {
    Coordinate[] pts = line.getCoordinates();
    return limiter.limit(pts);
  }
}
