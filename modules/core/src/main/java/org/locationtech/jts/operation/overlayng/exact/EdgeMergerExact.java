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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.util.Assert;

class EdgeMergerExact {
 
  public static List<EdgeExact> merge(List<EdgeExact> edges) {
    List<EdgeExact> mergedEdges = new ArrayList<EdgeExact>();
    Map<EdgeKeyExact, EdgeExact> edgeMap = new HashMap<EdgeKeyExact, EdgeExact>();

    for (EdgeExact edge : edges) {
      EdgeKeyExact edgeKey = EdgeKeyExact.create(edge);
      EdgeExact baseEdge = edgeMap.get(edgeKey);
      if (baseEdge == null) {
        edgeMap.put(edgeKey, edge);
        mergedEdges.add(edge);
      }
      else {
        Assert.isTrue(baseEdge.size() == edge.size(),
            "Merge of edges of different sizes - probable noding error.");
        
        baseEdge.merge(edge);
      }
    }
    return mergedEdges;
  }

}
