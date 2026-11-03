"""Standalone targeted check; does not run Gradle, install a mod, or start Minecraft."""
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[3]
java = Path('E:/JAVA/jdk-25.0.2/bin')
harness = '''package com.poisoncircle;
public class MapGeometryCheck {
 public static void main(String[] args) {
  for (boolean round : new boolean[]{false,true}) {
   for (double[] circle : new double[][]{{0,0,20},{70,0,50},{-70,40,80},{0,0,150},{0,0,0},{0,0,Double.NaN}}) {
    int[] count = {0};
    MapCircleGeometry.draw(circle[0],circle[1],circle[2],1,50,round,(ax,ay,bx,by,cx,cy,dx,dy)->{
     for (double[] p : new double[][]{{ax,ay},{bx,by},{cx,cy},{dx,dy}}) {
      if (!Double.isFinite(p[0]) || !Double.isFinite(p[1])) throw new AssertionError("nonfinite");
      if (Math.abs(p[0])>50.000001 || Math.abs(p[1])>50.000001) throw new AssertionError("square overflow");
      if (round && Math.hypot(p[0],p[1])>50.000001) throw new AssertionError("round overflow");
     }
     count[0]++;
    });
    if (circle[2]==20 && count[0]==0) throw new AssertionError("visible circle missing");
    if ((circle[2]==150 || circle[2]==0 || Double.isNaN(circle[2])) && count[0]!=0) throw new AssertionError("invisible circle drawn");
   }
  }
  System.out.println("Map geometry: square/round stroke clipping, offscreen centers, empty and invalid circles passed");
 }
}'''
with tempfile.TemporaryDirectory(prefix='poisoncircle-map-check-') as directory:
    target = Path(directory)
    source = target / 'MapGeometryCheck.java'
    source.write_text(harness, encoding='utf-8')
    subprocess.run([str(java/'javac.exe'), '-d', str(target),
                    str(root/'src/client/java/com/poisoncircle/MapCircleGeometry.java'), str(source)], check=True)
    subprocess.run([str(java/'java.exe'), '-cp', str(target), 'com.poisoncircle.MapGeometryCheck'], check=True)
mini = (root/'src/client/java/com/poisoncircle/XaeroMinimapCircleOverlay.java').read_text(encoding='utf-8')
world = (root/'src/client/java/com/poisoncircle/XaeroWorldMapCircleOverlay.java').read_text(encoding='utf-8')
assert 'circle.radius * terrainScale' in mini and 'mapPose.set(' in mini
assert 'circle.radius * info.scale' in world and 'previousX + x' in world
assert 'currentDetectorReveal()' in mini and 'currentDetectorReveal()' in world
print('Overlay integration: terrain scale, unclamped minimap pose, world residual offsets and expiry accessor retained')
pause = (root/'src/client/java/com/poisoncircle/mixin/XaeroWorldMapScreenMixin.java').read_text(encoding='utf-8')
assert '@Pseudo' in pause and '@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)' in pause
assert '@Override\n    public boolean isPauseScreen()' in pause
assert '!PoisonCircleClient.hasActiveCircle() && super.isPauseScreen()' in pause
print('World map pause regression: optional Xaero screen override keeps active circles ticking and preserves normal pause otherwise')
