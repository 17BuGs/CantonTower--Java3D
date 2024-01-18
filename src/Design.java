import java.applet.Applet;
import java.awt.*;
import com.sun.j3d.utils.image.*;
import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.universe.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.behaviors.mouse.*;

public class Design extends Applet {
  public BranchGroup createBranchGroupSceneGraph() {
    BranchGroup BranchGroupRoot = new BranchGroup();
    BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);
    // Color3f bgColor = new Color3f(0.0f, 0.0f, 0.0f);
    // Background bg = new Background(bgColor);
    // 设置背景图片
    Background bg = new Background();
    TextureLoader loader = new TextureLoader(new String("bgImage.png"), null);
    bg.setImage(loader.getImage());
    bg.setImageScaleMode(Background.SCALE_FIT_ALL);
    bg.setColor(new Color3f(0.0f, 0.0f, 0.0f));
    bg.setApplicationBounds(bounds);
    BranchGroupRoot.addChild(bg);
    Color3f directionalColor = new Color3f(1.f, 1.f, 1.f);
    Vector3f vec = new Vector3f(0.f, 0.f, -1.0f);
    DirectionalLight directionalLight = new DirectionalLight(directionalColor, vec);
    directionalLight.setInfluencingBounds(bounds);
    BranchGroupRoot.addChild(directionalLight);
    Transform3D tr = new Transform3D();
    tr.setScale(0.06);
    tr.setTranslation(new Vector3f(0.f, -0.55f, 0.f));
    TransformGroup transformgroup = new TransformGroup(tr);
    transformgroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    transformgroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
    BranchGroupRoot.addChild(transformgroup);
    MouseRotate mouserotate = new MouseRotate();
    mouserotate.setTransformGroup(transformgroup);
    BranchGroupRoot.addChild(mouserotate);
    mouserotate.setSchedulingBounds(bounds);
    MouseZoom mousezoom = new MouseZoom();
    mousezoom.setTransformGroup(transformgroup);
    BranchGroupRoot.addChild(mousezoom);
    mousezoom.setSchedulingBounds(bounds);
    MouseTranslate mousetranslate = new MouseTranslate();
    mousetranslate.setTransformGroup(transformgroup);
    BranchGroupRoot.addChild(mousetranslate);
    mousetranslate.setSchedulingBounds(bounds);

    // 基座
    Shape3D pedestalLateral = new PedestalLateral(2.5f, 0.5f, -0.4f);
    transformgroup.addChild(pedestalLateral);
    Shape3D lowerPedestalBottom = new PedestalBottom(2.5f, -0.4f);
    transformgroup.addChild(lowerPedestalBottom);
    Shape3D upperPedestalBottom = new PedestalBottom(2.5f, 0.1f);
    transformgroup.addChild(upperPedestalBottom);

    // 塔身
    // 椭圆的极坐标方程：$r(\theta)=\frac{b}{\sqrt{1-(e\cos{\theta})^2}}$
    int division = 24; // 椭圆截面按角度划分的份数
    float innerTheta; // 绘制椭圆边界使用的角度
    float deltaInnerTheta = (float) Math.PI * 2 / 24; // 水平截面按角度划分，每份的角度
    float outerTheta = (-45.f / 180.f) * (float) Math.PI; // 椭圆整体旋转的角度，即塔身扭转的角度
    float ecc = (float) Math.sqrt(7) / 4; // 椭圆的离心率，越低越趋于圆
    float aLower = 1.6f, bLower = 1.6f * 3 / 4; // 底层椭圆的半长轴，半短轴
    float aMiddle = 0.8f, bMiddle = 0.8f * 3 / 4; // 中间椭圆的半长轴，半短轴, lev=21
    int middleLev = 21; // 最小的椭圆对应层级
    float aUpper = 1.2f, bUpper = 1.2f * 3 / 4; // 上层椭圆的半长轴，半短轴
    float surfaceLoc[][][] = new float[31][25][4]; // 共31层坐标
    float deltaOuterTheta = (float) Math.PI / 2 / 20; // 每一层整体旋转的角度
    float firstLevHeight = 0.6f; // 第一层的高度
    float deltaHeight = 0.01f; // 每一层变化的高度
    float downwardTheta = (float) Math.PI * (-15) / 180; // 椭圆绕短轴顺时针旋转的角度
    float[][][][][] bezierControl = new float[10][8][4][4][4]; // 塔身表面的控制顶点
    int bezierDivs = BezierThreeOrderSurfaceface.getN();
    Shape3D[][] bezierSurface = new Shape3D[10][8]; // 塔身的Bezier曲面
    Shape3D[][] bezierControlGrids = new Shape3D[10][8]; // 塔身的Bezier曲面控制网格
    float tempLastLevSurfaceYLoc = 0.f; // 存储上一层（低层）椭圆的圆心y坐标
    float upperPointsLoc[][] = new float[bezierDivs * 8 + 1][4]; // 塔面顶层24个点的坐标
    // 第0 ~ middleLev-1层顶点坐标
    for (int i = 0; i <= middleLev - 1; i++) {
      innerTheta = 0.f;
      // 当前层椭圆的半长轴，半短轴
      float bTemp = bLower - i * (bLower - bMiddle) / (middleLev - 1);
      for (int j = 0; j <= division; j++) {
        float r = bTemp / (float) Math.sqrt(1 - (ecc * (float) Math.pow((float) Math.cos(innerTheta), 2)));
        surfaceLoc[i][j][0] = r * (float) Math.cos(downwardTheta) * (float) Math.cos(outerTheta + innerTheta);
        if (i != 0) {
          // 当前点的纵坐标 = 椭圆圆心上升高度 + 上一椭圆圆心高度 + 边界点倾斜高度变化
          // 边界点倾斜高度变化 = 极径 * sin(downwardTheta) * cos(innerTheta)
          // 边界点倾斜高度变化的计算过程：先将点投影到平行于y轴的面，再将点映射到y轴
          surfaceLoc[i][j][1] = firstLevHeight - deltaHeight * (i - 1) + tempLastLevSurfaceYLoc
              + r * (float) Math.sin(downwardTheta) * (float) Math.cos(innerTheta);
          if (surfaceLoc[i][j][1] < 0.f)
            surfaceLoc[i][j][1] = 0.f; // 当该点坐标小于0时，令其截断
        } else {
          surfaceLoc[i][j][1] = 0.f;
        }
        surfaceLoc[i][j][2] = r * (float) Math.cos(downwardTheta) * (float) Math.sin(outerTheta + innerTheta);
        surfaceLoc[i][j][3] = 1.f;
        innerTheta += deltaInnerTheta;
      }
      innerTheta = 0.f; // 复位theta
      outerTheta += deltaOuterTheta; // 旋转椭圆整体
      // 更新上一层的椭圆圆心y坐标
      if (i > 0)
        tempLastLevSurfaceYLoc = firstLevHeight - deltaHeight * (i - 1) + tempLastLevSurfaceYLoc;
    }

    // 第middleLev ~ 30层顶点坐标
    for (int i = middleLev; i <= 30; i++) {
      innerTheta = 0.f;
      // 当前层椭圆的半长轴，半短轴
      float bTemp = bMiddle + (i - middleLev + 1) * (bUpper - bMiddle) / (31 - middleLev);
      for (int j = 0; j <= division; j++) {
        float r = bTemp / (float) Math.sqrt(1 - (ecc * (float) Math.pow((float) Math.cos(innerTheta), 2)));
        surfaceLoc[i][j][0] = r * (float) Math.cos(downwardTheta) * (float) Math.cos(outerTheta + innerTheta);
        surfaceLoc[i][j][1] = firstLevHeight + deltaHeight * ((-1 * (middleLev - 2)) + i - middleLev + 1)
            + tempLastLevSurfaceYLoc + r * (float) Math.sin(downwardTheta) * (float) Math.cos(innerTheta);
        if (surfaceLoc[i][j][1] < 0)
          surfaceLoc[i][j][1] = 0; // 当该点坐标小于0时，令其截断
        surfaceLoc[i][j][2] = r * (float) Math.cos(downwardTheta) * (float) Math.sin(outerTheta + innerTheta);
        surfaceLoc[i][j][3] = 1.f;
        innerTheta += deltaInnerTheta;
      }
      innerTheta = 0.f; // 复位theta
      outerTheta += deltaOuterTheta; // 旋转椭圆整体
      // 更新上一层的椭圆圆心y坐标
      tempLastLevSurfaceYLoc = firstLevHeight + deltaHeight * ((-1 * (middleLev - 2)) + i - middleLev + 1)
          + tempLastLevSurfaceYLoc;
    }

