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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Dimension;
import org.locationtech.jts.geom.Location;

class EdgeExact {
  
  public static boolean isCollapsed(OverlayPoint[] pts) {
    if (pts.length < 2) return true;
    if (pts[0].equals(pts[1])) return true;
    if (pts.length > 2) {
      if (pts[pts.length-1].equals(pts[pts.length - 2])) return true;
    }
    return false;
  }
  
  private OverlayPoint[] pts;
  
  private int aDim = OverlayLabel.DIM_UNKNOWN;
  private int aDepthDelta = 0;
  private boolean aIsHole = false;
  
  private int bDim = OverlayLabel.DIM_UNKNOWN;
  private int bDepthDelta = 0;
  private boolean bIsHole = false;

  public EdgeExact(OverlayPoint[] pts, EdgeSourceInfo info) {
    this.pts = pts;
    copyInfo(info);
  }
  
  public OverlayPoint[] getCoordinates() {
    return pts;
  }

  public OverlayPoint getCoordinate(int index) {
    return pts[index];
  }
  
  public int size() {
    return pts.length;
  }
  
  public boolean direction() {
    if (pts.length < 2) {
      throw new IllegalStateException("Edge must have >= 2 points");
    }
    int i0 = firstDistinctIndex(true);
    int i1 = firstDistinctIndex(false);
    if (i0 < 0 || i1 < 0) {
      throw new IllegalStateException("Edge direction cannot be determined because all points are equal");
    }
    OverlayPoint p0 = pts[0];
    OverlayPoint p1 = pts[i0];
    
    OverlayPoint pn0 = pts[pts.length - 1];
    OverlayPoint pn1 = pts[i1];
    
    int cmp = 0;
    int cmp0 = p0.compareTo(pn0);
    if (cmp0 != 0) cmp = cmp0;
    
    if (cmp == 0) {
      int cmp1 = p1.compareTo(pn1);
      if (cmp1 != 0) cmp = cmp1;
    }
    
    if (cmp == 0) {
      throw new IllegalStateException("Edge direction cannot be determined because endpoints are equal");
    }
    
    return cmp == -1;
  }

  public int firstDistinctIndex(boolean isForward) {
    if (isForward) {
      for (int i = 1; i < pts.length; i++) {
        if (! pts[0].equals(pts[i])) {
          return i;
        }
      }
      return -1;
    }
    for (int i = pts.length - 2; i >= 0; i--) {
      if (! pts[pts.length - 1].equals(pts[i])) {
        return i;
      }
    }
    return -1;
  }

  public boolean relativeDirection(EdgeExact edge2) {
    if (! getCoordinate(0).equals(edge2.getCoordinate(0)))
      return false;
    if (! getCoordinate(1).equals(edge2.getCoordinate(1)))
      return false;
    return true;
  }
  
  public OverlayLabel createLabel() {
    OverlayLabel lbl = new OverlayLabel();
    initLabel(lbl, 0, aDim, aDepthDelta, aIsHole);
    initLabel(lbl, 1, bDim, bDepthDelta, bIsHole);
    return lbl;
  }
  
  private static void initLabel(OverlayLabel lbl, int geomIndex, int dim, int depthDelta, boolean isHole) {
    int dimLabel = labelDim(dim, depthDelta);
    
    switch (dimLabel) {
    case OverlayLabel.DIM_NOT_PART:
      lbl.initNotPart(geomIndex);
      break;
    case OverlayLabel.DIM_BOUNDARY: 
      lbl.initBoundary(geomIndex, locationLeft(depthDelta), locationRight(depthDelta), isHole);
      break;
    case OverlayLabel.DIM_COLLAPSE: 
      lbl.initCollapse(geomIndex, isHole);
      break;
    case OverlayLabel.DIM_LINE:
      lbl.initLine(geomIndex);
      break;
    }
  }

  private static int labelDim(int dim, int depthDelta) {
    if (dim == Dimension.FALSE) 
      return OverlayLabel.DIM_NOT_PART;

    if (dim == Dimension.L) 
      return OverlayLabel.DIM_LINE;
    
    boolean isCollapse = depthDelta == 0;
    if (isCollapse) return OverlayLabel.DIM_COLLAPSE;
        
    return OverlayLabel.DIM_BOUNDARY;
  }
  
  private boolean isShell(int geomIndex) {
    if (geomIndex == 0) {
      return aDim == OverlayLabel.DIM_BOUNDARY && ! aIsHole;
    }
    return bDim == OverlayLabel.DIM_BOUNDARY && ! bIsHole;
  }
  
  private static int locationRight(int depthDelta) {
    int delSign = delSign(depthDelta);
    switch (delSign) {
    case 0: return OverlayLabel.LOC_UNKNOWN;
    case 1: return Location.INTERIOR;
    case -1: return Location.EXTERIOR;
    }
    return OverlayLabel.LOC_UNKNOWN;
  }

  private static int locationLeft(int depthDelta) {
    int delSign = delSign(depthDelta);
    switch (delSign) {
    case 0: return OverlayLabel.LOC_UNKNOWN;
    case 1: return Location.EXTERIOR;
    case -1: return Location.INTERIOR;
    }
    return OverlayLabel.LOC_UNKNOWN;
  }

  private static int delSign(int depthDel) {
    if(depthDel > 0) return 1;
    if (depthDel < 0) return -1;
    return 0;
  }

  private void copyInfo(EdgeSourceInfo info) {
    if (info.getIndex() == 0) {
      aDim = info.getDimension();
      aIsHole = info.isHole();
      aDepthDelta = info.getDepthDelta();
    }
    else {
      bDim = info.getDimension();
      bIsHole = info.isHole();
      bDepthDelta = info.getDepthDelta();
    }
  }
  
  public void merge(EdgeExact edge) {
    aIsHole = isHoleMerged(0, this, edge);
    bIsHole = isHoleMerged(1, this, edge);

    if (edge.aDim > aDim) aDim = edge.aDim;
    if (edge.bDim > bDim) bDim = edge.bDim;
    
    boolean relDir = relativeDirection(edge);
    int flipFactor = relDir ? 1 : -1;
    aDepthDelta += flipFactor * edge.aDepthDelta;
    bDepthDelta += flipFactor * edge.bDepthDelta;
  }

  private static boolean isHoleMerged(int geomIndex, EdgeExact edge1, EdgeExact edge2) {
    boolean isShell1 = edge1.isShell(geomIndex);
    boolean isShell2 = edge2.isShell(geomIndex);
    boolean isShellMerged = isShell1 || isShell2;
    return ! isShellMerged;
  }

  public String toString() {
    String ptsStr = "EdgeExact(" + pts.length + " pts)";
    String aInfo = infoString(0, aDim, aIsHole, aDepthDelta );
    String bInfo = infoString(1, bDim, bIsHole, bDepthDelta );

    return "Edge( " + ptsStr  + " ) " 
        + aInfo + "/" + bInfo;
  }

  public static String infoString(int index, int dim, boolean isHole, int depthDelta) {
    return
        (index == 0 ? "A:" : "B:")
        + OverlayLabel.dimensionSymbol(dim)
        + ringRoleSymbol( dim, isHole )
        + Integer.toString(depthDelta);
  }
  
  private static String ringRoleSymbol(int dim, boolean isHole) {
    if (hasAreaParent(dim)) return "" + OverlayLabel.ringRoleSymbol(isHole);
    return "";
  }
  
  private static boolean hasAreaParent(int dim) {
    return dim == OverlayLabel.DIM_BOUNDARY || dim == OverlayLabel.DIM_COLLAPSE;
  }
}
