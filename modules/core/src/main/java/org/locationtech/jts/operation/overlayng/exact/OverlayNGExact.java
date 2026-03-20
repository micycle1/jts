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

import java.util.Collection;
import java.util.List;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.operation.overlay.OverlayOp;

/**
 * Computes the geometric overlay of two {@link Geometry}s using
 * Double-Double (DD) precision for noding and topology construction.
 * <p>
 * This class is a variant of {@link OverlayNG} for cases where
 * full floating-precision overlay can produce incorrect topology
 * without necessarily throwing a robustness error.
 * In particular, even when the input linework is successfully noded,
 * overlay can still fail if constructed intersection vertices are
 * rounded to {@code double} too early.  This can perturb node identity,
 * split-edge ordering, or edge embedding in the topology graph,
 * and may cause local face inversions or other incorrect area results.
 * <p>
 * To avoid these failures, this implementation maintains DD precision
 * throughout the topology-building phase, including constructed vertices
 * created during noding.  Topological decisions are therefore made using
 * the higher-precision arrangement, rather than from rounded
 * {@code double}-precision coordinates.
 * <p>
 * Output geometries are still represented using standard JTS
 * {@code Coordinate} values, so the final coordinate representation is
 * approximate.  The intent of this class is to make the internal overlay
 * topology robust, while accepting the usual limitations of the
 * {@code double}-based output model.
 * <p>
 * Supports only:
 * <ul>
 * <li>floating precision models</li>
 * <li>binary overlay operations</li>
 * <li>area/area inputs</li>
 * </ul>
 */
public class OverlayNGExact {
  static final boolean STRICT_MODE_DEFAULT = false;
  
  public static final int INTERSECTION  = OverlayOp.INTERSECTION;
  public static final int UNION         = OverlayOp.UNION;
  public static final int DIFFERENCE    = OverlayOp.DIFFERENCE;
  public static final int SYMDIFFERENCE = OverlayOp.SYMDIFFERENCE;

  static boolean isResultOfOp(int overlayOpCode, int loc0, int loc1) {
    if (loc0 == Location.BOUNDARY) loc0 = Location.INTERIOR;
    if (loc1 == Location.BOUNDARY) loc1 = Location.INTERIOR;
    switch (overlayOpCode) {
    case INTERSECTION:
      return loc0 == Location.INTERIOR && loc1 == Location.INTERIOR;
    case UNION:
      return loc0 == Location.INTERIOR || loc1 == Location.INTERIOR;
    case DIFFERENCE:
      return loc0 == Location.INTERIOR && loc1 != Location.INTERIOR;
    case SYMDIFFERENCE:
      return (loc0 == Location.INTERIOR && loc1 != Location.INTERIOR)
          || (loc0 != Location.INTERIOR && loc1 == Location.INTERIOR);
    }
    return false;
  }

  public static Geometry overlay(Geometry geom0, Geometry geom1, int opCode) {
    OverlayNGExact ov = new OverlayNGExact(geom0, geom1, opCode);
    return ov.getResult();
  }

  private int opCode;
  private InputGeometry inputGeom;
  private GeometryFactory geomFact;
  private PrecisionModel pm;
  private boolean isStrictMode = STRICT_MODE_DEFAULT;
  private boolean isOptimized = true;
  private boolean isOutputEdges = false;
  private boolean isOutputResultEdges = false;
  private boolean isOutputNodedEdges = false;

  public OverlayNGExact(Geometry geom0, Geometry geom1, int opCode) {
    this.pm = geom0.getFactory().getPrecisionModel();
    if (!OverlayUtil.isFloating(pm)) {
       throw new IllegalArgumentException("OverlayNGExact requires floating precision model");
    }
    this.opCode = opCode;
    geomFact = geom0.getFactory();
    inputGeom = new InputGeometry( geom0, geom1 );
    if (!inputGeom.isArea(0) || !inputGeom.isArea(1)) {
        throw new IllegalArgumentException("OverlayNGExact currently only supports Area/Area overlay");
    }
  }  

  public void setStrictMode(boolean isStrictMode) {
    this.isStrictMode = isStrictMode;
  }
  
  public void setOptimized(boolean isOptimized) {
    this.isOptimized = isOptimized;
  }
  