    // Bezier曲面，一共10层，每层8个Bezier曲面
    for (int lev = 0; lev <= 9; lev++) {
      for (int i = 0; i <= 7; i++) { // 复制点至8个Bezier曲面
        for (int j = 0; j <= 3; j++) { // 行
          for (int k = 0; k <= 3; k++) { // 列
            System.arraycopy(surfaceLoc[3 * lev + j][3 * i + k], 0, bezierControl[lev][i][j][k], 0, 4);
          }
        }
      }
    }

    // 定义第一层Bezier曲面外观属性
    Appearance app1 = new Appearance();
    PolygonAttributes polygona1 = new PolygonAttributes();
    polygona1.setBackFaceNormalFlip(true);
    polygona1.setCullFace(PolygonAttributes.CULL_NONE);
    polygona1.setPolygonMode(PolygonAttributes.POLYGON_FILL);
    app1.setPolygonAttributes(polygona1);
    Material material1 = new Material();
    Color3f color1 = new Color3f(0.5f, 0.9f, 1.f);
    material1.setDiffuseColor(color1);
    material1.setSpecularColor(color1);
    app1.setMaterial(material1);
    // TransparencyAttributes transparency1 = new
    // TransparencyAttributes(TransparencyAttributes.NICEST, 0.f);
    // app1.setTransparencyAttributes(transparency1);
    // 加入第一层的曲面
    for (int i = 0; i <= 7; i++) {
      // bezierControlGrids[0][i] = new
      // BezierSurfaceControlPoints(bezierControl[0][i], app1);
      // transformgroup.addChild(bezierControlGrids[0][i]);
      bezierSurface[0][i] = new BezierThreeOrderSurfaceface(bezierControl[0][i], app1, false, upperPointsLoc, 0);
      transformgroup.addChild(bezierSurface[0][i]);
    }

    // 定义第二层Bezier曲面外观属性
    Appearance app2 = new Appearance();
    PolygonAttributes polygona2 = new PolygonAttributes();
    polygona2.setBackFaceNormalFlip(true);
    polygona2.setCullFace(PolygonAttributes.CULL_NONE);
    polygona2.setPolygonMode(PolygonAttributes.POLYGON_FILL);
    app2.setPolygonAttributes(polygona2);
    Material material2 = new Material();
    Color3f color2 = new Color3f(0.5f, 1.f, 0.9f);
    material2.setDiffuseColor(color2);
    material2.setSpecularColor(color2);
    app2.setMaterial(material2);
    // TransparencyAttributes transparency2 = new
    // TransparencyAttributes(TransparencyAttributes.NICEST, 0.f);
    // app2.setTransparencyAttributes(transparency2);
    // 加入第二层的曲面
    for (int i = 0; i <= 7; i++) {
      // bezierControlGrids[0][i] = new
      // BezierSurfaceControlPoints(bezierControl[0][i], app1);
      // transformgroup.addChild(bezierControlGrids[0][i]);
      bezierSurface[1][i] = new BezierThreeOrderSurfaceface(bezierControl[1][i], app2, false, upperPointsLoc, 0);
      transformgroup.addChild(bezierSurface[1][i]);
    }

    // 定义第三层Bezier曲面外观属性
    Appearance app3 = new Appearance();
    PolygonAttributes polygona3 = new PolygonAttributes();
    polygona3.setBackFaceNormalFlip(true);
    polygona3.setCullFace(PolygonAttributes.CULL_NONE);
    polygona3.setPolygonMode(PolygonAttributes.POLYGON_FILL);
    app3.setPolygonAttributes(polygona3);
    Material material3 = new Material();
    Color3f color3 = new Color3f(0.8f, 1.f, 0.5f);
    material3.setDiffuseColor(color3);
    material3.setSpecularColor(color3);
    app3.setMaterial(material3);
    // TransparencyAttributes transparency3 = new
    // TransparencyAttributes(TransparencyAttributes.NICEST, 0.1f);
    // app3.setTransparencyAttributes(transparency3);
    // 加入第三层的曲面
    for (int i = 0; i <= 7; i++) {
      // bezierControlGrids[0][i] = new
      // BezierSurfaceControlPoints(bezierControl[0][i], app1);
      // transformgroup.addChild(bezierControlGrids[0][i]);
      bezierSurface[2][i] = new BezierThreeOrderSurfaceface(bezierControl[2][i], app3, false, upperPointsLoc, 0);
      transformgroup.addChild(bezierSurface[2][i]);
    }

    // 定义第四层Bezier曲面外观属性
    Appearance app4 = new Appearance();
    PolygonAttributes polygona4 = new PolygonAttributes();
    polygona4.setBackFaceNormalFlip(true);
    polygona4.setCullFace(PolygonAttributes.CULL_NONE);
    polygona4.setPolygonMode(PolygonAttributes.POLYGON_FILL);
    app4.setPolygonAttributes(polygona4);
    Material material4 = new Material();
    Color3f color4 = new Color3f(1.f, 0.9f, 0.5f);
    material4.setDiffuseColor(color4);
    material4.setSpecularColor(color4);
    app4.setMaterial(material4);
    // TransparencyAttributes transparency4 = new
    // TransparencyAttributes(TransparencyAttributes.NICEST, 0.2f);
    // app4.setTransparencyAttributes(transparency4);
    // 加入第四层的曲面
    for (int i = 0; i <= 7; i++) {
      // bezierControlGrids[0][i] = new
      // BezierSurfaceControlPoints(bezierControl[0][i], app1);
      // transformgroup.addChild(bezierControlGrids[0][i]);
      bezierSurface[3][i] = new BezierThreeOrderSurfaceface(bezierControl[3][i], app4, false, upperPointsLoc, 0);
      transformgroup.addChild(bezierSurface[3][i]);
    }

    // 定义第五层Bezier曲面外观属性
    Appearance app5 = new Appearance();
    PolygonAttributes polygona5 = new PolygonAttributes();
    polygona5.setBackFaceNormalFlip(true);
    polygona5.setCullFace(PolygonAttributes.CULL_NONE);
    polygona5.setPolygonMode(PolygonAttributes.POLYGON_FILL);
    app5.setPolygonAttributes(polygona5);
    Material material5 = new Material();
    Color3f color5 = new Color3f(1.f, 0.8f, 0.2f);
    material5.setDiffuseColor(color5);
    material5.setSpecularColor(color5);
    app5.setMaterial(material5);
    // TransparencyAttributes transparency5 = new
    // TransparencyAttributes(TransparencyAttributes.NICEST, 0.3f);
    // app5.setTransparencyAttributes(transparency5);
    // 加入第五层的曲面
    for (int i = 0; i <= 7; i++) {
      // bezierControlGrids[0][i] = new
      // BezierSurfaceControlPoints(bezierControl[0][i], app1);
      // transformgroup.addChild(bezierControlGrids[0][i]);
      bezierSurface[4][i] = new BezierThreeOrderSurfaceface(bezierControl[4][i], app5, false, upperPointsLoc, 0);
      transformgroup.addChild(bezierSurface[4][i]);
    }

    // 定义第六层Bezier曲面外观属性
    Appearance app6 = new Appearance();
    PolygonAttributes polygona6 = new PolygonAttributes();
    polygona6.setBackFaceNormalFlip(true);
    polygona6.setCullFace(PolygonAttributes.CULL_NONE);
    polygona6.setPolygonMode(PolygonAttributes.POLYGON_FILL);
    app6.setPolygonAttributes(polygona6);
    Material material6 = new Material();
    Color3f color6 = new Color3f(1.f, 0.5f, 0.8f);
    material6.setDiffuseColor(color6);
    material6.setSpecularColor(color6);
    app6.setMaterial(material6);
    // TransparencyAttributes transparency6 = new
    // TransparencyAttributes(TransparencyAttributes.NICEST, 0.4f);
    // app6.setTransparencyAttributes(transparency6);
    // 加入第六层的曲面
    for (int i = 0; i <= 7; i++) {
      // bezierControlGrids[0][i] = new
      // BezierSurfaceControlPoints(bezierControl[0][i], app1);
      // transformgroup.addChild(bezierControlGrids[0][i]);
      bezierSurface[5][i] = new BezierThreeOrderSurfaceface(bezierControl[5][i], app6, false, upperPointsLoc, 0);
      transformgroup.addChild(bezierSurface[5][i]);
    }

