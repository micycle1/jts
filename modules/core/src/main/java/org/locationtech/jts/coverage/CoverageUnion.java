/*
 * Copyright (c) 2022 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.coverage;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.TopologyException;

/**
 * Unions a valid coverage of polygons or lines in an efficient way.
 * <p>
 * A <b>polygonal coverage</b> is a collection of {@link org.locationtech.jts.geom.Polygon}s
 * which satisfy the following conditions:
 * <ol>
 * <li><b>Vector-clean</b> - Line segments within the collection
 * must either be identical or intersect only at endpoints.
 * <li><b>Non-overlapping</b> - No two polygons
 * may overlap. Equivalently, polygons must be interior-disjoint.
 * </ol>
 * <p>
 * A <b>linear coverage</b> is a collection of {@link org.locationtech.jts.geom.LineString}s
 * which satisfies the <b>Vector-clean</b> condition.
 * Note that this does not require the LineStrings to be fully noded
 * - i.e. they may contain coincident linework.
 * Coincident line segments are dissolved by the union.
 * <p>
 * No checking is done to determine whether the input is a valid coverage.
 * If the input is not a valid coverage 
 * then in <i>some</i> cases this will be detected during processing 
 * and a {@link org.locationtech.jts.geom.TopologyException} is thrown.
 * Otherwise, the computation will produce output, but it will be invalid.
 * 
 * @author Martin Davis
 * 
 * @see CoverageValidator
 *
 */
public class CoverageUnion {
  /**
   * Unions a polygonal coverage.
   * 
   * @param coverage the polygons in the coverage
   * @return the union of the coverage polygons
   *
   * @throws TopologyException in some cases if the coverage is invalid
   */
  public static Geometry union(Geometry[] coverage) {
    // union of an empty coverage is null, since no factory is available
    if (coverage.length == 0)
      return null;
    
    GeometryFactory geomFact = coverage[0].getFactory();
    GeometryCollection geoms = geomFact.createGeometryCollection(coverage);
    return org.locationtech.jts.operation.overlayng.CoverageUnion.union(geoms);
  }

  /**
   * Unions a valid polygonal coverage or linear network.
   *
   * @param coverage a coverage of polygons or lines
   * @return the union of the coverage
   *
   * @throws TopologyException in some cases if the coverage is invalid
   */
  public static Geometry union(Geometry coverage) {
    return org.locationtech.jts.operation.overlayng.CoverageUnion.union(coverage);
  }

  private CoverageUnion() {
  }
}
