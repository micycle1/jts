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

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.overlayng.OverlayNG;
import org.locationtech.jts.operation.overlay.OverlayOp;
import org.locationtech.jts.operation.valid.IsValidOp;
import junit.textui.TestRunner;
import test.jts.GeometryTestCase;

public class OverlayNGExactTest extends GeometryTestCase {

  public static void main(String args[]) {
    TestRunner.run(OverlayNGExactTest.class);
  }

  public OverlayNGExactTest(String name) { super(name); }

  public void testIntersectionSimple() {
    Geometry a = read("POLYGON ((0 0, 0 10, 10 10, 10 0, 0 0))");
    Geometry b = read("POLYGON ((5 5, 5 15, 15 15, 15 5, 5 5))");
    
    Geometry result = OverlayNGExact.overlay(a, b, OverlayOp.INTERSECTION);
    Geometry expected = read("POLYGON ((10 10, 10 5, 5 5, 5 10, 10 10))");
    assertTrue(result.equalsExact(expected));
  }

  public void testUnionSimple() {
    Geometry a = read("POLYGON ((0 0, 0 10, 10 10, 10 0, 0 0))");
    Geometry b = read("POLYGON ((5 5, 5 15, 15 15, 15 5, 5 5))");
    
    Geometry result = OverlayNGExact.overlay(a, b, OverlayOp.UNION);
    Geometry expected = read("POLYGON ((0 10, 5 10, 5 15, 15 15, 15 5, 10 5, 10 0, 0 0, 0 10))");
    assertTrue(result.equalsExact(expected));
  }

  public void testDifferenceSimple() {
    Geometry a = read("POLYGON ((0 0, 0 10, 10 10, 10 0, 0 0))");
    Geometry b = read("POLYGON ((5 5, 5 15, 15 15, 15 5, 5 5))");
    
    Geometry result = OverlayNGExact.overlay(a, b, OverlayOp.DIFFERENCE);
    Geometry expected = read("POLYGON ((0 10, 5 10, 5 5, 10 5, 10 0, 0 0, 0 10))");
    assertTrue(result.equalsExact(expected));
  }

  public void testUnionNearCoincidentSharedVertex() {
    Geometry a = read("POLYGON ((446 -207, 315 -75.99999999999999, 557 -76, 446 -207))");
    Geometry b = read("POLYGON ((184 -207, 315 -76, 446 -207, 315 -338, 184 -207))");

    Geometry result = OverlayNGExact.overlay(a, b, OverlayOp.UNION);
    assertTrue(result.isValid());
  }

  public void testUnionDuplicateStartNodeAfterExactNoding() {
    Geometry a = read("POLYGON ((458435 7432225, 458435 7432245, 458455 7432245, 458455 7432225, 458435 7432225))");
    Geometry b = read("POLYGON ((458500 7432200, 458400 7432200, 458400 7432300, 458500 7432300, 458500 7432200), (458454.7221242461 7432226.377759292, 458454.13999999897 7432223.769999999, 458455.62000000087 7432230.3999999985, 458454.7221242461 7432226.377759292))");

    Geometry result = OverlayNGExact.overlay(a, b, OverlayOp.UNION);
    IsValidOp validOp = new IsValidOp(result);
    String err = validOp.getValidationError() == null ? "valid" : validOp.getValidationError().toString();
    assertTrue(result.toText() + " :: " + err, result.isValid());
  }

  /**
   * Exact overlay sees only a point touch here.
   * Floating overlay may report an area because one nominally shared vertex differs by ~4.5e-13 in X.
   */
  public void testIntersectionNearCoincidentVertexIsEmpty() {
    Geometry c1 = read("POLYGON((0.0 65905.78568220709, 12540.144108116785 66644.10887217366, "
        + "13639.90993528687 64693.73062699103, 13323.160413385054 61494.1194419435, "
        + "2375.0223287135154 53673.4087205281, 0.0 52759.58192468053, 0.0 65905.78568220709))");
    Geometry c2 = read("POLYGON((23303.577415035626 55323.60484150198, 19851.52938808218 49637.68131132904, "
        + "2375.022328713516 53673.4087205281, 13323.160413385054 61494.1194419435, "
        + "23303.577415035626 55323.60484150198))");

    Geometry result = OverlayNGExact.overlay(c1, c2, OverlayNG.INTERSECTION);
    assertTrue(result.isValid());
    assertTrue(result.toText(), result.isEmpty());
  }

  public void testTriangleIntersectionFloatingCompatibility() {
    checkExactOverlay(
        "POLYGON ((0 0, 8 0, 8 3, 0 0))",
        "POLYGON ((0 5, 5 0, 0 0, 0 5))",
        OverlayNG.INTERSECTION,
        "POLYGON ((0 0, 3.6363636363636367 1.3636363636363638, 5 0, 0 0))",
        1e-10);
  }

  public void testPolygonWithRepeatedPointIntersectionSimpleFloatingCompatibility() {
    checkExactOverlay(
        "POLYGON ((100 200, 200 200, 200 100, 100 100, 100 151, 100 151, 100 151, 100 151, 100 200))",
        "POLYGON ((300 200, 300 100, 200 100, 200 200, 200 200, 300 200))",
        OverlayNG.INTERSECTION,
        "LINESTRING (200 200, 200 100)",
        1e-10);
  }