    // 定义第七层Bezier曲面外观属性
    Appearance app7 = new Appearance();
    PolygonAttributes polygona7 = new PolygonAttributes();
    polygona7.setBackFaceNormalFlip(true);
    polygona7.setCullFace(PolygonAttributes.CULL_NONE);
    polygona7.setPolygonMode(PolygonAttributes.POLYGON_FILL);
    app7.setPolygonAttributes(polygona7);
    Material material7 = new Material();
    Color3f color7 = new Color3f(0.8f, 0.5f, 1.f);
    material7.setDiffuseColor(color7);
    material7.setSpecularColor(color7);
    app7.setMaterial(material7);
    // TransparencyAttributes transparency7 = new
    // TransparencyAttributes(TransparencyAttributes.NICEST, 0.3f);
    // app7.setTransparencyAttributes(transparency7);
    // 加入第七层的曲面
    for (int i = 0; i <= 7; i++) {
      // bezierControlGrids[0][i] = new
      // BezierSurfaceControlPoints(bezierControl[0][i], app1);
      // transformgroup.addChild(bezierControlGrids[0][i]);
      bezierSurface[6][i] = new BezierThreeOrderSurfaceface(bezierControl[6][i], app7, false, upperPointsLoc, 0);
      transformgroup.addChild(bezierSurface[6][i]);
    }

    // 定义第八层Bezier曲面外观属性
    Appearance app8 = new Appearance();
    PolygonAttributes polygona8 = new PolygonAttributes();
    polygona8.setBackFaceNormalFlip(true);
    polygona8.setCullFace(PolygonAttributes.CULL_NONE);
    polygona8.setPolygonMode(PolygonAttributes.POLYGON_FILL);
    app8.setPolygonAttributes(polygona8);
    Material material8 = new Material();
    Color3f color8 = new Color3f(0.6f, 0.5f, 1.f);
    material8.setDiffuseColor(color8);
    material8.setSpecularColor(color8);
    app8.setMaterial(material8);
    // TransparencyAttributes transparency8 = new
    // TransparencyAttributes(TransparencyAttributes.NICEST, 0.2f);
    // app8.setTransparencyAttributes(transparency8);
    // 加入第八层的曲面
    for (int i = 0; i <= 7; i++) {
      // bezierControlGrids[0][i] = new
      // BezierSurfaceControlPoints(bezierControl[0][i], app1);
      // transformgroup.addChild(bezierControlGrids[0][i]);
      bezierSurface[7][i] = new BezierThreeOrderSurfaceface(bezierControl[7][i], app8, false, upperPointsLoc, 0);
      transformgroup.addChild(bezierSurface[7][i]);
    }

    // 定义第九层Bezier曲面外观属性
    Appearance app9 = new Appearance();
    PolygonAttributes polygona9 = new PolygonAttributes();
    polygona9.setBackFaceNormalFlip(true);
    polygona9.setCullFace(PolygonAttributes.CULL_NONE);
    polygona9.setPolygonMode(PolygonAttributes.POLYGON_FILL);
    app9.setPolygonAttributes(polygona9);
    Material material9 = new Material();
    Color3f color9 = new Color3f(0.4f, 0.6f, 1.f);
    material9.setDiffuseColor(color9);
    material9.setSpecularColor(color9);
    app9.setMaterial(material9);
    // TransparencyAttributes transparency9 = new
    // TransparencyAttributes(TransparencyAttributes.NICEST, 0.1f);
    // app9.setTransparencyAttributes(transparency9);
    // 加入第九层的曲面
    for (int i = 0; i <= 7; i++) {
      // bezierControlGrids[0][i] = new
      // BezierSurfaceControlPoints(bezierControl[0][i], app1);
      // transformgroup.addChild(bezierControlGrids[0][i]);
      bezierSurface[8][i] = new BezierThreeOrderSurfaceface(bezierControl[8][i], app9, false, upperPointsLoc, 0);
      transformgroup.addChild(bezierSurface[8][i]);
    }

    // 定义第十层Bezier曲面外观属性
    Appearance app10 = new Appearance();
    PolygonAttributes polygona10 = new PolygonAttributes();
    polygona10.setBackFaceNormalFlip(true);
    polygona10.setCullFace(PolygonAttributes.CULL_NONE);
    polygona10.setPolygonMode(PolygonAttributes.POLYGON_FILL);
    app10.setPolygonAttributes(polygona10);
    Material material10 = new Material();
    Color3f color10 = new Color3f(0.2f, 0.8f, 1.f);
    material10.setDiffuseColor(color10);
    material10.setSpecularColor(color10);
    app10.setMaterial(material10);
    // TransparencyAttributes transparency10 = new
    // TransparencyAttributes(TransparencyAttributes.NICEST, 0.f);
    // app10.setTransparencyAttributes(transparency10);
    // 加入第十层的曲面
    for (int i = 0; i <= 7; i++) {
      // bezierControlGrids[0][i] = new
      // BezierSurfaceControlPoints(bezierControl[0][i], app1);
      // transformgroup.addChild(bezierControlGrids[0][i]);
      bezierSurface[9][i] = new BezierThreeOrderSurfaceface(bezierControl[9][i], app10, true, upperPointsLoc, i);
      transformgroup.addChild(bezierSurface[9][i]);
    }

    // 生成塔顶的面
    Shape3D towerTopSurface = new TopFace(upperPointsLoc, bezierDivs);
    transformgroup.addChild(towerTopSurface);

    // 生成塔顶边缘的椭圆环柱
    float ringWidth = 0.2f;
    float cylinderHeight = 0.2f;
    TopEllipticRingCylinder topEllipticRingCylinder = new TopEllipticRingCylinder(upperPointsLoc, ringWidth,
        cylinderHeight);
    transformgroup.addChild(topEllipticRingCylinder);

    // 生成椭圆环柱上的球体，使用子transformgroup
    Transform3D[] t = new Transform3D[bezierDivs * 8];
    TransformGroup[] tg = new TransformGroup[bezierDivs * 8];
    float[][] centerLoc = new float[bezierDivs * 8][4];
    float[][][] calPoints = topEllipticRingCylinder.getEllipticRingCylinderGridPoints();
    PolygonAttributes polygonattributes = new PolygonAttributes();
    polygonattributes.setCullFace(PolygonAttributes.CULL_NONE);
    polygonattributes.setBackFaceNormalFlip(true);
    polygonattributes.setPolygonMode(PolygonAttributes.POLYGON_FILL);
    Appearance app11 = new Appearance();
    // app11.setPolygonAttributes(polygonattributes);
    // Material material11 = new Material();
    // Color3f color11 = new Color3f(0.75f, 0.75f, 0.75f);
    // material11.setDiffuseColor(color10);
    // material11.setEmissiveColor(color11);
    // app11.setMaterial(material11);
    String ballTextureName = new String("texture_1.JPG");
    app11 = createTextureAppearance(ballTextureName);
    for (int i = 0; i <= bezierDivs * 8 - 1; i++) {
      centerLoc[i][0] = (calPoints[2][i][0] + calPoints[3][i][0]) / 2;
      centerLoc[i][1] = (calPoints[2][i][1] + calPoints[3][i][1]) / 2 + 0.05f;
      centerLoc[i][2] = (calPoints[2][i][2] + calPoints[3][i][2]) / 2;
      centerLoc[i][3] = (calPoints[2][i][3] + calPoints[3][i][3]) / 2;
      // cylinderOnEllipse[i] = new Cylinder(centerLoc[i], 0.05f, 0.1f, app11);
      // transformgroup.addChild(cylinderOnEllipse[i]);
      t[i] = new Transform3D();
      t[i].setTranslation(new Vector3f(centerLoc[i][0], centerLoc[i][1],
          centerLoc[i][2]));
      tg[i] = new TransformGroup(t[i]);
      tg[i].addChild(new Sphere(0.1f, app11));
      if (i % 20 == 0)
        transformgroup.addChild(tg[i]);
    }

