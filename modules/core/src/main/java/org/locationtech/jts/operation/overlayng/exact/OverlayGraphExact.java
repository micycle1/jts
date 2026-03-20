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

import org.locationtech.jts.operation.overlayng.OverlayLabel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class OverlayGraphExact {
  
  private List<OverlayEdgeExact> edges = new ArrayList<OverlayEdgeExact>();
  private Map<OverlayPoint, OverlayEdgeExact> nodeMap = new HashMap<OverlayPoint, OverlayEdgeExact>();
  
  public OverlayGraphExact() {
  }

  public Collection<OverlayEdgeExact> getEdges() {
    return edges;
  }
  
  public Collection<OverlayEdgeExact> getNodeEdges() {
    return nodeMap.values();
  }

  public OverlayEdgeExact getNodeEdge(OverlayPoint nodePt) {
    return nodeMap.get(nodePt);
  }
  
  public List<OverlayEdgeExact> getResultAreaEdges() {
    List<OverlayEdgeExact> resultEdges = new ArrayList<OverlayEdgeExact>();
    for (OverlayEdgeExact edge : getEdges()) {
      if (edge.isInResultArea()) {
        resultEdges.add(edge);
      }
    } 
    return resultEdges;
  }
  
  public OverlayEdgeExact addEdge(OverlayPoint[] pts, OverlayLabel label) {
    OverlayEdgeExact e = OverlayEdgeExact.createEdgePair(pts, label);
    insert(e);
    insert(e.symOE());
    return e;
  }
 
  private void insert(OverlayEdgeExact e) {
    edges.add(e);
    
    OverlayEdgeExact nodeEdge = nodeMap.get(e.orig());
    if (nodeEdge != null) {
      nodeEdge.insert(e);
    }
    else {
      nodeMap.put(e.orig(), e);
    }
  }
}