  public Geometry getResult() {
    if (OverlayUtil.isEmptyResult(opCode, 
        inputGeom.getGeometry(0), 
        inputGeom.getGeometry(1),
        pm)) {
      return OverlayUtil.createEmptyResult(OverlayUtil.resultDimension(opCode, inputGeom.getDimension(0), inputGeom.getDimension(1)), geomFact); 
    }

    ElevationModel elevModel = ElevationModel.create(inputGeom.getGeometry(0), inputGeom.getGeometry(1));
    Geometry result = computeEdgeOverlay();
    
    elevModel.populateZ(result);
    return result;
  }
  
  private Geometry computeEdgeOverlay() {
    List<EdgeExact> edges = nodeEdges();
    
    OverlayGraphExact graph = buildGraph(edges);
    
    if (isOutputNodedEdges) {
      // Currently not implemented for Exact: returns empty geometry
      return geomFact.createGeometryCollection();
    }

    labelGraph(graph);
    
    if (isOutputEdges || isOutputResultEdges) {
      // Currently not implemented for Exact: returns empty geometry
      return geomFact.createGeometryCollection();
    }
    
    Geometry result = extractResult(opCode, graph);
    
    boolean isAreaConsistent = OverlayUtil.isResultAreaConsistent(inputGeom.getGeometry(0), inputGeom.getGeometry(1), opCode, result);
    if (! isAreaConsistent)
      throw new TopologyException("Result area inconsistent with overlay operation");    
    
    return result;
  }

  private List<EdgeExact> nodeEdges() {
    ExactEdgeNodingBuilder nodingBuilder = new ExactEdgeNodingBuilder();
    
    if ( isOptimized ) {
      Envelope clipEnv = OverlayUtil.clippingEnvelope(opCode, inputGeom, pm);
      if (clipEnv != null)
        nodingBuilder.setClipEnvelope( clipEnv );
    }
    
    List<EdgeExact> mergedEdges = nodingBuilder.build(
        inputGeom.getGeometry(0), 
        inputGeom.getGeometry(1));
    
    inputGeom.setCollapsed(0, ! nodingBuilder.hasEdgesFor(0) );
    inputGeom.setCollapsed(1, ! nodingBuilder.hasEdgesFor(1) );
    
    return mergedEdges;
  }

  private OverlayGraphExact buildGraph(Collection<EdgeExact> edges) {
    OverlayGraphExact graph = new OverlayGraphExact();
    for (EdgeExact e : edges) {
      graph.addEdge(e.getCoordinates(), e.createLabel());
    }
    return graph;
  }
  
  private void labelGraph(OverlayGraphExact graph) {
    OverlayLabellerExact labeller = new OverlayLabellerExact(graph, inputGeom);
    labeller.computeLabelling();
    labeller.markResultAreaEdges(opCode);
    labeller.unmarkDuplicateEdgesFromResultArea();
  }

  private Geometry extractResult(int opCode, OverlayGraphExact graph) {
    boolean isAllowMixedIntResult = ! isStrictMode;
    
    List<OverlayEdgeExact> resultAreaEdges = graph.getResultAreaEdges();
    PolygonBuilderExact polyBuilder = new PolygonBuilderExact(resultAreaEdges, geomFact);
    List<Polygon> resultPolyList = polyBuilder.getPolygons();
    boolean hasResultAreaComponents = resultPolyList.size() > 0;
    
    List<LineString> resultLineList = null;
    
    boolean isAllowLines = ! isStrictMode || inputGeom.isLine(0) || inputGeom.isLine(1);
    if (opCode == INTERSECTION && isAllowMixedIntResult) {
      isAllowLines = true;
    }
    if (hasResultAreaComponents && opCode == INTERSECTION && ! isAllowMixedIntResult) {
      isAllowLines = false;
    }

    if (isAllowLines) {
      LineBuilderExact lineBuilder = new LineBuilderExact(inputGeom, graph, hasResultAreaComponents, opCode, geomFact);
      lineBuilder.setStrictMode(isStrictMode);
      resultLineList = lineBuilder.getLines();
    }
    
    return OverlayUtil.createResultGeometry(resultPolyList, resultLineList, null, geomFact);
  }
}