    // 生成顶层的圆柱，半径为0.2 * cos(downwardTheta)
    float ellipseBelowCylinderA = 0.2f;
    float ellipseBelowCylinderB = 0.2f * (float) Math.cos(downwardTheta);
    float cylinderR = 0.2f * (float) Math.cos(downwardTheta);
    float eccEllipseBelowCylinder = (float) Math.sqrt(1 - (float) Math
        .pow((ellipseBelowCylinderB * ellipseBelowCylinderB / ellipseBelowCylinderA /
            ellipseBelowCylinderA), 2));
    float[][][] ellipseAndCirclePointsLoc = new float[2][bezierDivs * 8 + 1][4];
    float ellipseOuterTheta = (float) Math.PI / 2;
    float ellipseInnerTheta = 0.f;
    int cylinderDiv = bezierDivs * 8;
    float deltaEllipseInnerTheta = (float) Math.PI * 2 / cylinderDiv;
    for (int i = 0; i <= cylinderDiv; i++) {
      float r = ellipseBelowCylinderB
          / (float) Math.sqrt(1 - (eccEllipseBelowCylinder * (float) Math.pow((float) Math.cos(ellipseInnerTheta), 2)));
      ellipseAndCirclePointsLoc[0][i][0] = r * (float) Math.cos(downwardTheta)
          * (float) Math.cos(ellipseOuterTheta + ellipseInnerTheta);
      ellipseAndCirclePointsLoc[0][i][1] = r * (float) Math.sin(downwardTheta) *
          (float) Math.cos(ellipseInnerTheta)
          + topEllipticRingCylinder.getTopCenter()[1];
      ellipseAndCirclePointsLoc[0][i][2] = r * (float) Math.cos(downwardTheta)
          * (float) Math.sin(ellipseOuterTheta + ellipseInnerTheta);
      ellipseAndCirclePointsLoc[0][i][3] = 1.f;
      ellipseAndCirclePointsLoc[1][i][0] = cylinderR * (float) Math.cos(ellipseOuterTheta + ellipseInnerTheta);
      ellipseAndCirclePointsLoc[1][i][1] = (-1) * ellipseBelowCylinderA * (float) Math.sin(downwardTheta)
          + topEllipticRingCylinder.getTopCenter()[1];
      ellipseAndCirclePointsLoc[1][i][2] = cylinderR * (float) Math.sin(ellipseOuterTheta + ellipseInnerTheta);
      ellipseAndCirclePointsLoc[1][i][3] = 1.f;
      ellipseInnerTheta += deltaEllipseInnerTheta;
    }
    Appearance app12 = new Appearance();
    // PolygonAttributes polygona12 = new PolygonAttributes();
    // polygona12.setBackFaceNormalFlip(true);
    // polygona12.setCullFace(PolygonAttributes.CULL_NONE);
    // polygona12.setPolygonMode(PolygonAttributes.POLYGON_FILL);
    // app12.setPolygonAttributes(polygona12);
    // Material material12 = new Material();
    // Color3f color12 = new Color3f(0.8f, 0.8f, 0.8f);
    // material12.setDiffuseColor(color10);
    // material12.setEmissiveColor(color12);
    // app12.setMaterial(material12);
    String cyTextureName = new String("texture_2.JPG");
    app12 = createTextureAppearance(cyTextureName);
    Shape3D ellipseAndCircleCylinder = new EllipseAndCircleCylinder(ellipseAndCirclePointsLoc, cylinderDiv, app12);
    transformgroup.addChild(ellipseAndCircleCylinder);
    TopCylinder topCylinder1 = new TopCylinder(ellipseAndCirclePointsLoc[1], 1.f, 0.4f, app12, cylinderDiv);
    transformgroup.addChild(topCylinder1);
    TopCylinder topCylinder2 = new TopCylinder(ellipseAndCirclePointsLoc[1], 0.9f, 1.f, app12, cylinderDiv);
    transformgroup.addChild(topCylinder2);
    TopCylinder topCylinder3 = new TopCylinder(ellipseAndCirclePointsLoc[1], 0.8f, 1.2f, app12, cylinderDiv);
    transformgroup.addChild(topCylinder3);
    TopCylinder topCylinder4 = new TopCylinder(ellipseAndCirclePointsLoc[1], 0.6f, 1.8f, app12, cylinderDiv);
    transformgroup.addChild(topCylinder4);
    TopCylinder topCylinder5 = new TopCylinder(ellipseAndCirclePointsLoc[1], 0.5f, 2.0f, app12, cylinderDiv);
    transformgroup.addChild(topCylinder5);

    // 圆柱上的装饰
    float radius1 = cylinderR * 3 / 2 * 1.f;
    float height1 = 0.05f;
    Transform3D transformCld1 = new Transform3D();
    transformCld1.setTranslation(new Vector3f(0.f,
        ellipseAndCirclePointsLoc[1][0][1] + 0.4f, 0.f));
    TransformGroup tgCld1 = new TransformGroup(transformCld1);
    tgCld1.addChild(new Cylinder(radius1, height1, app12));
    transformgroup.addChild(tgCld1);

    float radius2 = cylinderR * 3 / 2 * 0.9f;
    float height2 = 0.1f;
    Transform3D transformCld2 = new Transform3D();
    transformCld2.setTranslation(new Vector3f(0.f,
        ellipseAndCirclePointsLoc[1][0][1] + 1.f, 0.f));
    TransformGroup tgCld2 = new TransformGroup(transformCld2);
    tgCld2.addChild(new Cylinder(radius2, height2, app12));
    transformgroup.addChild(tgCld2);

    float radius3 = cylinderR * 3 / 2 * 0.8f;
    float height3 = 0.05f;
    Transform3D transformCld3 = new Transform3D();
    transformCld3.setTranslation(new Vector3f(0.f,
        ellipseAndCirclePointsLoc[1][0][1] + 1.2f, 0.f));
    TransformGroup tgCld3 = new TransformGroup(transformCld3);
    tgCld3.addChild(new Cylinder(radius3, height3, app12));
    transformgroup.addChild(tgCld3);

    float radius4 = cylinderR * 3 / 2 * 0.6f;
    float height4 = 0.1f;
    Transform3D transformCld4 = new Transform3D();
    transformCld4.setTranslation(new Vector3f(0.f,
        ellipseAndCirclePointsLoc[1][0][1] + 1.8f, 0.f));
    TransformGroup tgCld4 = new TransformGroup(transformCld4);
    tgCld4.addChild(new Cylinder(radius4, height4, app12));
    transformgroup.addChild(tgCld4);

    float radius5 = cylinderR * 3 / 2 * 0.5f;
    float height5 = 0.05f;
    Transform3D transformCld5 = new Transform3D();
    transformCld5.setTranslation(new Vector3f(0.f,
        ellipseAndCirclePointsLoc[1][0][1] + 2.0f, 0.f));
    TransformGroup tgCld5 = new TransformGroup(transformCld5);
    tgCld5.addChild(new Cylinder(radius5, height5, app12));
    transformgroup.addChild(tgCld5);

    // 最后一个作为最细高的圆柱
    float radius6 = cylinderR * 0.3f;
    float height6 = 2.0f;
    Transform3D transformCld6 = new Transform3D();
    transformCld6.setTranslation(new Vector3f(0.f,
        ellipseAndCirclePointsLoc[1][0][1] + 2.05f, 0.f));
    TransformGroup tgCld6 = new TransformGroup(transformCld6);
    tgCld6.addChild(new Cylinder(radius6, height6, app12));
    transformgroup.addChild(tgCld6);

    BranchGroupRoot.compile();
    return BranchGroupRoot;
  }

  public Design() {
    setLayout(new BorderLayout());
    GraphicsConfiguration gc = SimpleUniverse.getPreferredConfiguration();
    Canvas3D c = new Canvas3D(gc);
    add("Center", c);
    BranchGroup BranchGroupScene = createBranchGroupSceneGraph();
    SimpleUniverse u = new SimpleUniverse(c);
    u.getViewingPlatform().setNominalViewingTransform();
    u.addBranchGraph(BranchGroupScene);
  }

  public static void main(String[] args) {
    new MainFrame(new Design(), 1200, 800);
  }

  Appearance createTextureAppearance(String filename) { // 定义带纹理属性的外观
    Appearance app = new Appearance();
    TextureLoader loader = new TextureLoader(filename, this);
    ImageComponent2D image = loader.getImage();
    Texture2D texture = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA, image.getWidth(), image.getHeight());
    texture.setImage(0, image);
    texture.setEnable(true);
    texture.setMagFilter(Texture.BASE_LEVEL_POINT);
    texture.setMinFilter(Texture.BASE_LEVEL_POINT);
    app.setTexture(texture);
    PolygonAttributes polygonattributes = new PolygonAttributes();
    polygonattributes.setBackFaceNormalFlip(true);
    polygonattributes.setCullFace(PolygonAttributes.CULL_NONE);
    app.setPolygonAttributes(polygonattributes);
    app.setCapability(Appearance.ALLOW_TEXGEN_WRITE);
    // 定义自动纹理坐标生成
    TexCoordGeneration tcg = new TexCoordGeneration(TexCoordGeneration.SPHERE_MAP,
        TexCoordGeneration.TEXTURE_COORDINATE_3);
    tcg.setPlaneR(new Vector4f(2, 0, 0, 0));
    tcg.setPlaneS(new Vector4f(0, 2, 0, 0));
    tcg.setPlaneT(new Vector4f(0, 0, 2, 0));
    app.setTexCoordGeneration(tcg);
    app.setCapability(Appearance.ALLOW_TEXGEN_WRITE);
    return app;
  }

}

