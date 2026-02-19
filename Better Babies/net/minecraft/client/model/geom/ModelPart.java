// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
package net.minecraft.client.model.geom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class ModelPart {
    public static final float DEFAULT_SCALE = 1.0F;
    public float x;
    public float y;
    public float z;
    public float xRot;
    public float yRot;
    public float zRot;
    public float xScale = 1.0F;
    public float yScale = 1.0F;
    public float zScale = 1.0F;
    public boolean visible = true;
    public boolean skipDraw;
    private final List<Cube> cubes;
    private final Map<String, ModelPart> children;
    private PartPose initialPose;

    public ModelPart(final List<Cube> cubes, final Map<String, ModelPart> children) {
        this.initialPose = PartPose.ZERO;
        this.cubes = cubes;
        this.children = children;
    }

    public PartPose storePose() {
        return PartPose.offsetAndRotation(this.x, this.y, this.z, this.xRot, this.yRot, this.zRot);
    }

    public PartPose getInitialPose() {
        return this.initialPose;
    }

    public void setInitialPose(final PartPose initialPose) {
        this.initialPose = initialPose;
    }

    public void resetPose() {
        this.loadPose(this.initialPose);
    }

    public void loadPose(final PartPose pose) {
        this.x = pose.x();
        this.y = pose.y();
        this.z = pose.z();
        this.xRot = pose.xRot();
        this.yRot = pose.yRot();
        this.zRot = pose.zRot();
        this.xScale = pose.xScale();
        this.yScale = pose.yScale();
        this.zScale = pose.zScale();
    }

    public boolean hasChild(final String name) {
        return this.children.containsKey(name);
    }

    public ModelPart getChild(final String name) {
        ModelPart result = (ModelPart) this.children.get(name);
        if (result == null) {
            throw new NoSuchElementException("Can't find part " + name);
        } else {
            return result;
        }
    }

    public void setPos(final float x, final float y, final float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setRotation(final float xRot, final float yRot, final float zRot) {
        this.xRot = xRot;
        this.yRot = yRot;
        this.zRot = zRot;
    }

    public void render(final PoseStack poseStack, final VertexConsumer buffer, final int lightCoords,
            final int overlayCoords) {
        this.render(poseStack, buffer, lightCoords, overlayCoords, -1);
    }

    public void render(final PoseStack poseStack, final VertexConsumer buffer, final int lightCoords,
            final int overlayCoords, final int color) {
        if (this.visible) {
            if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
                poseStack.pushPose();
                this.translateAndRotate(poseStack);
                if (!this.skipDraw) {
                    this.compile(poseStack.last(), buffer, lightCoords, overlayCoords, color);
                }

                Iterator var6 = this.children.values().iterator();

                while (var6.hasNext()) {
                    ModelPart child = (ModelPart) var6.next();
                    child.render(poseStack, buffer, lightCoords, overlayCoords, color);
                }

                poseStack.popPose();
            }
        }
    }

    public void rotateBy(final Quaternionf rotation) {
        Matrix3f oldRotation = (new Matrix3f()).rotationZYX(this.zRot, this.yRot, this.xRot);
        Matrix3f newRotation = oldRotation.rotate(rotation);
        Vector3f newAngles = newRotation.getEulerAnglesZYX(new Vector3f());
        this.setRotation(newAngles.x, newAngles.y, newAngles.z);
    }

    public void getExtentsForGui(final PoseStack poseStack, final Consumer<Vector3fc> output) {
        this.visit(poseStack, (pose, partPath, cubeIndex, cube) -> {
            Polygon[] arr$ = cube.polygons;
            int len$ = arr$.length;

            for (int i$x = 0; i$x < len$; ++i$x) {
                Polygon polygon = arr$[i$x];
                Vertex[] arr$x = polygon.vertices();
                int len$x = arr$x.length;

                for (int i$ = 0; i$ < len$x; ++i$) {
                    Vertex vertex = arr$x[i$];
                    float x = vertex.worldX();
                    float y = vertex.worldY();
                    float z = vertex.worldZ();
                    Vector3f pos = pose.pose().transformPosition(x, y, z, new Vector3f());
                    output.accept(pos);
                }
            }

        });
    }

    public void visit(final PoseStack poseStack, final Visitor visitor) {
        this.visit(poseStack, visitor, "");
    }

    private void visit(final PoseStack poseStack, final Visitor visitor, final String path) {
        if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
            poseStack.pushPose();
            this.translateAndRotate(poseStack);
            PoseStack.Pose pose = poseStack.last();

            for (int i = 0; i < this.cubes.size(); ++i) {
                visitor.visit(pose, path, i, (Cube) this.cubes.get(i));
            }

            String childPath = path + "/";
            this.children.forEach((name, child) -> {
                child.visit(poseStack, visitor, childPath + name);
            });
            poseStack.popPose();
        }
    }

    public void translateAndRotate(final PoseStack poseStack) {
        poseStack.translate(this.x / 16.0F, this.y / 16.0F, this.z / 16.0F);
        if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
            poseStack.mulPose((new Quaternionf()).rotationZYX(this.zRot, this.yRot, this.xRot));
        }

        if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
            poseStack.scale(this.xScale, this.yScale, this.zScale);
        }

    }

    private void compile(final PoseStack.Pose pose, final VertexConsumer builder, final int lightCoords,
            final int overlayCoords, final int color) {
        Iterator var6 = this.cubes.iterator();

        while (var6.hasNext()) {
            Cube cube = (Cube) var6.next();
            cube.compile(pose, builder, lightCoords, overlayCoords, color);
        }

    }

    public Cube getRandomCube(final RandomSource random) {
        return (Cube) this.cubes.get(random.nextInt(this.cubes.size()));
    }

    public boolean isEmpty() {
        return this.cubes.isEmpty();
    }

    public void offsetPos(final Vector3f offset) {
        this.x += offset.x();
        this.y += offset.y();
        this.z += offset.z();
    }

    public void offsetRotation(final Vector3f offset) {
        this.xRot += offset.x();
        this.yRot += offset.y();
        this.zRot += offset.z();
    }

    public void offsetScale(final Vector3f offset) {
        this.xScale += offset.x();
        this.yScale += offset.y();
        this.zScale += offset.z();
    }

    public List<ModelPart> getAllParts() {
        List<ModelPart> allParts = new ArrayList();
        allParts.add(this);
        this.addAllChildren((name, part) -> {
            allParts.add(part);
        });
        return List.copyOf(allParts);
    }

    public Function<String, @Nullable ModelPart> createPartLookup() {
        Map<String, ModelPart> parts = new HashMap();
        parts.put("root", this);
        Objects.requireNonNull(parts);
        this.addAllChildren(parts::putIfAbsent);
        Objects.requireNonNull(parts);
        return parts::get;
    }

    private void addAllChildren(final BiConsumer<String, ModelPart> output) {
        Iterator var2 = this.children.entrySet().iterator();

        while (var2.hasNext()) {
            Map.Entry<String, ModelPart> entry = (Map.Entry) var2.next();
            output.accept((String) entry.getKey(), (ModelPart) entry.getValue());
        }

        var2 = this.children.values().iterator();

        while (var2.hasNext()) {
            ModelPart part = (ModelPart) var2.next();
            part.addAllChildren(output);
        }

    }

    @FunctionalInterface
    public interface Visitor {
        void visit(final PoseStack.Pose pose, final String partPath, final int cubeIndex, final Cube cube);
    }

    public static class Cube {
        public final Polygon[] polygons;
        public final float minX;
        public final float minY;
        public final float minZ;
        public final float maxX;
        public final float maxY;
        public final float maxZ;

        public Cube(final int xTexOffs, final int yTexOffs, float minX, float minY, float minZ, final float width,
                final float height, final float depth, final float growX, final float growY, final float growZ,
                final boolean mirror, final float xTexSize, final float yTexSize, final Set<Direction> visibleFaces) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = minX + width;
            this.maxY = minY + height;
            this.maxZ = minZ + depth;
            this.polygons = new Polygon[visibleFaces.size()];
            float maxX = minX + width;
            float maxY = minY + height;
            float maxZ = minZ + depth;
            minX -= growX;
            minY -= growY;
            minZ -= growZ;
            maxX += growX;
            maxY += growY;
            maxZ += growZ;
            if (mirror) {
                float tmp = maxX;
                maxX = minX;
                minX = tmp;
            }

            Vertex t0 = new Vertex(minX, minY, minZ, 0.0F, 0.0F);
            Vertex t1 = new Vertex(maxX, minY, minZ, 0.0F, 8.0F);
            Vertex t2 = new Vertex(maxX, maxY, minZ, 8.0F, 8.0F);
            Vertex t3 = new Vertex(minX, maxY, minZ, 8.0F, 0.0F);
            Vertex l0 = new Vertex(minX, minY, maxZ, 0.0F, 0.0F);
            Vertex l1 = new Vertex(maxX, minY, maxZ, 0.0F, 8.0F);
            Vertex l2 = new Vertex(maxX, maxY, maxZ, 8.0F, 8.0F);
            Vertex l3 = new Vertex(minX, maxY, maxZ, 8.0F, 0.0F);
            float u0 = (float) xTexOffs;
            float u1 = (float) xTexOffs + depth;
            float u2 = (float) xTexOffs + depth + width;
            float u22 = (float) xTexOffs + depth + width + width;
            float u3 = (float) xTexOffs + depth + width + depth;
            float u4 = (float) xTexOffs + depth + width + depth + width;
            float v0 = (float) yTexOffs;
            float v1 = (float) yTexOffs + depth;
            float v2 = (float) yTexOffs + depth + height;
            int pos = 0;
            if (visibleFaces.contains(Direction.DOWN)) {
                this.polygons[pos++] = new Polygon(new Vertex[] { l1, l0, t0, t1 }, u1, v0, u2, v1, xTexSize, yTexSize,
                        mirror, Direction.DOWN);
            }

            if (visibleFaces.contains(Direction.UP)) {
                this.polygons[pos++] = new Polygon(new Vertex[] { t2, t3, l3, l2 }, u2, v1, u22, v0, xTexSize, yTexSize,
                        mirror, Direction.UP);
            }

            if (visibleFaces.contains(Direction.WEST)) {
                this.polygons[pos++] = new Polygon(new Vertex[] { t0, l0, l3, t3 }, u0, v1, u1, v2, xTexSize, yTexSize,
                        mirror, Direction.WEST);
            }

            if (visibleFaces.contains(Direction.NORTH)) {
                this.polygons[pos++] = new Polygon(new Vertex[] { t1, t0, t3, t2 }, u1, v1, u2, v2, xTexSize, yTexSize,
                        mirror, Direction.NORTH);
            }

            if (visibleFaces.contains(Direction.EAST)) {
                this.polygons[pos++] = new Polygon(new Vertex[] { l1, t1, t2, l2 }, u2, v1, u3, v2, xTexSize, yTexSize,
                        mirror, Direction.EAST);
            }

            if (visibleFaces.contains(Direction.SOUTH)) {
                this.polygons[pos] = new Polygon(new Vertex[] { l0, l1, l2, l3 }, u3, v1, u4, v2, xTexSize, yTexSize,
                        mirror, Direction.SOUTH);
            }

        }

        public void compile(final PoseStack.Pose pose, final VertexConsumer builder, final int lightCoords,
                final int overlayCoords, final int color) {
            Matrix4f matrix = pose.pose();
            Vector3f scratchVector = new Vector3f();
            Polygon[] var8 = this.polygons;
            int var9 = var8.length;

            for (int var10 = 0; var10 < var9; ++var10) {
                Polygon polygon = var8[var10];
                Vector3f normal = pose.transformNormal(polygon.normal, scratchVector);
                float nx = normal.x();
                float ny = normal.y();
                float nz = normal.z();
                Vertex[] var16 = polygon.vertices;
                int var17 = var16.length;

                for (int var18 = 0; var18 < var17; ++var18) {
                    Vertex vertex = var16[var18];
                    float x = vertex.worldX();
                    float y = vertex.worldY();
                    float z = vertex.worldZ();
                    Vector3f pos = matrix.transformPosition(x, y, z, scratchVector);
                    builder.addVertex(pos.x(), pos.y(), pos.z(), color, vertex.u, vertex.v, overlayCoords, lightCoords,
                            nx, ny, nz);
                }
            }

        }
    }

    public static record Polygon(Vertex[] vertices, Vector3fc normal) {
        public Polygon(final Vertex[] vertices, final float u0, final float v0, final float u1, final float v1,
                final float xTexSize, final float yTexSize, final boolean mirror, final Direction facing) {
            this(vertices, (mirror ? mirrorFacing(facing) : facing).getUnitVec3f());
            float us = 0.0F / xTexSize;
            float vs = 0.0F / yTexSize;
            vertices[0] = vertices[0].remap(u1 / xTexSize - us, v0 / yTexSize + vs);
            vertices[1] = vertices[1].remap(u0 / xTexSize + us, v0 / yTexSize + vs);
            vertices[2] = vertices[2].remap(u0 / xTexSize + us, v1 / yTexSize - vs);
            vertices[3] = vertices[3].remap(u1 / xTexSize - us, v1 / yTexSize - vs);
            if (mirror) {
                int length = vertices.length;

                for (int i = 0; i < length / 2; ++i) {
                    Vertex tmp = vertices[i];
                    vertices[i] = vertices[length - 1 - i];
                    vertices[length - 1 - i] = tmp;
                }
            }

        }

        public Polygon(Vertex[] vertices, Vector3fc normal) {
            this.vertices = vertices;
            this.normal = normal;
        }

        private static Direction mirrorFacing(final Direction facing) {
            return facing.getAxis() == Axis.X ? facing.getOpposite() : facing;
        }

        public Vertex[] vertices() {
            return this.vertices;
        }

        public Vector3fc normal() {
            return this.normal;
        }
    }

    public static record Vertex(float x, float y, float z, float u, float v) {
        public static final float SCALE_FACTOR = 16.0F;

        public Vertex(float x, float y, float z, float u, float v) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.u = u;
            this.v = v;
        }

        public Vertex remap(final float u, final float v) {
            return new Vertex(this.x, this.y, this.z, u, v);
        }

        public float worldX() {
            return this.x / 16.0F;
        }

        public float worldY() {
            return this.y / 16.0F;
        }

        public float worldZ() {
            return this.z / 16.0F;
        }

        public float x() {
            return this.x;
        }

        public float y() {
            return this.y;
        }

        public float z() {
            return this.z;
        }

        public float u() {
            return this.u;
        }

        public float v() {
            return this.v;
        }
    }
}
