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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.locationtech.jts.math.DD;

class ExactSegmentNodeList {
  private ExactNodedSegmentString edge;
  private TreeMap<ExactSegmentNode, ExactSegmentNode> nodeMap = new TreeMap<>();

  public ExactSegmentNodeList(ExactNodedSegmentString edge) {
    this.edge = edge;
  }

  public ExactNodedSegmentString getEdge() {
    return edge;
  }

  public ExactSegmentNode add(OverlayPoint exactPt, int segmentIndex) {
    // Octant based on original coordinates
    int octant = -1;
    if (segmentIndex < edge.size() - 1) {
      OverlayPoint p0 = edge.getExactCoordinate(segmentIndex);
      OverlayPoint p1 = edge.getExactCoordinate(segmentIndex + 1);
      octant = getOctant(p0, p1);
    }

    ExactSegmentNode node = new ExactSegmentNode(edge, exactPt, segmentIndex, octant);
    ExactSegmentNode existingNode = nodeMap.get(node);
    if (existingNode == null) {
      nodeMap.put(node, node);
      return node;
    }
    return existingNode;
  }

  private static int getOctant(OverlayPoint p0, OverlayPoint p1) {
	    DD dx = p1.getX().subtract(p0.getX());
	    DD dy = p1.getY().subtract(p0.getY());
	    
	    if (dx.isZero() && dy.isZero()) return 0;

	    DD adx = dx.abs();
	    DD ady = dy.abs();
	    boolean adxGeAdy = adx.compareTo(ady) >= 0;

	    if (dx.signum() >= 0) {
	      if (dy.signum() >= 0) {
	        if (adxGeAdy) return 0;
	        else return 1;
	      } else {
	        if (adxGeAdy) return 7;
	        else return 6;
	      }
	    } else {
	      if (dy.signum() >= 0) {
	        if (adxGeAdy) return 3;
	        else return 2;
	      } else {
	        if (adxGeAdy) return 4;
	        else return 5;
	      }
	    }
	  }

  private void addEndpoints() {
    int maxSegIndex = edge.size() - 1;
    add(edge.getExactCoordinate(0), 0);
    add(edge.getExactCoordinate(maxSegIndex), maxSegIndex);
  }

  /**
   * Adds nodes for collapsed edge pairs caused either by inserted nodes
   * or by repeated vertices already present in the segment string.
   * This mirrors SegmentNodeList and is required for fully noded output.
   */
  private void addCollapsedNodes() {
    List<Integer> collapsedVertexIndexes = new ArrayList<Integer>();

    findCollapsesFromInsertedNodes(collapsedVertexIndexes);
    findCollapsesFromExistingVertices(collapsedVertexIndexes);

    for (Integer vertexIndex : collapsedVertexIndexes) {
      add(edge.getExactCoordinate(vertexIndex.intValue()), vertexIndex.intValue());
    }
  }

  private void findCollapsesFromExistingVertices(List<Integer> collapsedVertexIndexes) {
    for (int i = 0; i < edge.size() - 2; i++) {
      OverlayPoint p0 = edge.getExactCoordinate(i);
      OverlayPoint p2 = edge.getExactCoordinate(i + 2);
      if (p0.equals(p2)) {
        collapsedVertexIndexes.add(Integer.valueOf(i + 1));
      }
    }
  }

  private void findCollapsesFromInsertedNodes(List<Integer> collapsedVertexIndexes) {
    int[] collapsedVertexIndex = new int[1];
    Iterator<ExactSegmentNode> it = nodeMap.values().iterator();
    ExactSegmentNode prev = it.next();
    while (it.hasNext()) {
      ExactSegmentNode next = it.next();
      if (findCollapseIndex(prev, next, collapsedVertexIndex)) {
        collapsedVertexIndexes.add(Integer.valueOf(collapsedVertexIndex[0]));
      }
      prev = next;
    }
  }

  private boolean findCollapseIndex(ExactSegmentNode s0, ExactSegmentNode s1, int[] collapsedVertexIndex) {
    if (!s0.exactCoord.equals(s1.exactCoord)) {
      return false;
    }

    int numVerticesBetween = s1.segmentIndex - s0.segmentIndex;
    if (!s1.isInterior()) {
      numVerticesBetween--;
    }

    if (numVerticesBetween == 1) {
      collapsedVertexIndex[0] = s0.segmentIndex + 1;
      return true;
    }
    return false;
  }

  public void addSplitEdges(Collection<ExactNodedSegmentString> edgeList) {
    addEndpoints();
    addCollapsedNodes();

    Iterator<ExactSegmentNode> it = nodeMap.values().iterator();
    ExactSegmentNode s0 = it.next();
    ExactSegmentNode s1 = null;

    while (it.hasNext()) {
      s1 = it.next();
      createSplitEdge(s0, s1, edgeList);
      s0 = s1;
    }
  }

  private void createSplitEdge(ExactSegmentNode s0, ExactSegmentNode s1, Collection<ExactNodedSegmentString> edgeList) {
    int nPts = s1.segmentIndex - s0.segmentIndex + 2;
    if (nPts == 2) {
       edgeList.add(new ExactNodedSegmentString(new OverlayPoint[]{s0.exactCoord, s1.exactCoord}, edge.getData()));
       return;
    }
    
    OverlayPoint lastSegStartPt = edge.getExactCoordinate(s1.segmentIndex);
    boolean useIntPt1 = s1.isInterior() || ! s1.exactCoord.equals(lastSegStartPt);
    if (! useIntPt1) {
      nPts--;
    }

    OverlayPoint[] exactPts = new OverlayPoint[nPts];
    int ipt = 0;
    exactPts[ipt++] = s0.exactCoord;
    for (int i = s0.segmentIndex + 1; i <= s1.segmentIndex; i++) {
        exactPts[ipt++] = edge.getExactCoordinate(i);
    }
    if (useIntPt1) exactPts[ipt] = s1.exactCoord;
    
    // Create new split edge
    ExactNodedSegmentString splitEdge = new ExactNodedSegmentString(exactPts, edge.getData());
    edgeList.add(splitEdge);
  }
}