class matrixm {
  public float CC[][] = new float[4][4];
  int ll, mm, kk;

  public matrixm(int mmm, int kkk, int nnn, float a[][], float b[][]) {
    for (ll = 0; ll < mmm; ll++)
      for (mm = 0; mm < nnn; mm++) {
        CC[ll][mm] = 0.f;
      }
    for (ll = 0; ll < mmm; ll++)
      for (mm = 0; mm < nnn; mm++) {
        for (kk = 0; kk < kkk; kk++)
          CC[ll][mm] = CC[ll][mm] + a[ll][kk] * b[kk][mm];
      }
  }
}

// 计算Bezier曲面上的点，并生成Bezier曲面
class BezierThreeOrderSurfaceface extends Shape3D {
  private static int n0 = 30;// 定义参数u、v在[0，1]区间的等分点数

  public static int getN() {
    return n0;
  }

  public BezierThreeOrderSurfaceface(float[][][] P, Appearance app, boolean delivFlag, float[][] delivDes, int offset) {
    int i, j, k;
    float division;// 定义参数u、v在[0，1]区间的等分线段长度
    division = 1.f / n0;
    // 分别定义存放控制顶点x、y、z坐标与第四维的数组
    float[][] PX = new float[4][4];
    float[][] PY = new float[4][4];
    float[][] PZ = new float[4][4];
    float[][] P4 = new float[4][4];
    // 定义系数矩阵及其转置矩阵
    float[][] M1 = { { 1.f, 0.f, 0.f, 0.f },
        { -3.f, 3.f, 0.f, 0.f },
        { 3.f, -6.f, 3.f, 0.f },
        { -1.f, 3.f, -3.f, 1.f } };
    float[][] M2 = { { 1.f, -3.f, 3.f, -1.f },
        { 0.f, 3.f, -6.f, 3.f },
        { 0.f, 0.f, 3.f, -3.f },
        { 0.f, 0.f, 0.f, 1.f } };
    // 定义存放Bezier曲面u、v参数分割点的坐标数组
    float[][][] UV = new float[n0 + 1][n0 + 1][2];
    // 定义U、V矩阵数组
    float[][] UU = new float[1][4];
    float[][] VV = new float[4][1];
    // 定义存放曲面上点的坐标的数组
    float[][][] SurfaceXYZ = new float[n0 + 1][n0 + 1][4];
    for (i = 0; i < n0 + 1; i++)
      for (j = 0; j < n0 + 1; j++) {
        UV[i][j][0] = i * division;
        UV[i][j][1] = j * division;
      }
    for (i = 0; i < 4; i++)
      for (j = 0; j < 4; j++) {
        PX[i][j] = P[i][j][0];
        PY[i][j] = P[i][j][1];
        PZ[i][j] = P[i][j][2];
        P4[i][j] = P[i][j][3];
      }
    // 计算曲面上所有点的坐标
    for (i = 0; i < n0 + 1; i++)
      for (j = 0; j < n0 + 1; j++) {
        UU[0][0] = 1.f;
        UU[0][1] = UV[i][j][0];
        UU[0][2] = UV[i][j][0] * UV[i][j][0];
        UU[0][3] = UV[i][j][0] * UV[i][j][0] * UV[i][j][0];
        VV[0][0] = 1.f;
        VV[1][0] = UV[i][j][1];
        VV[2][0] = UV[i][j][1] * UV[i][j][1];
        VV[3][0] = UV[i][j][1] * UV[i][j][1] * UV[i][j][1];
        // 计算一点的x坐标
        matrixm g0 = new matrixm(1, 4, 4, UU, M1);
        matrixm g1 = new matrixm(1, 4, 4, g0.CC, PX);
        matrixm g2 = new matrixm(1, 4, 4, g1.CC, M2);
        matrixm g3 = new matrixm(1, 4, 1, g2.CC, VV);
        SurfaceXYZ[i][j][0] = g3.CC[0][0];
        // 计算一点的y坐标
        matrixm g4 = new matrixm(1, 4, 4, UU, M1);
        matrixm g5 = new matrixm(1, 4, 4, g4.CC, PY);
        matrixm g6 = new matrixm(1, 4, 4, g5.CC, M2);
        matrixm g7 = new matrixm(1, 4, 1, g6.CC, VV);
        SurfaceXYZ[i][j][1] = g7.CC[0][0];
        // 计算一点的z坐标
        matrixm g8 = new matrixm(1, 4, 4, UU, M1);
        matrixm g9 = new matrixm(1, 4, 4, g8.CC, PZ);
        matrixm g10 = new matrixm(1, 4, 4, g9.CC, M2);
        matrixm g11 = new matrixm(1, 4, 1, g10.CC, VV);
        SurfaceXYZ[i][j][2] = g11.CC[0][0];
        // 计算一点的第4维坐标，在该程序中，第4维坐标全为1，可不计算
        matrixm g12 = new matrixm(1, 4, 4, UU, M1);
        matrixm g13 = new matrixm(1, 4, 4, g12.CC, P4);
        matrixm g14 = new matrixm(1, 4, 4, g13.CC, M2);
        matrixm g15 = new matrixm(1, 4, 1, g14.CC, VV);
        SurfaceXYZ[i][j][3] = g15.CC[0][0];
        // 将齐次坐标转换为三维坐标系坐标，如果第四维为1，可不除该项
        // SurfaceXYZ[i][j][0] = SurfaceXYZ[i][j][0] / SurfaceXYZ[i][j][3];
        // SurfaceXYZ[i][j][1] = SurfaceXYZ[i][j][1] / SurfaceXYZ[i][j][3];
        // SurfaceXYZ[i][j][2] = SurfaceXYZ[i][j][2] / SurfaceXYZ[i][j][3];
      }
    QuadArray BeziersurfacecontrolPointsNet = new QuadArray(n0 * n0 * 4,
        GeometryArray.COORDINATES | GeometryArray.NORMALS);
    int c = 0;// 以顶点数累加的方式设置数组中顶点的序号
    for (i = 0; i < n0; i++) {
      for (j = 0; j < n0; j++) {// 设置一个平面上的4个点
        Point3f A = new Point3f(SurfaceXYZ[i][j][0], SurfaceXYZ[i][j][1],
            SurfaceXYZ[i][j][2]);
        Point3f B = new Point3f(SurfaceXYZ[i][j + 1][0], SurfaceXYZ[i][j + 1][1],
            SurfaceXYZ[i][j + 1][2]);
        Point3f C = new Point3f(SurfaceXYZ[i + 1][j + 1][0], SurfaceXYZ[i + 1][j + 1][1],
            SurfaceXYZ[i + 1][j + 1][2]);
        Point3f D = new Point3f(SurfaceXYZ[i + 1][j][0], SurfaceXYZ[i + 1][j][1],
            SurfaceXYZ[i + 1][j][2]);
        // 计算四个点的法向量
        Vector3f a = new Vector3f(A.x - B.x, A.y - B.y, A.z - B.z);
        Vector3f b = new Vector3f(C.x - B.x, C.y - B.y, C.z - B.z);
        Vector3f n = new Vector3f();
        n.cross(b, a);
        n.normalize();
        // 设置点的序号
        BeziersurfacecontrolPointsNet.setCoordinate(c, A);
        BeziersurfacecontrolPointsNet.setCoordinate(c + 1, B);
        BeziersurfacecontrolPointsNet.setCoordinate(c + 2, C);
        BeziersurfacecontrolPointsNet.setCoordinate(c + 3, D);
        // 按序号设置点的法向量
        BeziersurfacecontrolPointsNet.setNormal(c, n);
        BeziersurfacecontrolPointsNet.setNormal(c + 1, n);
        BeziersurfacecontrolPointsNet.setNormal(c + 2, n);
        BeziersurfacecontrolPointsNet.setNormal(c + 3, n);
        c = c + 4;
      }
    }
    this.addGeometry(BeziersurfacecontrolPointsNet);
    this.setAppearance(app);
    if (delivFlag == true) {
      for (k = 0; k <= n0; k++) {
        System.arraycopy(SurfaceXYZ[n0][k], 0, delivDes[offset * n0 + k], 0, 4);
      }
    }
  }
}