  public void testPolygonWithRepeatedPointIntersectionFloatingRobustness() {
    Geometry a = read("POLYGON ((1231646.6575 1042601.8724999996, 1231646.6575 1042601.8724999996, 1231646.6575 1042601.8724999996, 1231646.6575 1042601.8724999996, 1231646.6575 1042601.8724999996, 1231646.6575 1042601.8724999996, 1231646.6575 1042601.8724999996, 1231646.6575 1042601.8724999996, 1231647.72 1042600.4349999996, 1231653.22 1042592.1849999996, 1231665.14087406 1042572.5988970799, 1231595.8411746 1042545.58898314, 1231595.26811297 1042580.9672385901, 1231595.2825 1042582.8724999996, 1231646.6575 1042601.8724999996))");
    Geometry b = read("POLYGON ((1231665.14087406 1042572.5988970799, 1231665.14087406 1042572.5988970799, 1231665.14087406 1042572.5988970799, 1231665.14087406 1042572.5988970799, 1231665.14087406 1042572.5988970799, 1231665.14087406 1042572.5988970799, 1231665.14087406 1042572.5988970799, 1231665.14087406 1042572.5988970799, 1231666.51617512 1042570.3392651202, 1231677.47 1042558.9349999996, 1231685.50958834 1042553.8506523697, 1231603.31532446 1042524.6022436405, 1231603.31532446 1042524.6022436405, 1231603.31532446 1042524.6022436405, 1231603.31532446 1042524.6022436405, 1231596.4075 1042522.1849999996, 1231585.07346906 1042541.8167165304, 1231586.62051091 1042542.3586940402, 1231586.62051091 1042542.3586940402, 1231595.8411746 1042545.58898314, 1231665.14087406 1042572.5988970799))");

    Geometry result = OverlayNGExact.overlay(a, b, OverlayNG.INTERSECTION);
    assertTrue(result.isValid());
    assertTrue("Area of intersection result area is too large", result.getArea() < 1);
  }

  public void testSegmentNodeOrderingIntersectionRobustness() {
    checkExactOverlayValid(
        "POLYGON ((654948.3853299792 1794977.105854025, 655016.3812220972 1794939.918901604, 655016.2022581929 1794940.1099794197, 655014.9264068712 1794941.4254068714, 655014.7408834674 1794941.6101225375, 654948.3853299792 1794977.105854025))",
        "POLYGON ((655103.6628454948 1794805.456674405, 655016.20226 1794940.10998, 655014.8317182435 1794941.5196832407, 655014.8295602322 1794941.5218318563, 655014.740883467 1794941.610122538, 655016.6029214273 1794938.7590508445, 655103.6628454948 1794805.456674405))",
        OverlayNG.INTERSECTION);
  }

  /**
   * Current limitation: exact overlay still fails this clipping-perturbation case
   * during shell/hole assignment.
   */
  public void xtestPolygonsWithClippingPerturbationIntersectionRobustness() {
    Geometry a = read("POLYGON ((4373089.33 5521847.89, 4373092.24 5521851.6, 4373118.52 5521880.22, 4373137.58 5521896.63, 4373153.33 5521906.43, 4373270.51 5521735.67, 4373202.5 5521678.73, 4373100.1 5521827.97, 4373089.33 5521847.89))");
    Geometry b = read("POLYGON ((4373225.587574724 5521801.132991467, 4373209.219497436 5521824.985294571, 4373355.5585138 5521943.53124194, 4373412.83157427 5521860.49206234, 4373412.577392304 5521858.140878815, 4373412.290476093 5521855.48690386, 4373374.245799139 5521822.532711867, 4373271.028377312 5521736.104060946, 4373225.587574724 5521801.132991467))");

    Geometry result = OverlayNGExact.overlay(a, b, OverlayNG.INTERSECTION);
    assertTrue(result.isValid());
    assertTrue("Area of intersection result area is too large", result.getArea() < 1);
  }

  public void testPolygonsWithClippingPerturbation2IntersectionRobustness() {
    Geometry a = read("POLYGON ((4379891.12 5470577.74, 4379875.16 5470581.54, 4379841.77 5470592.88, 4379787.53 5470612.89, 4379822.96 5470762.6, 4379873.52 5470976.3, 4379982.93 5470965.71, 4379936.91 5470771.25, 4379891.12 5470577.74))");
    Geometry b = read("POLYGON ((4379894.528437099 5470592.144163859, 4379968.579210246 5470576.004727546, 4379965.600743549 5470563.403176092, 4379965.350009631 5470562.383524827, 4379917.641365346 5470571.523966022, 4379891.224959933 5470578.183564024, 4379894.528437099 5470592.144163859))");

    Geometry result = OverlayNGExact.overlay(a, b, OverlayNG.INTERSECTION);
    assertTrue(result.isValid());
    assertTrue("Area of intersection result area is too large", result.getArea() < 1);
  }

  private void checkExactOverlay(String wktA, String wktB, int opCode, String wktExpected, double tolerance) {
    Geometry a = read(wktA);
    Geometry b = read(wktB);
    Geometry expected = read(wktExpected);
    Geometry actual = OverlayNGExact.overlay(a, b, opCode);
    assertTrue(actual.isValid());
    checkEqual(expected, actual, tolerance);
  }

  private void checkExactOverlayValid(String wktA, String wktB, int opCode) {
    Geometry a = read(wktA);
    Geometry b = read(wktB);
    Geometry actual = OverlayNGExact.overlay(a, b, opCode);
    assertTrue(actual.isValid());
  }
}