// 生成控制顶点网格
class BezierSurfaceControlPoints extends Shape3D {
  public BezierSurfaceControlPoints(float[][][] P, Appearance app) {
    int i, j, k;
    QuadArray BeziersurfacecontrolPointsNet = new QuadArray(3 * 3 * 4,
        GeometryArray.COORDINATES | GeometryArray.NORMALS);
    int c = 0;
    for (i = 0; i < 3; i++) {
      for (j = 0; j < 3; j++) {
        Point3f A = new Point3f(P[i][j][0], P[i][j][1], P[i][j][2]);
        Point3f B = new Point3f(P[i][j + 1][0], P[i][j + 1][1], P[i][j + 1][2]);
        Point3f C = new Point3f(P[i + 1][j + 1][0], P[i + 1][j + 1][1], P[i + 1][j + 1][2]);
        Point3f D = new Point3f(P[i + 1][j][0], P[i + 1][j][1], P[i + 1][j][2]);
        Vector3f a = new Vector3f(A.x - B.x, A.y - B.y, A.z - B.z);
        Vector3f b = new Vector3f(C.x - B.x, C.y - B.y, C.z - B.z);
        Vector3f n = new Vector3f();
        n.cross(b, a);
        n.normalize();
        BeziersurfacecontrolPointsNet.setCoordinate(c, A);
        BeziersurfacecontrolPointsNet.setCoordinate(c + 1, B);
        BeziersurfacecontrolPointsNet.setCoordinate(c + 2, C);
        BeziersurfacecontrolPointsNet.setCoordinate(c + 3, D);
        BeziersurfacecontrolPointsNet.setNormal(c, n);
        BeziersurfacecontrolPointsNet.setNormal(c + 1, n);
        BeziersurfacecontrolPointsNet.setNormal(c + 2, n);
        BeziersurfacecontrolPointsNet.setNormal(c + 3, n);
        c = c + 4;
      }
    }
    this.addGeometry(BeziersurfacecontrolPointsNet);
    this.setAppearance(app);
  }
}

// 底座的侧面
class PedestalLateral extends Shape3D {
  public PedestalLateral(float radius, float height, float base) {
    int numSides = 8;
    int numVerts = numSides * 2;
    Point3f[] verts = new Point3f[numVerts];
    float angleDelta = (float) (2 * Math.PI / 8);
    float angle = 0.0f;
    for (int i = 0; i < numSides; i++) {
      verts[i * 2] = new Point3f(radius * (float) Math.sin(angle), base, radius * (float) Math.cos(angle));
      verts[i * 2 + 1] = new Point3f(radius * (float) Math.sin(angle), base + height, radius * (float) Math.cos(angle));
      angle += angleDelta;
    }
    QuadArray quads = new QuadArray(numSides * 4, QuadArray.COORDINATES | IndexedQuadArray.NORMALS);
    int c = 0;
    for (int i = 0; i <= numSides - 2; i++) {
      quads.setCoordinate(c, new Point3f(verts[2 * i]));
      quads.setCoordinate(c + 1, new Point3f(verts[2 * i + 1]));
      quads.setCoordinate(c + 2, new Point3f(verts[2 * i + 3]));
      quads.setCoordinate(c + 3, new Point3f(verts[2 * i + 2]));
      Vector3f a = new Vector3f(verts[2 * i + 1].x - verts[2 * i].x,
          verts[2 * i + 1].y - verts[2 * i].y, verts[2 * i + 1].z - verts[2 * i].z);
      Vector3f b = new Vector3f(verts[2 * i + 3].x - verts[2 * i + 1].x,
          verts[2 * i + 3].y - verts[2 * i + 1].y, verts[2 * i + 3].z - verts[2 * i + 1].z);
      Vector3f n = new Vector3f();
      n.cross(a, b);
      n.normalize();
      quads.setNormal(c, n);
      quads.setNormal(c + 1, n);
      quads.setNormal(c + 2, n);
      quads.setNormal(c + 3, n);
      c += 4;
    }
    // 单独处理最后一个quad
    quads.setCoordinate(c, new Point3f(verts[2 * (numSides - 1)]));
    quads.setCoordinate(c + 1, new Point3f(verts[2 * (numSides - 1) + 1]));
    quads.setCoordinate(c + 2, new Point3f(verts[1]));
    quads.setCoordinate(c + 3, new Point3f(verts[0]));
    Vector3f a = new Vector3f(verts[2 * (numSides - 1) + 1].x - verts[2 * (numSides - 1)].x,
        verts[2 * (numSides - 1) + 1].y - verts[2 * (numSides - 1)].y,
        verts[2 * (numSides - 1) + 1].z - verts[2 * (numSides - 1)].z);
    Vector3f b = new Vector3f(verts[1].x - verts[2 * (numSides - 1) + 1].x,
        verts[1].y - verts[2 * (numSides - 1) + 1].y, verts[1].z - verts[2 * (numSides - 1) + 1].z);
    Vector3f n = new Vector3f();
    n.cross(a, b);
    n.normalize();
    quads.setNormal(c, n);
    quads.setNormal(c + 1, n);
    quads.setNormal(c + 2, n);
    quads.setNormal(c + 3, n);
    PolygonAttributes polygonattributes = new PolygonAttributes();
    polygonattributes.setCullFace(PolygonAttributes.CULL_NONE);
    polygonattributes.setBackFaceNormalFlip(true);
    polygonattributes.setPolygonMode(PolygonAttributes.POLYGON_FILL);
    Appearance app = new Appearance();
    app.setPolygonAttributes(polygonattributes);
    this.setGeometry(quads);
    Material material = new Material();
    Color3f color = new Color3f(0.6f, 0.6f, 0.6f);
    material.setDiffuseColor(color);
    app.setMaterial(material);
    this.setAppearance(app);
  }
}

// 底座的底面
class PedestalBottom extends Shape3D {
  public PedestalBottom(float radius, float base) {
    int numSides = 8;
    int numVerts = numSides;
    Point3f[] verts = new Point3f[numVerts];
    float angleDelta = (float) (2 * Math.PI / 8);
    float angle = 0.0f;
    for (int i = 0; i < numSides; i++) {
      verts[i] = new Point3f(radius * (float) Math.sin(angle), base, radius * (float) Math.cos(angle));
      angle += angleDelta;
    }
    TriangleArray triangleSurface = new TriangleArray(18, TriangleArray.COORDINATES | TriangleArray.NORMALS);
    int c = 0;// 以顶点数累加的方式设置点的序号
    Point3f A1 = new Point3f(verts[0]);
    for (int i = 0; i <= 5; i++) {
      Point3f A2 = new Point3f(verts[i + 1]);
      Point3f A3 = new Point3f(verts[i + 2]);
      Vector3f a = new Vector3f(A2.x - A1.x, A2.y - A1.y, A2.z - A1.z);
      Vector3f b = new Vector3f(A3.x - A1.x, A3.y - A1.y, A3.z - A1.z);
      Vector3f n = new Vector3f();
      n.cross(a, b);
      n.normalize();
      // 设置点序号及坐标
      triangleSurface.setCoordinate(c, A1);
      triangleSurface.setCoordinate(c + 1, A2);
      triangleSurface.setCoordinate(c + 2, A3);
      // 设置点法向量
      triangleSurface.setNormal(c, n);
      triangleSurface.setNormal(c + 1, n);
      triangleSurface.setNormal(c + 2, n);
      c = c + 3;
    }
    this.setGeometry(triangleSurface);
    // 设置外观
    PolygonAttributes polygonattributes = new PolygonAttributes();
    polygonattributes.setCullFace(PolygonAttributes.CULL_NONE);
    polygonattributes.setBackFaceNormalFlip(true);
    polygonattributes.setPolygonMode(PolygonAttributes.POLYGON_FILL);
    Appearance app = new Appearance();
    app.setPolygonAttributes(polygonattributes);
    Material material = new Material();
    Color3f color = new Color3f(0.6f, 0.6f, 0.6f);
    material.setDiffuseColor(color);
    material.setEmissiveColor(color);
    app.setMaterial(material);
    this.setAppearance(app);
  }
}

// 塔身顶层的面
class TopFace extends Shape3D {
  public TopFace(float surfacePoints[][], int bezierDivs) {
    int numSides = bezierDivs * 8;
    int numVerts = numSides;
    TriangleArray triangleSurface = new TriangleArray((numVerts - 2) * 3,
        TriangleArray.COORDINATES | TriangleArray.NORMALS);
    int c = 0;// 以顶点数累加的方式设置点的序号
    Point3f A1 = new Point3f(surfacePoints[0]);
    for (int i = 0; i <= (numVerts - 2) - 1; i++) {
      Point3f A2 = new Point3f(surfacePoints[i + 1]);
      Point3f A3 = new Point3f(surfacePoints[i + 2]);
      Vector3f a = new Vector3f(A2.x - A1.x, A2.y - A1.y, A2.z - A1.z);
      Vector3f b = new Vector3f(A3.x - A1.x, A3.y - A1.y, A3.z - A1.z);
      Vector3f n = new Vector3f();
      n.cross(a, b);
      n.normalize();
      // 设置点序号及坐标
      triangleSurface.setCoordinate(c, A1);
      triangleSurface.setCoordinate(c + 1, A2);
      triangleSurface.setCoordinate(c + 2, A3);
      // 设置点法向量
      triangleSurface.setNormal(c, n);
      triangleSurface.setNormal(c + 1, n);
      triangleSurface.setNormal(c + 2, n);
      c = c + 3;
    }
    this.setGeometry(triangleSurface);
    // 设置外观
    PolygonAttributes polygonattributes = new PolygonAttributes();
    polygonattributes.setCullFace(PolygonAttributes.CULL_NONE);
    polygonattributes.setBackFaceNormalFlip(true);
    polygonattributes.setPolygonMode(PolygonAttributes.POLYGON_FILL);
    Appearance app = new Appearance();
    app.setPolygonAttributes(polygonattributes);
    Material material = new Material();
    Color3f color = new Color3f(0.2f, 0.8f, 1.f);
    material.setDiffuseColor(color);
    // material.setEmissiveColor(color);
    app.setMaterial(material);
    this.setAppearance(app);
  }
}

// 塔顶边缘的椭圆环柱与椭圆环柱上的柱体
class TopEllipticRingCylinder extends Shape3D {
  int bezierDivs = BezierThreeOrderSurfaceface.getN();
  private float[][][] ellipticRingCylinderGridPoints = new float[4][bezierDivs * 8 + 1][4]; // 柱面上点的坐标
  private float[] topCenter = new float[4];

  // 返回椭圆环柱网格顶点的坐标
  public float[][][] getEllipticRingCylinderGridPoints() {
    return ellipticRingCylinderGridPoints;
  }

  public float[] getTopCenter() {
    return topCenter;
  }

  public TopEllipticRingCylinder(float surfacePoints[][], float ringWidth, float cylinderHeight) {
    int numOriPoints = bezierDivs * 8 + 1;
    // 计算顶层椭圆的圆心坐标
    float[] startPoint = new float[4];
    System.arraycopy(surfacePoints[0], 0, startPoint, 0, 4);
    float[] endPoint = {
        (surfacePoints[(numOriPoints - 1) / 2][0] + surfacePoints[(numOriPoints - 1) / 2 + 1][0]) / 2,
        (surfacePoints[(numOriPoints - 1) / 2][1] + surfacePoints[(numOriPoints - 1) / 2 + 1][1]) / 2,
        (surfacePoints[(numOriPoints - 1) / 2][2] + surfacePoints[(numOriPoints - 1) / 2 + 1][2]) / 2,
        (surfacePoints[(numOriPoints - 1) / 2][3] + surfacePoints[(numOriPoints - 1) / 2 + 1][3]) / 2
    };
    topCenter = new float[] {
        (startPoint[0] + endPoint[0]) / 2, (startPoint[1] + endPoint[1]) / 2, (startPoint[2] + endPoint[2]) / 2,
        (startPoint[3] + endPoint[3]) / 2
    };
    float cosAlpha, cosBeta, cosGamma; // 方向余弦
    for (int i = 0; i <= bezierDivs * 8; i++) {
      cosAlpha = (topCenter[0] - surfacePoints[i][0]) / ((float) Math.sqrt(
          (float) Math.pow((surfacePoints[i][0] - topCenter[0]), 2)
              + (float) Math.pow((surfacePoints[i][1] - topCenter[1]), 2)
              + (float) Math.pow((surfacePoints[i][2] - topCenter[2]), 2)));
      cosBeta = (topCenter[1] - surfacePoints[i][1]) / ((float) Math.sqrt(
          (float) Math.pow((surfacePoints[i][0] - topCenter[0]), 2)
              + (float) Math.pow((surfacePoints[i][1] - topCenter[1]), 2)
              + (float) Math.pow((surfacePoints[i][2] - topCenter[2]), 2)));
      cosGamma = (topCenter[2] - surfacePoints[i][2]) / ((float) Math.sqrt(
          (float) Math.pow((surfacePoints[i][0] - topCenter[0]), 2)
              + (float) Math.pow((surfacePoints[i][1] - topCenter[1]), 2)
              + (float) Math.pow((surfacePoints[i][2] - topCenter[2]), 2)));
      ellipticRingCylinderGridPoints[0][i][0] = surfacePoints[i][0];
      ellipticRingCylinderGridPoints[0][i][1] = surfacePoints[i][1];
      ellipticRingCylinderGridPoints[0][i][2] = surfacePoints[i][2];
      ellipticRingCylinderGridPoints[0][i][3] = 1.f;
      ellipticRingCylinderGridPoints[1][i][0] = surfacePoints[i][0] + cosAlpha * ringWidth;
      ellipticRingCylinderGridPoints[1][i][1] = surfacePoints[i][1] + cosBeta * ringWidth;
      ellipticRingCylinderGridPoints[1][i][2] = surfacePoints[i][2] + cosGamma * ringWidth;
      ellipticRingCylinderGridPoints[1][i][3] = 1.f;
      ellipticRingCylinderGridPoints[2][i][0] = surfacePoints[i][0] + cosAlpha * ringWidth;
      ellipticRingCylinderGridPoints[2][i][1] = surfacePoints[i][1] + cosBeta * ringWidth + cylinderHeight;
      ellipticRingCylinderGridPoints[2][i][2] = surfacePoints[i][2] + cosGamma * ringWidth;
      ellipticRingCylinderGridPoints[2][i][3] = 1.f;
      ellipticRingCylinderGridPoints[3][i][0] = surfacePoints[i][0];
      ellipticRingCylinderGridPoints[3][i][1] = surfacePoints[i][1] + cylinderHeight;
      ellipticRingCylinderGridPoints[3][i][2] = surfacePoints[i][2];
      ellipticRingCylinderGridPoints[3][i][3] = 1.f;
    }

    // 内层的椭圆柱
    QuadArray quads1 = new QuadArray((numOriPoints - 1) * 4, QuadArray.COORDINATES | IndexedQuadArray.NORMALS);
    int c1 = 0;
    for (int i = 0; i <= (numOriPoints - 1) - 1; i++) {
      quads1.setCoordinate(c1, new Point3f(ellipticRingCylinderGridPoints[1][i]));
      quads1.setCoordinate(c1 + 1, new Point3f(ellipticRingCylinderGridPoints[1][i + 1]));
      quads1.setCoordinate(c1 + 2, new Point3f(ellipticRingCylinderGridPoints[2][i + 1]));
      quads1.setCoordinate(c1 + 3, new Point3f(ellipticRingCylinderGridPoints[2][i]));
      Vector3f a = new Vector3f(
          ellipticRingCylinderGridPoints[1][i + 1][0] - ellipticRingCylinderGridPoints[1][i][0],
          ellipticRingCylinderGridPoints[1][i + 1][1] - ellipticRingCylinderGridPoints[1][i][1],
          ellipticRingCylinderGridPoints[1][i + 1][2] - ellipticRingCylinderGridPoints[1][i][2]);
      Vector3f b = new Vector3f(
          ellipticRingCylinderGridPoints[2][i + 1][0] - ellipticRingCylinderGridPoints[1][i + 1][0],
          ellipticRingCylinderGridPoints[2][i + 1][1] - ellipticRingCylinderGridPoints[1][i + 1][1],
          ellipticRingCylinderGridPoints[2][i + 1][2] - ellipticRingCylinderGridPoints[1][i + 1][2]);
      Vector3f n = new Vector3f();
      n.cross(a, b);
      n.normalize();
      quads1.setNormal(c1, n);
      quads1.setNormal(c1 + 1, n);
      quads1.setNormal(c1 + 2, n);
      quads1.setNormal(c1 + 3, n);
      c1 += 4;
    }

    // 外层的椭圆柱
    QuadArray quads2 = new QuadArray((numOriPoints - 1) * 4, QuadArray.COORDINATES | IndexedQuadArray.NORMALS);
    int c2 = 0;
    for (int i = 0; i <= (numOriPoints - 1) - 1; i++) {
      quads2.setCoordinate(c2, new Point3f(ellipticRingCylinderGridPoints[0][i]));
      quads2.setCoordinate(c2 + 1, new Point3f(ellipticRingCylinderGridPoints[0][i + 1]));
      quads2.setCoordinate(c2 + 2, new Point3f(ellipticRingCylinderGridPoints[3][i + 1]));
      quads2.setCoordinate(c2 + 3, new Point3f(ellipticRingCylinderGridPoints[3][i]));
      Vector3f a = new Vector3f(
          ellipticRingCylinderGridPoints[0][i + 1][0] - ellipticRingCylinderGridPoints[0][i][0],
          ellipticRingCylinderGridPoints[0][i + 1][1] - ellipticRingCylinderGridPoints[0][i][1],
          ellipticRingCylinderGridPoints[0][i + 1][2] - ellipticRingCylinderGridPoints[0][i][2]);
      Vector3f b = new Vector3f(
          ellipticRingCylinderGridPoints[3][i + 1][0] - ellipticRingCylinderGridPoints[0][i + 1][0],
          ellipticRingCylinderGridPoints[3][i + 1][1] - ellipticRingCylinderGridPoints[0][i + 1][1],
          ellipticRingCylinderGridPoints[3][i + 1][2] - ellipticRingCylinderGridPoints[0][i + 1][2]);
      Vector3f n = new Vector3f();
      n.cross(b, a);
      n.normalize();
      quads2.setNormal(c2, n);
      quads2.setNormal(c2 + 1, n);
      quads2.setNormal(c2 + 2, n);
      quads2.setNormal(c2 + 3, n);
      c2 += 4;
    }

    // 上层的椭圆环面
    QuadArray quads3 = new QuadArray((numOriPoints - 1) * 4, QuadArray.COORDINATES | IndexedQuadArray.NORMALS);
    int c3 = 0;
    for (int i = 0; i <= (numOriPoints - 1) - 1; i++) {
      quads3.setCoordinate(c3, new Point3f(ellipticRingCylinderGridPoints[2][i]));
      quads3.setCoordinate(c3 + 1, new Point3f(ellipticRingCylinderGridPoints[2][i + 1]));
      quads3.setCoordinate(c3 + 2, new Point3f(ellipticRingCylinderGridPoints[3][i + 1]));
      quads3.setCoordinate(c3 + 3, new Point3f(ellipticRingCylinderGridPoints[3][i]));
      Vector3f a = new Vector3f(
          ellipticRingCylinderGridPoints[2][i + 1][0] - ellipticRingCylinderGridPoints[2][i][0],
          ellipticRingCylinderGridPoints[2][i + 1][1] - ellipticRingCylinderGridPoints[2][i][1],
          ellipticRingCylinderGridPoints[2][i + 1][2] - ellipticRingCylinderGridPoints[2][i][2]);
      Vector3f b = new Vector3f(
          ellipticRingCylinderGridPoints[3][i + 1][0] - ellipticRingCylinderGridPoints[2][i + 1][0],
          ellipticRingCylinderGridPoints[3][i + 1][1] - ellipticRingCylinderGridPoints[2][i + 1][1],
          ellipticRingCylinderGridPoints[3][i + 1][2] - ellipticRingCylinderGridPoints[2][i + 1][2]);
      Vector3f n = new Vector3f();
      n.cross(a, b);
      n.normalize();
      quads3.setNormal(c3, n);
      quads3.setNormal(c3 + 1, n);
      quads3.setNormal(c3 + 2, n);
      quads3.setNormal(c3 + 3, n);
      c3 += 4;
    }

    PolygonAttributes polygonattributes = new PolygonAttributes();
    polygonattributes.setCullFace(PolygonAttributes.CULL_NONE);
    polygonattributes.setBackFaceNormalFlip(true);
    polygonattributes.setPolygonMode(PolygonAttributes.POLYGON_FILL);
    Appearance app = new Appearance();
    app.setPolygonAttributes(polygonattributes);
    Material material = new Material();
    Color3f color = new Color3f(0.2f, 0.8f, 1.f);
    material.setDiffuseColor(color);
    material.setEmissiveColor(color);
    app.setMaterial(material);
    // TransparencyAttributes transparency = new
    // TransparencyAttributes(TransparencyAttributes.NICEST, 0.f);
    // app.setTransparencyAttributes(transparency);

    this.addGeometry(quads1);
    this.addGeometry(quads2);
    this.addGeometry(quads3);
    this.setAppearance(app);
  }
}

// 绘制圆柱体
class TopCylinder extends Shape3D {
  public TopCylinder(float[][] loc, float radiusRate, float height, Appearance app, int division) {
    int divisions = division;
    float[][][] points = new float[2][divisions + 1][4];
    for (int i = 0; i <= divisions; i++) {
      points[0][i] = new float[] { loc[i][0] * radiusRate, loc[i][1], loc[i][2] * radiusRate };
      points[1][i] = new float[] { loc[i][0] * radiusRate, height + loc[i][1], loc[i][2] * radiusRate };
    }
    int numSides = divisions;
    QuadArray quads = new QuadArray(numSides * 4, QuadArray.COORDINATES | IndexedQuadArray.NORMALS);
    int c = 0;
    for (int i = 0; i <= numSides - 1; i++) {
      quads.setCoordinate(c, new Point3f(points[0][i]));
      quads.setCoordinate(c + 1, new Point3f(points[0][i + 1]));
      quads.setCoordinate(c + 2, new Point3f(points[1][i + 1]));
      quads.setCoordinate(c + 3, new Point3f(points[1][i]));
      Vector3f a = new Vector3f(
          points[0][i + 1][0] - points[0][i][0],
          points[0][i + 1][1] - points[0][i][1],
          points[0][i + 1][2] - points[0][i][2]);
      Vector3f b = new Vector3f(
          points[1][i + 1][0] - points[0][i + 1][0],
          points[1][i + 1][1] - points[0][i + 1][1],
          points[1][i + 1][2] - points[0][i + 1][2]);
      Vector3f n = new Vector3f();
      n.cross(a, b);
      n.normalize();
      quads.setNormal(c, n);
      quads.setNormal(c + 1, n);
      quads.setNormal(c + 2, n);
      quads.setNormal(c + 3, n);
      c += 4;
    }
    this.setGeometry(quads);
    this.setAppearance(app);
  }
}

// 绘制塔顶的椭圆到正圆过渡圆柱
class EllipseAndCircleCylinder extends Shape3D {
  public EllipseAndCircleCylinder(float[][][] ellipseAndCirclePointsLoc, int cylinderDiv, Appearance app) {
    int numSides = cylinderDiv;
    QuadArray quads = new QuadArray(numSides * 4, QuadArray.COORDINATES | IndexedQuadArray.NORMALS);
    int c = 0;
    for (int i = 0; i <= numSides - 1; i++) {
      quads.setCoordinate(c, new Point3f(ellipseAndCirclePointsLoc[0][i]));
      quads.setCoordinate(c + 1, new Point3f(ellipseAndCirclePointsLoc[0][i + 1]));
      quads.setCoordinate(c + 2, new Point3f(ellipseAndCirclePointsLoc[1][i + 1]));
      quads.setCoordinate(c + 3, new Point3f(ellipseAndCirclePointsLoc[1][i]));
      Vector3f a = new Vector3f(
          ellipseAndCirclePointsLoc[0][i + 1][0] - ellipseAndCirclePointsLoc[0][i][0],
          ellipseAndCirclePointsLoc[0][i + 1][1] - ellipseAndCirclePointsLoc[0][i][1],
          ellipseAndCirclePointsLoc[0][i + 1][2] - ellipseAndCirclePointsLoc[0][i][2]);
      Vector3f b = new Vector3f(
          ellipseAndCirclePointsLoc[1][i + 1][0] - ellipseAndCirclePointsLoc[0][i + 1][0],
          ellipseAndCirclePointsLoc[1][i + 1][1] - ellipseAndCirclePointsLoc[0][i + 1][1],
          ellipseAndCirclePointsLoc[1][i + 1][2] - ellipseAndCirclePointsLoc[0][i + 1][2]);
      Vector3f n = new Vector3f();
      n.cross(a, b);
      n.normalize();
      quads.setNormal(c, n);
      quads.setNormal(c + 1, n);
      quads.setNormal(c + 2, n);
      quads.setNormal(c + 3, n);
      c += 4;
    }
    this.setGeometry(quads);
    this.setAppearance(app);
  }
}